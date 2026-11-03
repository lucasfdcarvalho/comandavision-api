package br.com.comandavision.api.pix;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.comandavision.api.comanda.Comanda;
import br.com.comandavision.api.comanda.ComandaNaoEncontradaException;
import br.com.comandavision.api.comanda.ComandaRepository;
import br.com.comandavision.api.comanda.ItemComanda;
import br.com.comandavision.api.comanda.ItemComandaRepository;
import br.com.comandavision.api.comanda.StatusComanda;
import br.com.comandavision.api.pagamento.ChaveIdempotencia;
import br.com.comandavision.api.pagamento.ChaveIdempotenciaEmConflitoException;
import br.com.comandavision.api.pagamento.ComandaNaoPodeReceberPagamentoException;
import br.com.comandavision.api.pagamento.FormaPagamento;
import br.com.comandavision.api.pagamento.OperacaoPagamentoInvalidaException;
import br.com.comandavision.api.pagamento.Pagamento;
import br.com.comandavision.api.pagamento.PagamentoNaoEncontradoException;
import br.com.comandavision.api.pagamento.PagamentoRepository;
import br.com.comandavision.api.pagamento.StatusPagamento;
import br.com.comandavision.api.pagamento.ValorPagamentoExcedeSaldoException;
import br.com.comandavision.api.pix.dto.CobrancaPixResponse;
import br.com.comandavision.api.pix.mercadopago.MercadoPagoClient;
import br.com.comandavision.api.pix.mercadopago.PedidoMercadoPago;

// Pix pelo Mercado Pago: a cobrança nasce como pagamento PENDENTE e vira CONFIRMADO quando o
// pedido é pago. O status sempre vem de uma consulta ao Mercado Pago (nunca do corpo do
// webhook), então um aviso falso não consegue confirmar pagamento.
// As chamadas ao Mercado Pago ficam fora das transações do banco.
@Service
public class PixService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PixService.class);

    // Tempo para o cliente pagar na mesa. O mínimo aceito pelo Mercado Pago é 30 minutos.
    static final Duration VALIDADE = Duration.ofMinutes(30);
    private static final String VALIDADE_ISO = "PT30M";

    private final MercadoPagoClient mercadoPagoClient;
    private final CobrancaPixRepository cobrancaPixRepository;
    private final PagamentoRepository pagamentoRepository;
    private final ComandaRepository comandaRepository;
    private final ItemComandaRepository itemComandaRepository;
    private final TransactionTemplate transacao;

    public PixService(
            MercadoPagoClient mercadoPagoClient,
            CobrancaPixRepository cobrancaPixRepository,
            PagamentoRepository pagamentoRepository,
            ComandaRepository comandaRepository,
            ItemComandaRepository itemComandaRepository,
            PlatformTransactionManager gerenciadorTransacao) {
        this.mercadoPagoClient = mercadoPagoClient;
        this.cobrancaPixRepository = cobrancaPixRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.comandaRepository = comandaRepository;
        this.itemComandaRepository = itemComandaRepository;
        this.transacao = new TransactionTemplate(gerenciadorTransacao);
    }

    public CobrancaPixResponse gerar(Long comandaId, BigDecimal valor, String chaveIdempotencia) {
        if (!mercadoPagoClient.estaConfigurado()) {
            throw new PixIndisponivelException();
        }

        String chave = ChaveIdempotencia.validar(chaveIdempotencia);

        // Pix pendentes já vencidos podem ter sido pagos no último minuto: confere antes de calcular o saldo.
        OffsetDateTime agora = OffsetDateTime.now();
        cobrancaPixRepository.findByPagamentoComandaIdAndPagamentoStatus(comandaId, StatusPagamento.PENDENTE).stream()
                .filter(cobranca -> cobranca.expirou(agora))
                .forEach(this::sincronizar);

        ReservaPagamento reserva = transacao.execute(status -> reservarPagamento(comandaId, valor, chave));
        if (reserva == null) {
            throw new IllegalStateException("Não foi possível reservar o pagamento Pix");
        }

        CobrancaPix cobrancaExistente = cobrancaPixRepository.findByPagamentoId(reserva.pagamento().getId())
                .orElse(null);
        if (cobrancaExistente != null) {
            return CobrancaPixResponse.from(cobrancaExistente);
        }

        String referencia = referenciaExterna(comandaId, chave);
        PedidoMercadoPago pedido = criarOuRecuperarPedido(reserva, valor, referencia, chave);

        if (pedido == null || pedido.id() == null || pedido.qrCode() == null || pedido.qrCodeBase64() == null) {
            throw new FalhaMercadoPagoException("O Mercado Pago não devolveu o QR Code do Pix. Tente novamente", null);
        }

        try {
            return transacao.execute(status -> {
                Pagamento pagamento = pagamentoRepository.findWithLockById(reserva.pagamento().getId())
                        .orElseThrow();
                CobrancaPix jaSalva = cobrancaPixRepository.findByPagamentoId(pagamento.getId()).orElse(null);
                if (jaSalva != null) {
                    return CobrancaPixResponse.from(jaSalva);
                }
                OffsetDateTime baseExpiracao = pagamento.getCriadoEm() == null ? agora : pagamento.getCriadoEm();
                CobrancaPix cobranca = cobrancaPixRepository.saveAndFlush(new CobrancaPix(
                        pagamento, pedido.id(), pedido.qrCode(), pedido.qrCodeBase64(), baseExpiracao.plus(VALIDADE)));
                return CobrancaPixResponse.from(cobranca);
            });
        } catch (RuntimeException exception) {
            // A order não é cancelada aqui: um resultado incerto precisa ser reconciliado.
            LOGGER.warn("Pix {} será reconciliado depois porque o vínculo local falhou", pedido.id());
            throw new FalhaMercadoPagoException(
                    "O Pix pode ter sido criado, mas ainda não foi conciliado. Tente novamente pela mesma tela; não gere outra cobrança",
                    exception,
                    true);
        }
    }

    public CobrancaPixResponse consultar(Long comandaId, Long pagamentoId) {
        CobrancaPix cobranca = buscarOuReconciliarCobranca(comandaId, pagamentoId);

        if (cobranca.getPagamento().getStatus() == StatusPagamento.PENDENTE) {
            sincronizar(cobranca);
        }

        return transacao.execute(status -> CobrancaPixResponse.from(buscarCobranca(comandaId, pagamentoId)));
    }

    public CobrancaPixResponse cancelar(Long comandaId, Long pagamentoId) {
        CobrancaPix cobranca = buscarOuReconciliarCobranca(comandaId, pagamentoId);

        if (cobranca.getPagamento().getStatus() != StatusPagamento.PENDENTE) {
            throw new OperacaoPagamentoInvalidaException("Somente um Pix aguardando pagamento pode ser cancelado");
        }

        boolean cancelado = mercadoPagoClient.cancelarPedido(cobranca.getPedidoMercadoPago());

        if (!cancelado) {
            // O Mercado Pago recusou o cancelamento: normalmente o cliente acabou de pagar.
            sincronizar(cobranca);
            CobrancaPixResponse atual = transacao.execute(status -> CobrancaPixResponse.from(buscarCobranca(comandaId, pagamentoId)));
            if (atual.status() == StatusPagamento.CONFIRMADO) {
                throw new OperacaoPagamentoInvalidaException("Este Pix já foi pago e não pode mais ser cancelado");
            }
            return atual;
        }

        return transacao.execute(status -> {
            CobrancaPix atual = buscarCobranca(comandaId, pagamentoId);
            if (atual.getPagamento().getStatus() == StatusPagamento.PENDENTE) {
                atual.getPagamento().cancelar();
            }
            return CobrancaPixResponse.from(atual);
        });
    }

    // Chamado pelo webhook. Pedido desconhecido é ignorado (pode ser de outra integração da conta).
    public void processarNotificacao(String pedidoId) {
        cobrancaPixRepository.findByPedidoMercadoPago(pedidoId).ifPresentOrElse(
                this::sincronizar,
                () -> LOGGER.info("Notificação do Mercado Pago para pedido desconhecido: {}", pedidoId));
    }

    // Consulta o pedido no Mercado Pago e aplica o resultado no pagamento local.
    void sincronizar(CobrancaPix cobranca) {
        PedidoMercadoPago pedido = mercadoPagoClient.buscarPedido(cobranca.getPedidoMercadoPago());
        if (pedido == null) {
            return;
        }

        transacao.executeWithoutResult(status -> {
            Pagamento pagamento = pagamentoRepository.findWithLockById(cobranca.getPagamento().getId())
                    .orElseThrow();
            StatusPagamento atual = pagamento.getStatus();

            if (pedido.foiPago() && atual == StatusPagamento.PENDENTE) {
                pagamento.confirmar(pedido.id());
            } else if (pedido.foiDevolvido() && atual == StatusPagamento.CONFIRMADO) {
                pagamento.confirmarDevolucaoPeloProvedor();
            } else if (pedido.foiEncerradoSemPagamento() && atual == StatusPagamento.PENDENTE) {
                pagamento.cancelar();
            } else if (pedido.foiPago() && atual == StatusPagamento.CANCELADO) {
                LOGGER.warn("Pix {} foi pago depois de cancelado localmente (pagamento {})", pedido.id(), pagamento.getId());
            }
        });
    }

    private ReservaPagamento reservarPagamento(Long comandaId, BigDecimal valor, String chave) {
        Pagamento repetido = pagamentoRepository.findByChaveIdempotencia(chave).orElse(null);
        if (repetido != null) {
            validarRepeticaoPix(repetido, comandaId, valor);
            return new ReservaPagamento(repetido, false);
        }

        Comanda comanda = comandaRepository.findByIdForUpdate(comandaId)
                .orElseThrow(() -> new ComandaNaoEncontradaException(comandaId));

        repetido = pagamentoRepository.findByChaveIdempotencia(chave).orElse(null);
        if (repetido != null) {
            validarRepeticaoPix(repetido, comandaId, valor);
            return new ReservaPagamento(repetido, false);
        }

        validarValor(comandaId, valor);
        Pagamento pagamento = pagamentoRepository.saveAndFlush(
                new Pagamento(comanda, FormaPagamento.PIX, valor, chave));
        return new ReservaPagamento(pagamento, true);
    }

    private PedidoMercadoPago criarOuRecuperarPedido(
            ReservaPagamento reserva,
            BigDecimal valor,
            String referencia,
            String chave) {
        PedidoMercadoPago pedido = null;

        if (!reserva.nova()) {
            pedido = buscarPedidoParaReconciliacao(reserva.pagamento(), referencia);
        }

        if (pedido != null) {
            return pedido;
        }

        try {
            return mercadoPagoClient.criarPedidoPix(valor, referencia, VALIDADE_ISO, chave);
        } catch (FalhaMercadoPagoException falhaCriacao) {
            if (!falhaCriacao.resultadoIncerto()) {
                descartarReservaSemPedido(reserva.pagamento().getId());
                throw falhaCriacao;
            }
            if (falhaCriacao.resultadoIncerto()) {
                try {
                    pedido = buscarPedidoParaReconciliacao(reserva.pagamento(), referencia);
                } catch (FalhaMercadoPagoException falhaConsulta) {
                    falhaCriacao.addSuppressed(falhaConsulta);
                }
            }
            if (pedido != null) {
                return pedido;
            }
            throw falhaCriacao;
        }
    }

    private PedidoMercadoPago buscarPedidoParaReconciliacao(Pagamento pagamento, String referencia) {
        OffsetDateTime agora = OffsetDateTime.now();
        OffsetDateTime criadoEm = pagamento.getCriadoEm() == null ? agora.minusHours(1) : pagamento.getCriadoEm();
        return mercadoPagoClient.buscarPedidoPorReferencia(
                referencia,
                criadoEm.minusMinutes(5),
                agora.plusMinutes(5));
    }

    private void descartarReservaSemPedido(Long pagamentoId) {
        transacao.executeWithoutResult(status -> {
            Pagamento pagamento = pagamentoRepository.findWithLockById(pagamentoId).orElse(null);
            if (pagamento != null
                    && pagamento.getStatus() == StatusPagamento.PENDENTE
                    && cobrancaPixRepository.findByPagamentoId(pagamentoId).isEmpty()) {
                pagamentoRepository.delete(pagamento);
            }
        });
    }

    private void validarValor(Long comandaId, BigDecimal valor) {
        Comanda comanda = comandaRepository.findById(comandaId)
                .orElseThrow(() -> new ComandaNaoEncontradaException(comandaId));

        if (comanda.getStatus() != StatusComanda.FECHADA) {
            throw new ComandaNaoPodeReceberPagamentoException(comandaId, comanda.getStatus());
        }

        BigDecimal total = itemComandaRepository.findByComandaIdOrderByCriadoEmAsc(comandaId).stream()
                .map(ItemComanda::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Pagamento> pagamentos = pagamentoRepository.findByComandaIdOrderByCriadoEmAsc(comandaId);
        BigDecimal confirmado = somar(pagamentos, StatusPagamento.CONFIRMADO);
        BigDecimal pendente = somar(pagamentos, StatusPagamento.PENDENTE);
        BigDecimal saldo = total.subtract(confirmado);

        if (valor.compareTo(saldo) > 0) {
            throw new ValorPagamentoExcedeSaldoException(valor, saldo);
        }
        if (valor.compareTo(saldo.subtract(pendente)) > 0) {
            throw new CobrancaPixPendenteException(pendente);
        }
    }

    private CobrancaPix buscarOuReconciliarCobranca(Long comandaId, Long pagamentoId) {
        CobrancaPix existente = cobrancaPixRepository.findByPagamentoId(pagamentoId).orElse(null);
        if (existente != null) {
            if (!Objects.equals(existente.getPagamento().getComanda().getId(), comandaId)) {
                throw new PagamentoNaoEncontradoException(pagamentoId, comandaId);
            }
            return existente;
        }

        Pagamento pagamento = pagamentoRepository.findByIdAndComandaId(pagamentoId, comandaId)
                .filter(item -> item.getForma() == FormaPagamento.PIX && item.getChaveIdempotencia() != null)
                .orElseThrow(() -> new PagamentoNaoEncontradoException(pagamentoId, comandaId));

        String referencia = referenciaExterna(comandaId, pagamento.getChaveIdempotencia());
        PedidoMercadoPago pedido = buscarPedidoParaReconciliacao(pagamento, referencia);
        if (pedido == null) {
            throw new FalhaMercadoPagoException(
                    "Ainda não foi possível confirmar a criação deste Pix. Tente novamente; não gere outra cobrança",
                    null,
                    true);
        }
        if (pedido.id() == null || pedido.qrCode() == null || pedido.qrCodeBase64() == null) {
            throw new FalhaMercadoPagoException(
                    "O Mercado Pago não devolveu todos os dados do Pix durante a conciliação",
                    null,
                    true);
        }

        transacao.executeWithoutResult(status -> {
            if (cobrancaPixRepository.findByPagamentoId(pagamentoId).isPresent()) {
                return;
            }
            Pagamento bloqueado = pagamentoRepository.findWithLockById(pagamentoId).orElseThrow();
            if (cobrancaPixRepository.findByPagamentoId(pagamentoId).isEmpty()) {
                OffsetDateTime base = bloqueado.getCriadoEm() == null
                        ? OffsetDateTime.now()
                        : bloqueado.getCriadoEm();
                cobrancaPixRepository.saveAndFlush(new CobrancaPix(
                        bloqueado,
                        pedido.id(),
                        pedido.qrCode(),
                        pedido.qrCodeBase64(),
                        base.plus(VALIDADE)));
            }
        });

        return buscarCobranca(comandaId, pagamentoId);
    }

    private static void validarRepeticaoPix(Pagamento pagamento, Long comandaId, BigDecimal valor) {
        boolean mesmaOperacao = Objects.equals(pagamento.getComanda().getId(), comandaId)
                && pagamento.getForma() == FormaPagamento.PIX
                && pagamento.getValor().compareTo(valor) == 0;
        if (!mesmaOperacao) {
            throw new ChaveIdempotenciaEmConflitoException();
        }
    }

    private static String referenciaExterna(Long comandaId, String chave) {
        return "cv-" + comandaId + "-" + chave;
    }

    private static BigDecimal somar(List<Pagamento> pagamentos, StatusPagamento status) {
        return pagamentos.stream()
                .filter(pagamento -> pagamento.getStatus() == status)
                .map(Pagamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CobrancaPix buscarCobranca(Long comandaId, Long pagamentoId) {
        return cobrancaPixRepository.findByPagamentoId(pagamentoId)
                .filter(cobranca -> cobranca.getPagamento().getComanda().getId().equals(comandaId))
                .orElseThrow(() -> new PagamentoNaoEncontradoException(pagamentoId, comandaId));
    }

    private record ReservaPagamento(Pagamento pagamento, boolean nova) {
    }
}

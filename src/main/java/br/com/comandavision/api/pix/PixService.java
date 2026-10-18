package br.com.comandavision.api.pix;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

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

    public CobrancaPixResponse gerar(Long comandaId, BigDecimal valor) {
        if (!mercadoPagoClient.estaConfigurado()) {
            throw new PixIndisponivelException();
        }

        // Pix pendentes já vencidos podem ter sido pagos no último minuto: confere antes de calcular o saldo.
        OffsetDateTime agora = OffsetDateTime.now();
        cobrancaPixRepository.findByPagamentoComandaIdAndPagamentoStatus(comandaId, StatusPagamento.PENDENTE).stream()
                .filter(cobranca -> cobranca.expirou(agora))
                .forEach(this::sincronizar);

        transacao.executeWithoutResult(status -> validarValor(comandaId, valor));

        String referencia = "comanda-" + comandaId + "-" + UUID.randomUUID();
        PedidoMercadoPago pedido = mercadoPagoClient.criarPedidoPix(valor, referencia, VALIDADE_ISO);

        if (pedido == null || pedido.id() == null || pedido.qrCode() == null || pedido.qrCodeBase64() == null) {
            throw new FalhaMercadoPagoException("O Mercado Pago não devolveu o QR Code do Pix. Tente novamente", null);
        }

        try {
            return transacao.execute(status -> {
                Comanda comanda = comandaRepository.findById(comandaId)
                        .orElseThrow(() -> new ComandaNaoEncontradaException(comandaId));
                Pagamento pagamento = pagamentoRepository.save(new Pagamento(comanda, FormaPagamento.PIX, valor));
                CobrancaPix cobranca = cobrancaPixRepository.save(new CobrancaPix(
                        pagamento, pedido.id(), pedido.qrCode(), pedido.qrCodeBase64(), agora.plus(VALIDADE)));
                return CobrancaPixResponse.from(cobranca);
            });
        } catch (RuntimeException exception) {
            // Sem registro local, o Pix não pode ficar valendo no Mercado Pago.
            try {
                mercadoPagoClient.cancelarPedido(pedido.id());
            } catch (RuntimeException falhaCancelamento) {
                LOGGER.error("Pix {} ficou sem registro local e não pôde ser cancelado", pedido.id(), falhaCancelamento);
            }
            throw exception;
        }
    }

    public CobrancaPixResponse consultar(Long comandaId, Long pagamentoId) {
        CobrancaPix cobranca = buscarCobranca(comandaId, pagamentoId);

        if (cobranca.getPagamento().getStatus() == StatusPagamento.PENDENTE) {
            sincronizar(cobranca);
        }

        return transacao.execute(status -> CobrancaPixResponse.from(buscarCobranca(comandaId, pagamentoId)));
    }

    public CobrancaPixResponse cancelar(Long comandaId, Long pagamentoId) {
        CobrancaPix cobranca = buscarCobranca(comandaId, pagamentoId);

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
            Pagamento pagamento = pagamentoRepository.findById(cobranca.getPagamento().getId())
                    .orElseThrow();
            StatusPagamento atual = pagamento.getStatus();

            if (pedido.foiPago() && atual == StatusPagamento.PENDENTE) {
                pagamento.confirmar(pedido.id());
            } else if (pedido.foiDevolvido() && atual == StatusPagamento.CONFIRMADO) {
                pagamento.estornar();
            } else if (pedido.foiEncerradoSemPagamento() && atual == StatusPagamento.PENDENTE) {
                pagamento.cancelar();
            } else if (pedido.foiPago() && atual == StatusPagamento.CANCELADO) {
                LOGGER.warn("Pix {} foi pago depois de cancelado localmente (pagamento {})", pedido.id(), pagamento.getId());
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
}

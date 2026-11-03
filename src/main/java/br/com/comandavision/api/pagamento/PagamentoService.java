package br.com.comandavision.api.pagamento;

import br.com.comandavision.api.pix.CobrancaPixPendenteException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.comandavision.api.comanda.Comanda;
import br.com.comandavision.api.comanda.ComandaNaoEncontradaException;
import br.com.comandavision.api.comanda.ComandaRepository;
import br.com.comandavision.api.comanda.ItemComanda;
import br.com.comandavision.api.comanda.ItemComandaRepository;
import br.com.comandavision.api.comanda.StatusComanda;
import br.com.comandavision.api.pagamento.dto.PagamentoResponse;
import br.com.comandavision.api.pagamento.dto.CorrigirPagamentoRequest;
import br.com.comandavision.api.pagamento.dto.RegistrarPagamentoRequest;
import br.com.comandavision.api.pagamento.dto.ResumoFinanceiroResponse;
import br.com.comandavision.api.pix.CobrancaPixRepository;

@Service
public class PagamentoService {
        private final PagamentoRepository pagamentoRepository;
        private final ComandaRepository comandaRepository;
        private final ItemComandaRepository itemComandaRepository;
        private final CobrancaPixRepository cobrancaPixRepository;

        public PagamentoService(
                        PagamentoRepository pagamentoRepository,
                        ComandaRepository comandaRepository,
                        ItemComandaRepository itemComandaRepository,
                        CobrancaPixRepository cobrancaPixRepository) {

                this.pagamentoRepository = pagamentoRepository;
                this.comandaRepository = comandaRepository;
                this.itemComandaRepository = itemComandaRepository;
                this.cobrancaPixRepository = cobrancaPixRepository;
        }

        @Transactional
        public PagamentoResponse registrar(
                        Long comandaId,
                        RegistrarPagamentoRequest request,
                        String chaveIdempotencia) {

                return registrar(comandaId, request, ChaveIdempotencia.obrigatoria(chaveIdempotencia));
        }

        @Transactional
        public PagamentoResponse registrar(
                        Long comandaId,
                        RegistrarPagamentoRequest request,
                        String chaveIdempotencia,
                        UUID usuarioId) {

                ChaveIdempotencia.Resolucao resolucao = ChaveIdempotencia.resolverTemporaria(
                                chaveIdempotencia,
                                usuarioId,
                                "pagamento-manual",
                                comandaId,
                                List.of(
                                                request.forma().name(),
                                                request.valor().stripTrailingZeros().toPlainString(),
                                                Objects.toString(request.referenciaExterna(), "")),
                                Instant.now());
                return registrar(comandaId, request, resolucao);
        }

        private PagamentoResponse registrar(
                        Long comandaId,
                        RegistrarPagamentoRequest request,
                        ChaveIdempotencia.Resolucao resolucao) {

                String chave = resolucao.efetiva();

                Pagamento repetido = buscarRepetido(resolucao);
                if (repetido != null) {
                        validarRepeticao(repetido, comandaId, request);
                        return PagamentoResponse.from(repetido);
                }

                // Serializa o cálculo do saldo e a reserva/registro para a mesma comanda.
                Comanda comanda = comandaRepository.findByIdForUpdate(comandaId)
                                .orElseThrow(() -> new ComandaNaoEncontradaException(comandaId));

                repetido = buscarRepetido(resolucao);
                if (repetido != null) {
                        validarRepeticao(repetido, comandaId, request);
                        return PagamentoResponse.from(repetido);
                }

                if (comanda.getStatus() != StatusComanda.FECHADA) {
                        throw new ComandaNaoPodeReceberPagamentoException(comandaId, comanda.getStatus());
                }

                List<ItemComanda> itens = itemComandaRepository.findByComandaIdOrderByCriadoEmAsc(comandaId);

                BigDecimal totalComanda = itens.stream()
                                .map(ItemComanda::calcularSubtotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                List<Pagamento> pagamentos = pagamentoRepository.findByComandaIdOrderByCriadoEmAsc(comandaId);

                BigDecimal totalPago = pagamentos.stream()
                                .filter(Pagamento::estaConfirmado)
                                .map(Pagamento::getValor)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal saldoRestante = totalComanda.subtract(totalPago);

                if (request.valor().compareTo(saldoRestante) > 0) {
                        throw new ValorPagamentoExcedeSaldoException(request.valor(), saldoRestante);
                }

                // Pix aguardando pagamento reserva parte do saldo: evita cobrar duas vezes o mesmo valor.
                BigDecimal totalPendente = pagamentos.stream()
                                .filter(pagamento -> pagamento.getStatus() == StatusPagamento.PENDENTE)
                                .map(Pagamento::getValor)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (request.valor().compareTo(saldoRestante.subtract(totalPendente)) > 0) {
                        throw new CobrancaPixPendenteException(totalPendente);
                }

                Pagamento pagamento = new Pagamento(comanda, request.forma(), request.valor(), chave);

                pagamento.confirmar(request.referenciaExterna());

                Pagamento pagamentoSalvo = pagamentoRepository.saveAndFlush(pagamento);

                return PagamentoResponse.from(pagamentoSalvo);
        }

        private Pagamento buscarRepetido(ChaveIdempotencia.Resolucao resolucao) {
                for (String candidata : resolucao.candidatas()) {
                        Pagamento encontrado = pagamentoRepository.findByChaveIdempotencia(candidata).orElse(null);
                        if (encontrado != null) {
                                return encontrado;
                        }
                }
                return null;
        }

        @Transactional
        public PagamentoResponse cancelar(Long comandaId, Long pagamentoId) {
                comandaRepository.findById(comandaId)
                                .orElseThrow(() -> new ComandaNaoEncontradaException(comandaId));

                Pagamento pagamento = pagamentoRepository.findByIdAndComandaId(pagamentoId, comandaId)
                                .orElseThrow(() -> new PagamentoNaoEncontradoException(pagamentoId, comandaId));

                pagamento.cancelar();

                return PagamentoResponse.from(pagamento);
        }

        @Transactional
        public PagamentoResponse corrigirRegistro(
                        Long comandaId,
                        Long pagamentoId,
                        CorrigirPagamentoRequest request,
                        UUID usuarioId) {
                comandaRepository.findById(comandaId)
                                .orElseThrow(() -> new ComandaNaoEncontradaException(comandaId));

                Pagamento pagamento = pagamentoRepository.findWithLockByIdAndComandaId(pagamentoId, comandaId)
                                .orElseThrow(() -> new PagamentoNaoEncontradoException(pagamentoId, comandaId));

                if (cobrancaPixRepository.existsByPagamentoId(pagamentoId)) {
                        throw new OperacaoPagamentoInvalidaException(
                                        "Uma cobrança Pix integrada não pode ser corrigida localmente. "
                                                        + "Realize a devolução no Mercado Pago e aguarde a confirmação do provedor "
                                                        + "antes de atualizar o estado no ComandaVision");
                }

                pagamento.corrigirRegistro(request.motivo(), usuarioId);

                return PagamentoResponse.from(pagamento);
        }

        @Transactional(readOnly = true)
        public List<PagamentoResponse> listar(Long comandaId) {
                comandaRepository.findById(comandaId)
                                .orElseThrow(() -> new ComandaNaoEncontradaException(comandaId));
                List<Pagamento> pagamentos = pagamentoRepository.findByComandaIdOrderByCriadoEmAsc(comandaId);
                Set<Long> pagamentosPixIntegrados = cobrancaPixRepository.findPagamentoIdsByComandaId(comandaId)
                                .stream()
                                .collect(Collectors.toSet());
                return pagamentos.stream()
                                .map(pagamento -> PagamentoResponse.from(
                                                pagamento,
                                                pagamentosPixIntegrados.contains(pagamento.getId())))
                                .toList();
        }

        @Transactional(readOnly = true)
        public ResumoFinanceiroResponse buscarResumo(Long comandaId) {
                comandaRepository.findById(comandaId)
                                .orElseThrow(() -> new ComandaNaoEncontradaException(comandaId));

                List<ItemComanda> itens = itemComandaRepository
                                .findByComandaIdOrderByCriadoEmAsc(comandaId);

                BigDecimal totalComanda = itens.stream()
                                .map(ItemComanda::calcularSubtotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                List<Pagamento> pagamentos = pagamentoRepository
                                .findByComandaIdOrderByCriadoEmAsc(comandaId);

                BigDecimal totalPago = pagamentos.stream()
                                .filter(Pagamento::estaConfirmado)
                                .map(Pagamento::getValor)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal saldoRestante = totalComanda.subtract(totalPago);

                SituacaoPagamento situacao;

                if (totalPago.compareTo(BigDecimal.ZERO) == 0) {
                        situacao = SituacaoPagamento.NAO_PAGO;
                } else if (totalPago.compareTo(totalComanda) < 0) {
                        situacao = SituacaoPagamento.PARCIAL;
                } else {
                        situacao = SituacaoPagamento.PAGO;
                }

                return new ResumoFinanceiroResponse(
                                comandaId,
                                totalComanda,
                                totalPago,
                                saldoRestante,
                                situacao);
        }

        private static void validarRepeticao(
                        Pagamento pagamento,
                        Long comandaId,
                        RegistrarPagamentoRequest request) {
                boolean mesmaOperacao = Objects.equals(pagamento.getComanda().getId(), comandaId)
                                && pagamento.getForma() == request.forma()
                                && pagamento.getValor().compareTo(request.valor()) == 0
                                && Objects.equals(pagamento.getReferenciaExterna(), request.referenciaExterna());

                if (!mesmaOperacao) {
                        throw new ChaveIdempotenciaEmConflitoException();
                }
        }
}

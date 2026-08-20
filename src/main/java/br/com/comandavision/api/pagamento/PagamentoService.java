package br.com.comandavision.api.pagamento;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.comandavision.api.comanda.Comanda;
import br.com.comandavision.api.comanda.ComandaNaoEncontradaException;
import br.com.comandavision.api.comanda.ComandaRepository;
import br.com.comandavision.api.comanda.ItemComanda;
import br.com.comandavision.api.comanda.ItemComandaRepository;
import br.com.comandavision.api.comanda.StatusComanda;
import br.com.comandavision.api.pagamento.dto.PagamentoResponse;
import br.com.comandavision.api.pagamento.dto.RegistrarPagamentoRequest;

@Service
public class PagamentoService {
    private final PagamentoRepository pagamentoRepository;
    private final ComandaRepository comandaRepository;
    private final ItemComandaRepository itemComandaRepository;

    public PagamentoService(
            PagamentoRepository pagamentoRepository,
            ComandaRepository comandaRepository,
            ItemComandaRepository itemComandaRepository) {

        this.pagamentoRepository = pagamentoRepository;
        this.comandaRepository = comandaRepository;
        this.itemComandaRepository = itemComandaRepository;
    }

    @Transactional
    public PagamentoResponse registrar(
            Long comandaId,
            RegistrarPagamentoRequest request) {

        Comanda comanda = comandaRepository.findById(comandaId)
                .orElseThrow(() -> new ComandaNaoEncontradaException(comandaId));

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

        Pagamento pagamento = new Pagamento(comanda, request.forma(), request.valor());

        pagamento.confirmar(request.referenciaExterna());

        Pagamento pagamentoSalvo = pagamentoRepository.save(pagamento);

        return PagamentoResponse.from(pagamentoSalvo);
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
    public PagamentoResponse estornar(Long comandaId, Long pagamentoId) {
        comandaRepository.findById(comandaId)
                .orElseThrow(() -> new ComandaNaoEncontradaException(comandaId));

        Pagamento pagamento = pagamentoRepository.findByIdAndComandaId(pagamentoId, comandaId)
                .orElseThrow(() -> new PagamentoNaoEncontradoException(pagamentoId, comandaId));

        pagamento.estornar();

        return PagamentoResponse.from(pagamento);
    }

    @Transactional(readOnly = true)
    public List<PagamentoResponse> listar(Long comandaId) {
        comandaRepository.findById(comandaId)
                .orElseThrow(() -> new ComandaNaoEncontradaException(comandaId));
        List<Pagamento> pagamentos = pagamentoRepository.findByComandaIdOrderByCriadoEmAsc(comandaId);
        return pagamentos.stream()
                .map(PagamentoResponse::from)
                .toList();
    }
}

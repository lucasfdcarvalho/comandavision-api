package br.com.comandavision.api.pagamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.comandavision.api.categoria.Categoria;
import br.com.comandavision.api.comanda.Comanda;
import br.com.comandavision.api.comanda.ComandaRepository;
import br.com.comandavision.api.comanda.ItemComanda;
import br.com.comandavision.api.comanda.ItemComandaRepository;
import br.com.comandavision.api.pagamento.dto.PagamentoResponse;
import br.com.comandavision.api.pagamento.dto.RegistrarPagamentoRequest;
import br.com.comandavision.api.pagamento.dto.ResumoFinanceiroResponse;
import br.com.comandavision.api.produto.Produto;

@ExtendWith(MockitoExtension.class)
public class PagamentoServiceTest {

    @Mock
    private PagamentoRepository pagamentoRepository;

    @Mock
    private ComandaRepository comandaRepository;

    @Mock
    private ItemComandaRepository itemComandaRepository;

    @InjectMocks
    private PagamentoService pagamentoService;

    @Test
    public void naoDeveRegistrarPagamentoEmComandaAberta() {
        Long comandaId = 10L;

        Comanda comanda = new Comanda("Mesa 10", null);

        RegistrarPagamentoRequest request = new RegistrarPagamentoRequest(
                FormaPagamento.PIX,
                new BigDecimal("20.00"),
                "PIX-TESTE");

        when(comandaRepository.findById(comandaId))
                .thenReturn(Optional.of(comanda));

        assertThrows(
                ComandaNaoPodeReceberPagamentoException.class,
                () -> pagamentoService.registrar(
                        comandaId,
                        request));

        verifyNoInteractions(
                itemComandaRepository,
                pagamentoRepository);
    }

    @Test
    public void naoDeveRegistrarPagamentoMaiorQueSaldoRestante() {
        Long comandaId = 10L;

        Comanda comanda = new Comanda("Mesa 10", null);
        comanda.fechar();

        Categoria categoria = new Categoria("Bebida", null);

        Produto produto = new Produto(categoria, "Coca-cola", null, new BigDecimal("10.00"));

        ItemComanda item = new ItemComanda(comanda, produto, 2, null);

        Pagamento pagamentoExistente = new Pagamento(comanda, FormaPagamento.PIX, new BigDecimal("15.00"));

        pagamentoExistente.confirmar("PIX-ANTERIOR");

        RegistrarPagamentoRequest request = new RegistrarPagamentoRequest(FormaPagamento.DINHEIRO,
                new BigDecimal("6.00"), null);

        when(comandaRepository.findById(comandaId))
                .thenReturn(Optional.of(comanda));

        when(itemComandaRepository.findByComandaIdOrderByCriadoEmAsc(comandaId))
                .thenReturn(List.of(item));

        when(pagamentoRepository.findByComandaIdOrderByCriadoEmAsc(comandaId))
                .thenReturn(List.of(pagamentoExistente));

        assertThrows(ValorPagamentoExcedeSaldoException.class,
                () -> pagamentoService.registrar(comandaId, request));

        verify(pagamentoRepository, never())
                .save(any(Pagamento.class));
    }

    @Test
    public void deveRegistrarPagamentoIgualAoSaldoRestante() {
        Long comandaId = 10L;

        Comanda comanda = new Comanda("Mesa 10", null);
        comanda.fechar();

        Categoria categoria = new Categoria("Bebidas", null);

        Produto produto = new Produto(
                categoria,
                "Coca-Cola",
                null,
                new BigDecimal("10.00"));

        ItemComanda item = new ItemComanda(
                comanda,
                produto,
                2,
                null);

        Pagamento pagamentoExistente = new Pagamento(
                comanda,
                FormaPagamento.PIX,
                new BigDecimal("15.00"));

        pagamentoExistente.confirmar("PIX-ANTERIOR");

        RegistrarPagamentoRequest request = new RegistrarPagamentoRequest(
                FormaPagamento.DINHEIRO,
                new BigDecimal("5.00"),
                null);

        when(comandaRepository.findById(comandaId))
                .thenReturn(Optional.of(comanda));

        when(itemComandaRepository
                .findByComandaIdOrderByCriadoEmAsc(comandaId))
                .thenReturn(List.of(item));

        when(pagamentoRepository
                .findByComandaIdOrderByCriadoEmAsc(comandaId))
                .thenReturn(List.of(pagamentoExistente));

        when(pagamentoRepository.save(any(Pagamento.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        PagamentoResponse resposta = pagamentoService.registrar(comandaId, request);

        assertEquals(
                StatusPagamento.CONFIRMADO,
                resposta.status());

        assertEquals(
                new BigDecimal("5.00"),
                resposta.valor());

        assertNotNull(resposta.pagoEm());

        verify(pagamentoRepository)
                .save(any(Pagamento.class));
    }

    @Test
    public void naoDeveEstornarPagamentoDeOutraComanda() {
        Long comandaId = 10L;
        Long pagamentoId = 50L;

        Comanda comanda = new Comanda("Mesa 10", null);
        comanda.fechar();

        when(comandaRepository.findById(comandaId))
                .thenReturn(Optional.of(comanda));

        when(pagamentoRepository.findByIdAndComandaId(
                pagamentoId,
                comandaId))
                .thenReturn(Optional.empty());

        assertThrows(
                PagamentoNaoEncontradoException.class,
                () -> pagamentoService.estornar(
                        comandaId,
                        pagamentoId));
    }

    @Test
    public void deveEstornarPagamentoConfirmado() {
        Long comandaId = 10L;
        Long pagamentoId = 50L;

        Comanda comanda = new Comanda("Mesa 10", null);
        comanda.fechar();

        Pagamento pagamento = new Pagamento(
                comanda,
                FormaPagamento.PIX,
                new BigDecimal("20.00"));

        pagamento.confirmar("PIX-TESTE");

        var dataDoPagamento = pagamento.getPagoEm();

        when(comandaRepository.findById(comandaId))
                .thenReturn(Optional.of(comanda));

        when(pagamentoRepository.findByIdAndComandaId(
                pagamentoId,
                comandaId))
                .thenReturn(Optional.of(pagamento));

        PagamentoResponse resposta = pagamentoService.estornar(
                comandaId,
                pagamentoId);

        assertEquals(
                StatusPagamento.ESTORNADO,
                resposta.status());

        assertEquals(
                dataDoPagamento,
                resposta.pagoEm());
    }

    @Test
    public void deveLiberarSaldoDepoisDoEstorno() {
        Long comandaId = 10L;

        Comanda comanda = new Comanda("Mesa 10", null);
        comanda.fechar();

        Categoria categoria = new Categoria("Bebidas", null);

        Produto produto = new Produto(
                categoria,
                "Coca-Cola",
                null,
                new BigDecimal("10.00"));

        ItemComanda item = new ItemComanda(
                comanda,
                produto,
                2,
                null);

        Pagamento pagamento = new Pagamento(
                comanda,
                FormaPagamento.PIX,
                new BigDecimal("20.00"));

        pagamento.confirmar("PIX-TESTE");
        pagamento.estornar();

        when(comandaRepository.findById(comandaId))
                .thenReturn(Optional.of(comanda));

        when(itemComandaRepository
                .findByComandaIdOrderByCriadoEmAsc(comandaId))
                .thenReturn(List.of(item));

        when(pagamentoRepository
                .findByComandaIdOrderByCriadoEmAsc(comandaId))
                .thenReturn(List.of(pagamento));

        ResumoFinanceiroResponse resumo = pagamentoService.buscarResumo(comandaId);

        assertEquals(
                new BigDecimal("20.00"),
                resumo.totalComanda());

        assertEquals(
                BigDecimal.ZERO,
                resumo.totalPago());

        assertEquals(
                new BigDecimal("20.00"),
                resumo.saldoRestante());

        assertEquals(
                SituacaoPagamento.NAO_PAGO,
                resumo.situacao());
    }

}

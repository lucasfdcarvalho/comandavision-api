package br.com.comandavision.api.pagamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.comandavision.api.categoria.Categoria;
import br.com.comandavision.api.comanda.Comanda;
import br.com.comandavision.api.comanda.ComandaRepository;
import br.com.comandavision.api.comanda.ItemComanda;
import br.com.comandavision.api.comanda.ItemComandaRepository;
import br.com.comandavision.api.pagamento.dto.PagamentoResponse;
import br.com.comandavision.api.pagamento.dto.CorrigirPagamentoRequest;
import br.com.comandavision.api.pagamento.dto.RegistrarPagamentoRequest;
import br.com.comandavision.api.pagamento.dto.ResumoFinanceiroResponse;
import br.com.comandavision.api.pix.CobrancaPixPendenteException;
import br.com.comandavision.api.pix.CobrancaPixRepository;
import br.com.comandavision.api.produto.Produto;

@ExtendWith(MockitoExtension.class)
public class PagamentoServiceTest {
    private static final String CHAVE = "5c6ad2ad-b438-4bd1-9b2c-aebcf3cc87ef";

    @Mock
    private PagamentoRepository pagamentoRepository;

    @Mock
    private ComandaRepository comandaRepository;

    @Mock
    private ItemComandaRepository itemComandaRepository;

    @Mock
    private CobrancaPixRepository cobrancaPixRepository;

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

        when(comandaRepository.findByIdForUpdate(comandaId))
                .thenReturn(Optional.of(comanda));

        assertThrows(
                ComandaNaoPodeReceberPagamentoException.class,
                () -> pagamentoService.registrar(
                        comandaId,
                        request,
                        CHAVE));

        verifyNoInteractions(itemComandaRepository);
        verify(pagamentoRepository, never()).saveAndFlush(any());
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

        when(comandaRepository.findByIdForUpdate(comandaId))
                .thenReturn(Optional.of(comanda));

        when(itemComandaRepository.findByComandaIdOrderByCriadoEmAsc(comandaId))
                .thenReturn(List.of(item));

        when(pagamentoRepository.findByComandaIdOrderByCriadoEmAsc(comandaId))
                .thenReturn(List.of(pagamentoExistente));

        assertThrows(ValorPagamentoExcedeSaldoException.class,
                () -> pagamentoService.registrar(comandaId, request, CHAVE));

        verify(pagamentoRepository, never())
                .saveAndFlush(any(Pagamento.class));
    }

    @Test
    public void naoDeveRegistrarValorReservadoPorPixPendente() {
        Long comandaId = 10L;

        Comanda comanda = new Comanda("Mesa 10", null);
        comanda.fechar();

        Produto produto = new Produto(new Categoria("Bebida", null), "Coca-cola", null, new BigDecimal("10.00"));
        ItemComanda item = new ItemComanda(comanda, produto, 2, null);

        // Pix de R$ 15 aguardando pagamento: sobram só R$ 5 para outras formas.
        Pagamento pixPendente = new Pagamento(comanda, FormaPagamento.PIX, new BigDecimal("15.00"));

        RegistrarPagamentoRequest request = new RegistrarPagamentoRequest(FormaPagamento.DINHEIRO,
                new BigDecimal("6.00"), null);

        when(comandaRepository.findByIdForUpdate(comandaId))
                .thenReturn(Optional.of(comanda));

        when(itemComandaRepository.findByComandaIdOrderByCriadoEmAsc(comandaId))
                .thenReturn(List.of(item));

        when(pagamentoRepository.findByComandaIdOrderByCriadoEmAsc(comandaId))
                .thenReturn(List.of(pixPendente));

        assertThrows(CobrancaPixPendenteException.class,
                () -> pagamentoService.registrar(comandaId, request, CHAVE));

        verify(pagamentoRepository, never())
                .saveAndFlush(any(Pagamento.class));
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

        when(comandaRepository.findByIdForUpdate(comandaId))
                .thenReturn(Optional.of(comanda));

        when(itemComandaRepository
                .findByComandaIdOrderByCriadoEmAsc(comandaId))
                .thenReturn(List.of(item));

        when(pagamentoRepository
                .findByComandaIdOrderByCriadoEmAsc(comandaId))
                .thenReturn(List.of(pagamentoExistente));

        when(pagamentoRepository.saveAndFlush(any(Pagamento.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        PagamentoResponse resposta = pagamentoService.registrar(comandaId, request, CHAVE);

        assertEquals(
                StatusPagamento.CONFIRMADO,
                resposta.status());

        assertEquals(
                new BigDecimal("5.00"),
                resposta.valor());

        assertNotNull(resposta.pagoEm());

        verify(pagamentoRepository)
                .saveAndFlush(any(Pagamento.class));
    }

    @Test
    public void deveRepetirPagamentoComMesmaChaveSemDuplicar() {
        Long comandaId = 10L;
        Comanda comanda = new Comanda("Mesa 10", null);
        ReflectionTestUtils.setField(comanda, "id", comandaId);
        comanda.fechar();

        Pagamento existente = new Pagamento(
                comanda,
                FormaPagamento.CARTAO_DEBITO,
                new BigDecimal("20.00"),
                CHAVE);
        ReflectionTestUtils.setField(existente, "id", 50L);
        existente.confirmar("NSU-123");

        RegistrarPagamentoRequest request = new RegistrarPagamentoRequest(
                FormaPagamento.CARTAO_DEBITO,
                new BigDecimal("20.00"),
                "NSU-123");
        when(pagamentoRepository.findByChaveIdempotencia(CHAVE)).thenReturn(Optional.of(existente));

        PagamentoResponse resposta = pagamentoService.registrar(comandaId, request, CHAVE);

        assertEquals(50L, resposta.id());
        assertEquals(StatusPagamento.CONFIRMADO, resposta.status());
        verify(comandaRepository, never()).findByIdForUpdate(any());
        verify(pagamentoRepository, never()).saveAndFlush(any());
    }

    @Test
    public void deveDeduplicarRetryDeClienteLegadoSemCabecalho() {
        Long comandaId = 10L;
        UUID usuarioId = UUID.fromString("b35bbcb0-1128-45a2-bd16-cb8d99be154f");
        Comanda comanda = new Comanda("Mesa 10", null);
        ReflectionTestUtils.setField(comanda, "id", comandaId);
        comanda.fechar();
        Produto produto = new Produto(
                new Categoria("Bebidas", null), "Suco", null, new BigDecimal("100.00"));
        ItemComanda item = new ItemComanda(comanda, produto, 1, null);
        RegistrarPagamentoRequest request = new RegistrarPagamentoRequest(
                FormaPagamento.DINHEIRO, new BigDecimal("20.00"), null);
        AtomicReference<Pagamento> salvo = new AtomicReference<>();

        when(pagamentoRepository.findByChaveIdempotencia(any())).thenAnswer(chamada -> {
            Pagamento pagamento = salvo.get();
            return pagamento != null && pagamento.getChaveIdempotencia().equals(chamada.getArgument(0))
                    ? Optional.of(pagamento)
                    : Optional.empty();
        });
        when(comandaRepository.findByIdForUpdate(comandaId)).thenReturn(Optional.of(comanda));
        when(itemComandaRepository.findByComandaIdOrderByCriadoEmAsc(comandaId)).thenReturn(List.of(item));
        when(pagamentoRepository.findByComandaIdOrderByCriadoEmAsc(comandaId)).thenReturn(List.of());
        when(pagamentoRepository.saveAndFlush(any(Pagamento.class))).thenAnswer(chamada -> {
            Pagamento pagamento = chamada.getArgument(0);
            ReflectionTestUtils.setField(pagamento, "id", 50L);
            salvo.set(pagamento);
            return pagamento;
        });

        PagamentoResponse primeira = pagamentoService.registrar(comandaId, request, null, usuarioId);
        PagamentoResponse repetida = pagamentoService.registrar(comandaId, request, null, usuarioId);

        assertEquals(50L, primeira.id());
        assertEquals(primeira.id(), repetida.id());
        assertNotNull(salvo.get().getChaveIdempotencia());
        verify(comandaRepository, times(1)).findByIdForUpdate(comandaId);
        verify(pagamentoRepository, times(1)).saveAndFlush(any(Pagamento.class));
    }

    @Test
    public void deveSerializarPagamentosConcorrentesDaMesmaComanda() throws Exception {
        Long comandaId = 10L;
        Comanda comanda = new Comanda("Mesa 10", null);
        ReflectionTestUtils.setField(comanda, "id", comandaId);
        comanda.fechar();
        Produto produto = new Produto(
                new Categoria("Bebidas", null),
                "Suco",
                null,
                new BigDecimal("100.00"));
        ItemComanda item = new ItemComanda(comanda, produto, 1, null);
        RegistrarPagamentoRequest request = new RegistrarPagamentoRequest(
                FormaPagamento.DINHEIRO,
                new BigDecimal("70.00"),
                null);

        ReentrantLock bloqueioDoBancoSimulado = new ReentrantLock();
        CopyOnWriteArrayList<Pagamento> registrados = new CopyOnWriteArrayList<>();
        when(pagamentoRepository.findByChaveIdempotencia(any())).thenReturn(Optional.empty());
        when(comandaRepository.findByIdForUpdate(comandaId)).thenAnswer(chamada -> {
            bloqueioDoBancoSimulado.lock();
            return Optional.of(comanda);
        });
        when(itemComandaRepository.findByComandaIdOrderByCriadoEmAsc(comandaId)).thenReturn(List.of(item));
        when(pagamentoRepository.findByComandaIdOrderByCriadoEmAsc(comandaId))
                .thenAnswer(chamada -> List.copyOf(registrados));
        when(pagamentoRepository.saveAndFlush(any(Pagamento.class))).thenAnswer(chamada -> {
            Pagamento pagamento = chamada.getArgument(0);
            registrados.add(pagamento);
            bloqueioDoBancoSimulado.unlock();
            return pagamento;
        });

        CyclicBarrier inicioSimultaneo = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeira = executor.submit(() -> {
                inicioSimultaneo.await(5, TimeUnit.SECONDS);
                try {
                    return pagamentoService.registrar(
                            comandaId, request, "6080246f-cdc7-44e2-8e0c-c78061f908a3");
                } catch (RuntimeException exception) {
                    return exception;
                }
            });
            var segunda = executor.submit(() -> {
                inicioSimultaneo.await(5, TimeUnit.SECONDS);
                try {
                    return pagamentoService.registrar(
                            comandaId, request, "9f8dfa4a-4b03-4d47-86e4-61a28bba05f4");
                } catch (RuntimeException exception) {
                    return exception;
                }
            });

            List<Object> resultados = List.of(
                    primeira.get(10, TimeUnit.SECONDS),
                    segunda.get(10, TimeUnit.SECONDS));
            assertEquals(1, resultados.stream().filter(PagamentoResponse.class::isInstance).count());
            assertEquals(1, resultados.stream().filter(ValorPagamentoExcedeSaldoException.class::isInstance).count());
        }

        assertEquals(1, registrados.size());
        verify(pagamentoRepository, times(1)).saveAndFlush(any(Pagamento.class));
    }

    @Test
    public void naoDeveCorrigirPagamentoDeOutraComanda() {
        Long comandaId = 10L;
        Long pagamentoId = 50L;

        Comanda comanda = new Comanda("Mesa 10", null);
        comanda.fechar();

        when(comandaRepository.findById(comandaId))
                .thenReturn(Optional.of(comanda));

        when(pagamentoRepository.findWithLockByIdAndComandaId(
                pagamentoId,
                comandaId))
                .thenReturn(Optional.empty());

        assertThrows(
                PagamentoNaoEncontradoException.class,
                () -> pagamentoService.corrigirRegistro(
                        comandaId,
                        pagamentoId,
                        new CorrigirPagamentoRequest("Registro incorreto"),
                        UUID.fromString("b35bbcb0-1128-45a2-bd16-cb8d99be154f")));
    }

    @Test
    public void deveCorrigirPagamentoManualComAuditoria() {
        Long comandaId = 10L;
        Long pagamentoId = 50L;

        Comanda comanda = new Comanda("Mesa 10", null);
        comanda.fechar();

        Pagamento pagamento = new Pagamento(
                comanda,
                FormaPagamento.DINHEIRO,
                new BigDecimal("20.00"));

        pagamento.confirmar("PIX-TESTE");

        var dataDoPagamento = pagamento.getPagoEm();
        UUID usuarioId = UUID.fromString("b35bbcb0-1128-45a2-bd16-cb8d99be154f");

        when(comandaRepository.findById(comandaId))
                .thenReturn(Optional.of(comanda));

        when(pagamentoRepository.findWithLockByIdAndComandaId(
                pagamentoId,
                comandaId))
                .thenReturn(Optional.of(pagamento));

        PagamentoResponse resposta = pagamentoService.corrigirRegistro(
                comandaId, pagamentoId,
                new CorrigirPagamentoRequest("Forma de pagamento informada incorretamente"),
                usuarioId);

        assertEquals(
                StatusPagamento.ESTORNADO,
                resposta.status());

        assertEquals(
                dataDoPagamento,
                resposta.pagoEm());
        assertEquals("Forma de pagamento informada incorretamente", resposta.motivoCorrecao());
        assertNotNull(resposta.corrigidoEm());
        assertEquals(usuarioId, pagamento.getCorrigidoPor());
    }

    @Test
    public void devePermitirCorrigirPixRegistradoManualmente() {
        Long comandaId = 10L;
        Long pagamentoId = 50L;
        Comanda comanda = new Comanda("Mesa 10", null);
        comanda.fechar();
        Pagamento pixManual = new Pagamento(comanda, FormaPagamento.PIX, new BigDecimal("20.00"));
        pixManual.confirmar("PIX-INFORMADO-MANUALMENTE");

        when(comandaRepository.findById(comandaId)).thenReturn(Optional.of(comanda));
        when(pagamentoRepository.findWithLockByIdAndComandaId(pagamentoId, comandaId))
                .thenReturn(Optional.of(pixManual));
        when(cobrancaPixRepository.existsByPagamentoId(pagamentoId)).thenReturn(false);

        PagamentoResponse resposta = pagamentoService.corrigirRegistro(
                comandaId, pagamentoId,
                new CorrigirPagamentoRequest("Pix registrado na comanda errada"),
                UUID.fromString("b35bbcb0-1128-45a2-bd16-cb8d99be154f"));

        assertEquals(StatusPagamento.ESTORNADO, resposta.status());
        assertEquals("Pix registrado na comanda errada", resposta.motivoCorrecao());
    }

    @Test
    public void deveDistinguirPixIntegradoPeloVinculoDaCobranca() {
        Long comandaId = 10L;
        Comanda comanda = new Comanda("Mesa 10", null);
        ReflectionTestUtils.setField(comanda, "id", comandaId);
        comanda.fechar();

        Pagamento pixManual = new Pagamento(comanda, FormaPagamento.PIX, new BigDecimal("10.00"));
        ReflectionTestUtils.setField(pixManual, "id", 50L);
        pixManual.confirmar("PIX-MANUAL");

        Pagamento pixIntegrado = new Pagamento(comanda, FormaPagamento.PIX, new BigDecimal("20.00"));
        ReflectionTestUtils.setField(pixIntegrado, "id", 51L);
        pixIntegrado.confirmar("ORD-INTEGRADA");
        when(comandaRepository.findById(comandaId)).thenReturn(Optional.of(comanda));
        when(pagamentoRepository.findByComandaIdOrderByCriadoEmAsc(comandaId))
                .thenReturn(List.of(pixManual, pixIntegrado));
        when(cobrancaPixRepository.findPagamentoIdsByComandaId(comandaId)).thenReturn(List.of(51L));

        List<PagamentoResponse> resposta = pagamentoService.listar(comandaId);

        assertEquals(false, resposta.get(0).pixIntegrado());
        assertEquals(true, resposta.get(1).pixIntegrado());
    }

    @Test
    public void deveBloquearCorrecaoLocalQuandoExisteCobrancaPixIntegrada() {
        Long comandaId = 10L;
        Long pagamentoId = 50L;
        Comanda comanda = new Comanda("Mesa 10", null);
        comanda.fechar();
        Pagamento pix = new Pagamento(comanda, FormaPagamento.PIX, new BigDecimal("20.00"));
        pix.confirmar("ORD-PIX");

        when(comandaRepository.findById(comandaId)).thenReturn(Optional.of(comanda));
        when(pagamentoRepository.findWithLockByIdAndComandaId(pagamentoId, comandaId))
                .thenReturn(Optional.of(pix));
        when(cobrancaPixRepository.existsByPagamentoId(pagamentoId)).thenReturn(true);

        OperacaoPagamentoInvalidaException erro = assertThrows(
                OperacaoPagamentoInvalidaException.class,
                () -> pagamentoService.corrigirRegistro(
                        comandaId, pagamentoId,
                        new CorrigirPagamentoRequest("Cliente solicitou devolução"),
                        UUID.fromString("b35bbcb0-1128-45a2-bd16-cb8d99be154f")));

        assertTrue(erro.getMessage().contains("não pode ser corrigida localmente"));
        assertTrue(erro.getMessage().contains("Mercado Pago"));
        assertTrue(erro.getMessage().contains("confirmação do provedor"));
        assertEquals(StatusPagamento.CONFIRMADO, pix.getStatus());
        assertEquals(null, pix.getMotivoCorrecao());
    }

    @Test
    public void deveLiberarSaldoDepoisDaCorrecaoManual() {
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
                FormaPagamento.DINHEIRO,
                new BigDecimal("20.00"));

        pagamento.confirmar("PIX-TESTE");
        pagamento.corrigirRegistro(
                "Pagamento duplicado",
                UUID.fromString("b35bbcb0-1128-45a2-bd16-cb8d99be154f"));

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

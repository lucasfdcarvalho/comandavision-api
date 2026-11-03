package br.com.comandavision.api.pix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import br.com.comandavision.api.categoria.Categoria;
import br.com.comandavision.api.comanda.Comanda;
import br.com.comandavision.api.comanda.ComandaRepository;
import br.com.comandavision.api.comanda.ItemComanda;
import br.com.comandavision.api.comanda.ItemComandaRepository;
import br.com.comandavision.api.pagamento.ComandaNaoPodeReceberPagamentoException;
import br.com.comandavision.api.pagamento.ChaveIdempotenciaEmConflitoException;
import br.com.comandavision.api.pagamento.FormaPagamento;
import br.com.comandavision.api.pagamento.OperacaoPagamentoInvalidaException;
import br.com.comandavision.api.pagamento.Pagamento;
import br.com.comandavision.api.pagamento.PagamentoRepository;
import br.com.comandavision.api.pagamento.StatusPagamento;
import br.com.comandavision.api.pagamento.ValorPagamentoExcedeSaldoException;
import br.com.comandavision.api.pix.dto.CobrancaPixResponse;
import br.com.comandavision.api.pix.mercadopago.MercadoPagoClient;
import br.com.comandavision.api.pix.mercadopago.PedidoMercadoPago;
import br.com.comandavision.api.produto.Produto;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PixServiceTest {
    private static final Long COMANDA_ID = 10L;
    private static final String PEDIDO_ID = "ORD01TESTE";
    private static final String CHAVE = "e1f0aebf-59c5-4da2-ac39-b2628f896f7b";

    @Mock
    private MercadoPagoClient mercadoPagoClient;
    @Mock
    private CobrancaPixRepository cobrancaPixRepository;
    @Mock
    private PagamentoRepository pagamentoRepository;
    @Mock
    private ComandaRepository comandaRepository;
    @Mock
    private ItemComandaRepository itemComandaRepository;
    // Mock: TransactionTemplate só repassa o callback, sem transação real.
    @Mock
    private PlatformTransactionManager gerenciadorTransacao;

    private PixService pixService;
    private Comanda comanda;
    private Pagamento ultimoPagamentoSalvo;

    @BeforeEach
    public void preparar() {
        pixService = new PixService(mercadoPagoClient, cobrancaPixRepository, pagamentoRepository,
                comandaRepository, itemComandaRepository, gerenciadorTransacao);

        comanda = new Comanda("Mesa 10", null);
        ReflectionTestUtils.setField(comanda, "id", COMANDA_ID);
        comanda.fechar();

        Produto produto = new Produto(new Categoria("Bebidas", null), "Suco", null, new BigDecimal("25.00"));
        when(comandaRepository.findById(COMANDA_ID)).thenReturn(Optional.of(comanda));
        when(comandaRepository.findByIdForUpdate(COMANDA_ID)).thenReturn(Optional.of(comanda));
        when(itemComandaRepository.findByComandaIdOrderByCriadoEmAsc(COMANDA_ID))
                .thenReturn(List.of(new ItemComanda(comanda, produto, 4, null)));
        when(mercadoPagoClient.estaConfigurado()).thenReturn(true);
        when(pagamentoRepository.saveAndFlush(any(Pagamento.class))).thenAnswer(chamada -> {
            Pagamento pagamento = chamada.getArgument(0);
            ReflectionTestUtils.setField(pagamento, "id", 1L);
            ultimoPagamentoSalvo = pagamento;
            return pagamento;
        });
        when(pagamentoRepository.findWithLockById(1L))
                .thenAnswer(chamada -> Optional.ofNullable(ultimoPagamentoSalvo));
        when(cobrancaPixRepository.saveAndFlush(any(CobrancaPix.class))).thenAnswer(chamada -> chamada.getArgument(0));
    }

    private static PedidoMercadoPago pedido(String status) {
        return new PedidoMercadoPago(PEDIDO_ID, status, null, new PedidoMercadoPago.Transacoes(List.of(
                new PedidoMercadoPago.PagamentoPedido("PAY01", status,
                        new PedidoMercadoPago.MetodoPagamento("pix", "00020126580014br.gov.bcb.pix", "iVBORw0KGgo=")))));
    }

    private Pagamento pagamentoPendente(String valor) {
        Pagamento pagamento = new Pagamento(comanda, FormaPagamento.PIX, new BigDecimal(valor));
        ReflectionTestUtils.setField(pagamento, "id", 1L);
        return pagamento;
    }

    private CobrancaPix cobranca(Pagamento pagamento, OffsetDateTime expiraEm) {
        ultimoPagamentoSalvo = pagamento;
        CobrancaPix cobranca = new CobrancaPix(pagamento, PEDIDO_ID, "00020126", "iVBOR", expiraEm);
        when(cobrancaPixRepository.findByPagamentoId(1L)).thenReturn(Optional.of(cobranca));
        when(pagamentoRepository.findWithLockById(1L)).thenReturn(Optional.of(pagamento));
        return cobranca;
    }

    @Test
    public void deveGerarPixPendenteComQrCode() {
        when(mercadoPagoClient.criarPedidoPix(eq(new BigDecimal("40.00")), anyString(), eq("PT30M"), eq(CHAVE)))
                .thenReturn(pedido("action_required"));

        CobrancaPixResponse resposta = pixService.gerar(COMANDA_ID, new BigDecimal("40.00"), CHAVE);

        assertEquals(StatusPagamento.PENDENTE, resposta.status());
        assertEquals("00020126580014br.gov.bcb.pix", resposta.qrCode());
        assertEquals("iVBORw0KGgo=", resposta.qrCodeBase64());
        assertEquals(new BigDecimal("40.00"), resposta.valor());
    }

    @Test
    public void naoDeveGerarPixParaComandaAberta() {
        Comanda aberta = new Comanda("Mesa 11", null);
        when(comandaRepository.findById(COMANDA_ID)).thenReturn(Optional.of(aberta));

        assertThrows(ComandaNaoPodeReceberPagamentoException.class,
                () -> pixService.gerar(COMANDA_ID, new BigDecimal("10.00"), CHAVE));

        verify(mercadoPagoClient, never()).criarPedidoPix(any(), anyString(), anyString(), anyString());
    }

    @Test
    public void naoDeveGerarPixAcimaDoSaldo() {
        assertThrows(ValorPagamentoExcedeSaldoException.class,
                () -> pixService.gerar(COMANDA_ID, new BigDecimal("100.01"), CHAVE));
    }

    @Test
    public void naoDeveCobrarValorJaReservadoPorOutroPix() {
        when(pagamentoRepository.findByComandaIdOrderByCriadoEmAsc(COMANDA_ID))
                .thenReturn(List.of(pagamentoPendente("80.00")));

        assertThrows(CobrancaPixPendenteException.class,
                () -> pixService.gerar(COMANDA_ID, new BigDecimal("30.00"), CHAVE));

        verify(mercadoPagoClient, never()).criarPedidoPix(any(), anyString(), anyString(), anyString());
    }

    @Test
    public void deveInformarQuandoPixNaoEstaConfigurado() {
        when(mercadoPagoClient.estaConfigurado()).thenReturn(false);

        assertThrows(PixIndisponivelException.class,
                () -> pixService.gerar(COMANDA_ID, new BigDecimal("10.00"), CHAVE));
    }

    @Test
    public void deveManterReservaParaReconciliarSeNaoConseguirSalvar() {
        when(mercadoPagoClient.criarPedidoPix(any(), anyString(), anyString(), anyString()))
                .thenReturn(pedido("action_required"));
        when(cobrancaPixRepository.saveAndFlush(any(CobrancaPix.class)))
                .thenThrow(new DataIntegrityViolationException("falha"));

        assertThrows(FalhaMercadoPagoException.class,
                () -> pixService.gerar(COMANDA_ID, new BigDecimal("10.00"), CHAVE));

        verify(mercadoPagoClient, never()).cancelarPedido(PEDIDO_ID);
    }

    @Test
    public void deveRepetirMesmaChaveSemCriarOutroPedido() {
        Pagamento pagamento = new Pagamento(comanda, FormaPagamento.PIX, new BigDecimal("40.00"), CHAVE);
        ReflectionTestUtils.setField(pagamento, "id", 1L);
        CobrancaPix cobranca = new CobrancaPix(
                pagamento,
                PEDIDO_ID,
                "00020126580014br.gov.bcb.pix",
                "iVBORw0KGgo=",
                OffsetDateTime.now().plusMinutes(20));
        when(pagamentoRepository.findByChaveIdempotencia(CHAVE)).thenReturn(Optional.of(pagamento));
        when(cobrancaPixRepository.findByPagamentoId(1L)).thenReturn(Optional.of(cobranca));

        CobrancaPixResponse resposta = pixService.gerar(COMANDA_ID, new BigDecimal("40.00"), CHAVE);

        assertEquals(PEDIDO_ID, cobranca.getPedidoMercadoPago());
        assertEquals(StatusPagamento.PENDENTE, resposta.status());
        verify(mercadoPagoClient, never()).criarPedidoPix(any(), anyString(), anyString(), anyString());
        verify(mercadoPagoClient, never()).buscarPedidoPorReferencia(anyString(), any(), any());
    }

    @Test
    public void deveRecusarMesmaChaveComValorDiferente() {
        Pagamento pagamento = new Pagamento(comanda, FormaPagamento.PIX, new BigDecimal("40.00"), CHAVE);
        ReflectionTestUtils.setField(pagamento, "id", 1L);
        when(pagamentoRepository.findByChaveIdempotencia(CHAVE)).thenReturn(Optional.of(pagamento));

        assertThrows(ChaveIdempotenciaEmConflitoException.class,
                () -> pixService.gerar(COMANDA_ID, new BigDecimal("41.00"), CHAVE));

        verify(mercadoPagoClient, never()).criarPedidoPix(any(), anyString(), anyString(), anyString());
    }

    @Test
    public void deveReconciliarResultadoIncertoSemCriarSegundoPedido() {
        AtomicReference<Pagamento> reserva = new AtomicReference<>();
        when(pagamentoRepository.findByChaveIdempotencia(CHAVE))
                .thenAnswer(chamada -> Optional.ofNullable(reserva.get()));
        when(pagamentoRepository.saveAndFlush(any(Pagamento.class))).thenAnswer(chamada -> {
            Pagamento pagamento = chamada.getArgument(0);
            ReflectionTestUtils.setField(pagamento, "id", 1L);
            reserva.set(pagamento);
            ultimoPagamentoSalvo = pagamento;
            return pagamento;
        });
        when(mercadoPagoClient.criarPedidoPix(any(), anyString(), anyString(), eq(CHAVE)))
                .thenThrow(new FalhaMercadoPagoException("resultado incerto", null, true));
        when(mercadoPagoClient.buscarPedidoPorReferencia(anyString(), any(), any()))
                .thenReturn(null, pedido("action_required"));

        assertThrows(FalhaMercadoPagoException.class,
                () -> pixService.gerar(COMANDA_ID, new BigDecimal("40.00"), CHAVE));

        CobrancaPixResponse resposta = pixService.gerar(COMANDA_ID, new BigDecimal("40.00"), CHAVE);

        assertEquals(StatusPagamento.PENDENTE, resposta.status());
        verify(mercadoPagoClient, times(1)).criarPedidoPix(any(), anyString(), anyString(), eq(CHAVE));
        verify(mercadoPagoClient, times(2)).buscarPedidoPorReferencia(anyString(), any(), any());
        verify(pagamentoRepository, times(1)).saveAndFlush(any(Pagamento.class));
    }

    @Test
    public void deveLiberarReservaQuandoProvedorRecusaSemCriarPedido() {
        when(mercadoPagoClient.criarPedidoPix(any(), anyString(), anyString(), eq(CHAVE)))
                .thenThrow(new FalhaMercadoPagoException("requisição recusada", null, false));

        FalhaMercadoPagoException erro = assertThrows(
                FalhaMercadoPagoException.class,
                () -> pixService.gerar(COMANDA_ID, new BigDecimal("40.00"), CHAVE));

        assertEquals(false, erro.resultadoIncerto());
        verify(pagamentoRepository).delete(ultimoPagamentoSalvo);
        verify(mercadoPagoClient, never()).buscarPedidoPorReferencia(anyString(), any(), any());
    }

    @Test
    public void deveConfirmarPagamentoQuandoMercadoPagoInformaPago() {
        Pagamento pagamento = pagamentoPendente("40.00");
        cobranca(pagamento, OffsetDateTime.now().plusMinutes(20));
        when(mercadoPagoClient.buscarPedido(PEDIDO_ID)).thenReturn(pedido("processed"));

        CobrancaPixResponse resposta = pixService.consultar(COMANDA_ID, 1L);

        assertEquals(StatusPagamento.CONFIRMADO, resposta.status());
        assertEquals(PEDIDO_ID, pagamento.getReferenciaExterna());
    }

    @Test
    public void deveManterPendenteEnquantoAguardaTransferencia() {
        Pagamento pagamento = pagamentoPendente("40.00");
        cobranca(pagamento, OffsetDateTime.now().plusMinutes(20));
        when(mercadoPagoClient.buscarPedido(PEDIDO_ID)).thenReturn(pedido("action_required"));

        assertEquals(StatusPagamento.PENDENTE, pixService.consultar(COMANDA_ID, 1L).status());
    }

    @Test
    public void deveCancelarPagamentoQuandoPixExpira() {
        Pagamento pagamento = pagamentoPendente("40.00");
        cobranca(pagamento, OffsetDateTime.now().minusMinutes(1));
        when(mercadoPagoClient.buscarPedido(PEDIDO_ID)).thenReturn(pedido("failed"));

        assertEquals(StatusPagamento.CANCELADO, pixService.consultar(COMANDA_ID, 1L).status());
    }

    @Test
    public void naoDeveConsultarMercadoPagoParaPixJaConfirmado() {
        Pagamento pagamento = pagamentoPendente("40.00");
        pagamento.confirmar(PEDIDO_ID);
        cobranca(pagamento, OffsetDateTime.now().plusMinutes(20));

        pixService.consultar(COMANDA_ID, 1L);

        verify(mercadoPagoClient, never()).buscarPedido(anyString());
    }

    @Test
    public void deveCancelarPixAindaNaoPago() {
        Pagamento pagamento = pagamentoPendente("40.00");
        cobranca(pagamento, OffsetDateTime.now().plusMinutes(20));
        when(mercadoPagoClient.cancelarPedido(PEDIDO_ID)).thenReturn(true);

        assertEquals(StatusPagamento.CANCELADO, pixService.cancelar(COMANDA_ID, 1L).status());
    }

    @Test
    public void naoDeveCancelarPixQueAcabouDeSerPago() {
        Pagamento pagamento = pagamentoPendente("40.00");
        cobranca(pagamento, OffsetDateTime.now().plusMinutes(20));
        when(mercadoPagoClient.cancelarPedido(PEDIDO_ID)).thenReturn(false);
        when(mercadoPagoClient.buscarPedido(PEDIDO_ID)).thenReturn(pedido("processed"));

        assertThrows(OperacaoPagamentoInvalidaException.class, () -> pixService.cancelar(COMANDA_ID, 1L));
        assertEquals(StatusPagamento.CONFIRMADO, pagamento.getStatus());
    }

    @Test
    public void deveAtualizarSomenteDepoisQueProvedorConfirmaDevolucao() {
        Pagamento pagamento = pagamentoPendente("40.00");
        pagamento.confirmar(PEDIDO_ID);
        CobrancaPix cobranca = cobranca(pagamento, OffsetDateTime.now().plusMinutes(20));
        when(cobrancaPixRepository.findByPedidoMercadoPago(PEDIDO_ID)).thenReturn(Optional.of(cobranca));
        when(mercadoPagoClient.buscarPedido(PEDIDO_ID)).thenReturn(pedido("refunded"));

        pixService.processarNotificacao(PEDIDO_ID);

        assertEquals(StatusPagamento.ESTORNADO, pagamento.getStatus());
        assertEquals(null, pagamento.getMotivoCorrecao());
        assertEquals(null, pagamento.getCorrigidoPor());
        assertEquals(null, pagamento.getCorrigidoEm());
    }

    @Test
    public void deveIgnorarNotificacaoDePedidoDesconhecido() {
        when(cobrancaPixRepository.findByPedidoMercadoPago("ORD-DESCONHECIDO")).thenReturn(Optional.empty());

        pixService.processarNotificacao("ORD-DESCONHECIDO");

        verify(mercadoPagoClient, never()).buscarPedido(anyString());
    }
}

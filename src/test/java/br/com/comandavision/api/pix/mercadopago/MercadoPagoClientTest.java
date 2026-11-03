package br.com.comandavision.api.pix.mercadopago;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import br.com.comandavision.api.pix.FalhaMercadoPagoException;
import br.com.comandavision.api.pix.PixIndisponivelException;

// Servidor HTTP local no lugar do Mercado Pago: confere o que o cliente envia e se ele
// entende a resposta no formato documentado da API de Orders.
public class MercadoPagoClientTest {
    private static final String CHAVE = "2dad9831-70a9-4dc7-b75b-ec2cd6808bed";
    private static final String RESPOSTA_PEDIDO = """
            {"id":"ORD01HRYFWNYRE1MR1E60MW3X0T2P","type":"online","total_amount":"50.00",
             "status":"action_required","status_detail":"waiting_transfer",
             "transactions":{"payments":[{"id":"PAY01","status":"action_required",
               "payment_method":{"id":"pix","type":"bank_transfer",
                 "ticket_url":"https://www.mercadopago.com.br/sandbox/payments/1/ticket",
                 "qr_code":"00020126580014br.gov.bcb.pix","qr_code_base64":"iVBORw0KGgo="}}]},
             "campo_novo_que_nao_conhecemos":true}
            """;

    private HttpServer servidor;
    private String urlBase;
    private final AtomicReference<String> corpoRecebido = new AtomicReference<>();
    private final AtomicReference<HttpExchange> requisicaoRecebida = new AtomicReference<>();
    private volatile int statusResposta = 201;
    private volatile String respostaAtual = RESPOSTA_PEDIDO;
    private Function<HttpExchange, String> respostaDinamica;

    @BeforeEach
    public void iniciar() throws IOException {
        respostaDinamica = troca -> respostaAtual;
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", troca -> {
            requisicaoRecebida.set(troca);
            corpoRecebido.set(new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] resposta = respostaDinamica.apply(troca).getBytes(StandardCharsets.UTF_8);
            troca.getResponseHeaders().add("Content-Type", "application/json");
            troca.sendResponseHeaders(statusResposta, resposta.length);
            try (OutputStream saida = troca.getResponseBody()) {
                saida.write(resposta);
            }
        });
        servidor.start();
        urlBase = "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    @AfterEach
    public void parar() {
        servidor.stop(0);
    }

    private MercadoPagoClient cliente(boolean modoTeste, String emailPagador) {
        return new MercadoPagoClient("APP_USR-token-de-teste", modoTeste, emailPagador, urlBase);
    }

    @Test
    public void deveCriarPedidoPixNoFormatoDaApiDeOrders() {
        PedidoMercadoPago pedido = cliente(true, "").criarPedidoPix(
                new BigDecimal("50"), "comanda-10-abc", "PT30M", CHAVE);

        HttpExchange requisicao = requisicaoRecebida.get();
        assertEquals("POST", requisicao.getRequestMethod());
        assertEquals("/v1/orders", requisicao.getRequestURI().getPath());
        assertEquals("Bearer APP_USR-token-de-teste", requisicao.getRequestHeaders().getFirst("Authorization"));
        assertEquals(CHAVE, requisicao.getRequestHeaders().getFirst("X-Idempotency-Key"));

        String corpo = corpoRecebido.get();
        assertTrue(corpo.contains("\"total_amount\":\"50.00\""), corpo);
        assertTrue(corpo.contains("\"amount\":\"50.00\""), corpo);
        assertTrue(corpo.contains("\"processing_mode\":\"automatic\""), corpo);
        assertTrue(corpo.contains("\"id\":\"pix\""), corpo);
        assertTrue(corpo.contains("\"type\":\"bank_transfer\""), corpo);
        assertTrue(corpo.contains("\"expiration_time\":\"PT30M\""), corpo);
        assertTrue(corpo.contains("\"external_reference\":\"comanda-10-abc\""), corpo);
        // Modo de teste: pagador APRO faz o Mercado Pago aprovar o Pix sozinho.
        assertTrue(corpo.contains("\"first_name\":\"APRO\""), corpo);

        assertEquals("ORD01HRYFWNYRE1MR1E60MW3X0T2P", pedido.id());
        assertEquals("00020126580014br.gov.bcb.pix", pedido.qrCode());
        assertEquals("iVBORw0KGgo=", pedido.qrCodeBase64());
        assertFalse(pedido.foiPago());
    }

    @Test
    public void deveUsarEmailDoEstabelecimentoEmProducao() {
        cliente(false, "contato@supri.com.br").criarPedidoPix(
                new BigDecimal("12.5"), "ref", "PT30M", CHAVE);

        String corpo = corpoRecebido.get();
        assertTrue(corpo.contains("\"email\":\"contato@supri.com.br\""), corpo);
        assertFalse(corpo.contains("APRO"), corpo);
        assertTrue(corpo.contains("\"total_amount\":\"12.50\""), corpo);
    }

    @Test
    public void deveFicarIndisponivelSemEmailEmProducao() {
        MercadoPagoClient cliente = cliente(false, "");

        assertFalse(cliente.estaConfigurado());
        assertThrows(PixIndisponivelException.class,
                () -> cliente.criarPedidoPix(BigDecimal.TEN, "ref", "PT30M", CHAVE));
    }

    @Test
    public void deveLerPedidoPago() {
        statusResposta = 200;
        respostaAtual = "{\"id\":\"ORD1\",\"status\":\"processed\",\"status_detail\":\"accredited\"}";

        PedidoMercadoPago pedido = cliente(true, "").buscarPedido("ORD1");

        assertEquals("/v1/orders/ORD1", requisicaoRecebida.get().getRequestURI().getPath());
        assertTrue(pedido.foiPago());
    }

    @Test
    public void deveReconciliarPedidoPelaReferenciaExterna() {
        AtomicReference<String> consulta = new AtomicReference<>();
        respostaDinamica = troca -> {
            if ("/v1/orders".equals(troca.getRequestURI().getPath())) {
                consulta.set(troca.getRequestURI().getRawQuery());
                return "{\"data\":[{\"id\":\"ORD1\"}]}";
            }
            return RESPOSTA_PEDIDO;
        };

        PedidoMercadoPago pedido = cliente(true, "").buscarPedidoPorReferencia(
                "cv-10-abc",
                OffsetDateTime.parse("2026-11-02T10:00:00-03:00"),
                OffsetDateTime.parse("2026-11-02T11:00:00-03:00"));

        assertTrue(consulta.get().contains("external_reference=cv-10-abc"), consulta.get());
        assertTrue(consulta.get().contains("begin_date="), consulta.get());
        assertEquals("/v1/orders/ORD1", requisicaoRecebida.get().getRequestURI().getPath());
        assertEquals("ORD01HRYFWNYRE1MR1E60MW3X0T2P", pedido.id());
    }

    @Test
    public void deveInformarQuandoPedidoNaoPodeSerCancelado() {
        statusResposta = 409;
        respostaAtual = "{\"errors\":[{\"code\":\"cannot_cancel_order\"}]}";

        assertFalse(cliente(true, "").cancelarPedido("ORD1"));
        assertEquals("/v1/orders/ORD1/cancel", requisicaoRecebida.get().getRequestURI().getPath());
        assertNotNull(requisicaoRecebida.get().getRequestHeaders().getFirst("X-Idempotency-Key"));
    }

    @Test
    public void deveTraduzirRecusaDoMercadoPago() {
        statusResposta = 400;
        respostaAtual = "{\"errors\":[{\"code\":\"invalid_total_amount\"}]}";

        FalhaMercadoPagoException erro = assertThrows(FalhaMercadoPagoException.class,
                () -> cliente(true, "").criarPedidoPix(BigDecimal.ONE, "ref", "PT30M", CHAVE));
        assertFalse(erro.resultadoIncerto());
    }
}

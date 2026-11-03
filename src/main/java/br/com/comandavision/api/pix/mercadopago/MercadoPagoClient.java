package br.com.comandavision.api.pix.mercadopago;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import br.com.comandavision.api.pix.FalhaMercadoPagoException;
import br.com.comandavision.api.pix.PixIndisponivelException;

// Cliente da API de Orders do Mercado Pago (Pix). O access token fica só no backend.
// Documentação: https://www.mercadopago.com.br/developers/pt/docs/checkout-api-orders/payment-integration/pix
@Component
public class MercadoPagoClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(MercadoPagoClient.class);

    // E-mail e nome usados nas credenciais de teste: o nome APRO faz o Mercado Pago
    // aprovar o Pix sozinho, sem precisar pagar o QR Code.
    private static final String EMAIL_PAGADOR_TESTE = "test@testuser.com";
    private static final String NOME_PAGADOR_TESTE = "APRO";

    private final RestClient restClient;
    private final boolean configurado;
    private final boolean modoTeste;
    private final String emailPagador;

    public MercadoPagoClient(
            @Value("${mercadopago.access-token:}") String accessToken,
            @Value("${mercadopago.modo-teste:false}") boolean modoTeste,
            @Value("${mercadopago.email-pagador:}") String emailPagador,
            @Value("${mercadopago.url-base:https://api.mercadopago.com}") String urlBase) {

        // Em produção o Mercado Pago exige um e-mail de pagador; como a comanda não tem o
        // e-mail do cliente, usamos o do estabelecimento (MERCADOPAGO_EMAIL_PAGADOR).
        boolean temPagador = modoTeste || (emailPagador != null && !emailPagador.isBlank());
        this.configurado = accessToken != null && !accessToken.isBlank() && temPagador;
        this.modoTeste = modoTeste;
        this.emailPagador = emailPagador;
        this.restClient = RestClient.builder()
                .baseUrl(urlBase)
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .build();
    }

    public boolean estaConfigurado() {
        return configurado;
    }

    public PedidoMercadoPago criarPedidoPix(
            BigDecimal valor,
            String referenciaExterna,
            String duracaoExpiracao,
            String chaveIdempotencia) {
        verificarConfiguracao();

        String valorTexto = valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
        Map<String, Object> pagador = modoTeste
                ? Map.of("email", EMAIL_PAGADOR_TESTE, "first_name", NOME_PAGADOR_TESTE)
                : Map.of("email", emailPagador);

        Map<String, Object> corpo = Map.of(
                "type", "online",
                "processing_mode", "automatic",
                "total_amount", valorTexto,
                "external_reference", referenciaExterna,
                "payer", pagador,
                "transactions", Map.of("payments", List.of(Map.of(
                        "amount", valorTexto,
                        "payment_method", Map.of("id", "pix", "type", "bank_transfer"),
                        "expiration_time", duracaoExpiracao))));

        try {
            return restClient.post()
                    .uri("/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Idempotency-Key", chaveIdempotencia)
                    .body(corpo)
                    .retrieve()
                    .body(PedidoMercadoPago.class);
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            if (status >= 400 && status < 500 && status != 409 && status != 423 && status != 429) {
                throw new FalhaMercadoPagoException(
                        "O Mercado Pago recusou a criação do Pix. Revise a configuração antes de tentar novamente",
                        exception,
                        false);
            }
            LOGGER.error("Mercado Pago recusou a criação do Pix: {} {}", exception.getStatusCode(), exception.getResponseBodyAsString());
            throw new FalhaMercadoPagoException("O Mercado Pago não conseguiu gerar o Pix. Tente novamente", exception);
        } catch (RestClientException exception) {
            throw new FalhaMercadoPagoException("Não foi possível falar com o Mercado Pago. Tente novamente", exception);
        }
    }

    public PedidoMercadoPago buscarPedidoPorReferencia(
            String referenciaExterna,
            OffsetDateTime inicio,
            OffsetDateTime fim) {
        verificarConfiguracao();

        try {
            BuscaPedidosMercadoPago resposta = restClient.get()
                    .uri(construtor -> construtor
                            .path("/v1/orders")
                            .queryParam("begin_date", inicio.toString())
                            .queryParam("end_date", fim.toString())
                            .queryParam("external_reference", referenciaExterna)
                            .queryParam("type", "online")
                            .queryParam("page", 1)
                            .queryParam("page_size", 2)
                            .build())
                    .retrieve()
                    .body(BuscaPedidosMercadoPago.class);

            if (resposta == null || resposta.data() == null || resposta.data().isEmpty()) {
                return null;
            }

            // A busca reconcilia o ID; a consulta por ID devolve os dados completos do QR Code.
            return buscarPedido(resposta.data().getFirst().id());
        } catch (FalhaMercadoPagoException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new FalhaMercadoPagoException(
                    "Não foi possível reconciliar a tentativa de Pix no Mercado Pago",
                    exception,
                    true);
        }
    }

    public PedidoMercadoPago buscarPedido(String pedidoId) {
        verificarConfiguracao();

        try {
            return restClient.get()
                    .uri("/v1/orders/{id}", pedidoId)
                    .retrieve()
                    .body(PedidoMercadoPago.class);
        } catch (RestClientException exception) {
            throw new FalhaMercadoPagoException("Não foi possível consultar o Pix no Mercado Pago", exception);
        }
    }

    // Só cancela pedidos ainda não pagos. Se o Pix já foi pago, o Mercado Pago responde 409
    // e quem chamou deve consultar o pedido para registrar o pagamento.
    public boolean cancelarPedido(String pedidoId) {
        verificarConfiguracao();

        try {
            restClient.post()
                    .uri("/v1/orders/{id}/cancel", pedidoId)
                    .header("X-Idempotency-Key", UUID.randomUUID().toString())
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 409) {
                return false;
            }
            throw new FalhaMercadoPagoException("Não foi possível cancelar o Pix no Mercado Pago", exception);
        } catch (RestClientException exception) {
            throw new FalhaMercadoPagoException("Não foi possível falar com o Mercado Pago", exception);
        }
    }

    private void verificarConfiguracao() {
        if (!configurado) {
            // Falta a variável de ambiente MERCADOPAGO_ACCESS_TOKEN.
            throw new PixIndisponivelException();
        }
    }
}

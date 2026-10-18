package br.com.comandavision.api.pix;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.comandavision.api.pix.mercadopago.AssinaturaWebhook;
import io.swagger.v3.oas.annotations.Hidden;

// Recebe os avisos do Mercado Pago (tópico "order"). Rota pública, sem login: a
// autenticidade vem da assinatura x-signature, e o status é sempre conferido no Mercado Pago.
@Hidden
@RestController
public class WebhookMercadoPagoController {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookMercadoPagoController.class);

    private final PixService pixService;
    private final String segredo;

    public WebhookMercadoPagoController(
            PixService pixService,
            @Value("${mercadopago.webhook-secret:}") String segredo) {
        this.pixService = pixService;
        this.segredo = segredo;
    }

    @PostMapping("/webhooks/mercadopago")
    public ResponseEntity<Void> receber(
            @RequestParam(name = "data.id", required = false) String dataIdUrl,
            @RequestParam(name = "type", required = false) String tipoUrl,
            @RequestHeader(name = "x-signature", required = false) String assinatura,
            @RequestHeader(name = "x-request-id", required = false) String idRequisicao,
            @RequestBody(required = false) Map<String, Object> corpo) {

        String pedidoId = dataIdUrl != null ? dataIdUrl : idDoCorpo(corpo);
        String tipo = tipoUrl != null ? tipoUrl : (corpo == null ? null : String.valueOf(corpo.get("type")));

        // Sem segredo configurado a validação é pulada; o status é conferido no Mercado Pago de qualquer forma.
        if (!segredo.isBlank() && !AssinaturaWebhook.ehValida(assinatura, idRequisicao, pedidoId, segredo)) {
            LOGGER.warn("Webhook do Mercado Pago com assinatura inválida (pedido {})", pedidoId);
            return ResponseEntity.status(401).build();
        }

        if (!"order".equals(tipo) || pedidoId == null) {
            return ResponseEntity.ok().build();
        }

        try {
            pixService.processarNotificacao(pedidoId);
        } catch (RuntimeException exception) {
            // Responde erro para o Mercado Pago tentar de novo mais tarde.
            LOGGER.error("Falha ao processar aviso do Mercado Pago para o pedido {}", pedidoId, exception);
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok().build();
    }

    @SuppressWarnings("unchecked")
    private static String idDoCorpo(Map<String, Object> corpo) {
        if (corpo == null || !(corpo.get("data") instanceof Map<?, ?> dados)) {
            return null;
        }
        Object id = ((Map<String, Object>) dados).get("id");
        return id == null ? null : String.valueOf(id);
    }
}

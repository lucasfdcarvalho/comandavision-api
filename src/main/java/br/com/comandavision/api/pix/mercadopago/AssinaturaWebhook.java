package br.com.comandavision.api.pix.mercadopago;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

// Valida o header x-signature dos webhooks do Mercado Pago (HMAC SHA-256).
// Manifesto: "id:<data.id>;request-id:<x-request-id>;ts:<ts>;", com data.id em minúsculas
// e omitindo as partes que não vierem na notificação.
public final class AssinaturaWebhook {

    private AssinaturaWebhook() {
    }

    public static boolean ehValida(String cabecalhoAssinatura, String idRequisicao, String dataId, String segredo) {
        if (cabecalhoAssinatura == null || segredo == null || segredo.isBlank()) {
            return false;
        }

        String ts = null;
        String hashRecebido = null;
        for (String parte : cabecalhoAssinatura.split(",")) {
            String[] chaveValor = parte.trim().split("=", 2);
            if (chaveValor.length != 2) {
                continue;
            }
            if ("ts".equals(chaveValor[0])) {
                ts = chaveValor[1];
            } else if ("v1".equals(chaveValor[0])) {
                hashRecebido = chaveValor[1];
            }
        }

        if (ts == null || hashRecebido == null) {
            return false;
        }

        String manifesto = montarManifesto(dataId, idRequisicao, ts);
        String hashCalculado = calcularHmac(manifesto, segredo);

        // Comparação em tempo constante para não vazar informação pelo tempo de resposta.
        return MessageDigest.isEqual(
                hashCalculado.getBytes(StandardCharsets.UTF_8),
                hashRecebido.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }

    static String montarManifesto(String dataId, String idRequisicao, String ts) {
        StringBuilder manifesto = new StringBuilder();
        if (dataId != null && !dataId.isBlank()) {
            manifesto.append("id:").append(dataId.toLowerCase(Locale.ROOT)).append(';');
        }
        if (idRequisicao != null && !idRequisicao.isBlank()) {
            manifesto.append("request-id:").append(idRequisicao).append(';');
        }
        manifesto.append("ts:").append(ts).append(';');
        return manifesto.toString();
    }

    static String calcularHmac(String conteudo, String segredo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(conteudo.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível calcular a assinatura do webhook", exception);
        }
    }
}

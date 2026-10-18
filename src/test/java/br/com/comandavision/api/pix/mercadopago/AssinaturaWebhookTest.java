package br.com.comandavision.api.pix.mercadopago;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class AssinaturaWebhookTest {
    private static final String SEGREDO = "segredo-de-teste";
    private static final String ID_REQUISICAO = "bb56a2f1-6aae-46ac-982e-9dcd3581d08e";
    // Calculado fora do Java (Python hmac/hashlib) para o manifesto abaixo.
    private static final String HASH_ESPERADO = "ecdde4afc88312478e7eda3f577698e9fdbe9c05f348be23357f04613dac89ac";

    @Test
    public void deveMontarManifestoComIdEmMinusculas() {
        assertEquals(
                "id:ord01hryfwnyre1mr1e60mw3x0t2p;request-id:" + ID_REQUISICAO + ";ts:1704908010;",
                AssinaturaWebhook.montarManifesto("ORD01HRYFWNYRE1MR1E60MW3X0T2P", ID_REQUISICAO, "1704908010"));
    }

    @Test
    public void deveAceitarAssinaturaValida() {
        assertTrue(AssinaturaWebhook.ehValida(
                "ts=1704908010,v1=" + HASH_ESPERADO, ID_REQUISICAO, "ORD01HRYFWNYRE1MR1E60MW3X0T2P", SEGREDO));
    }

    @Test
    public void deveRecusarAssinaturaDeOutroSegredo() {
        assertFalse(AssinaturaWebhook.ehValida(
                "ts=1704908010,v1=" + HASH_ESPERADO, ID_REQUISICAO, "ORD01HRYFWNYRE1MR1E60MW3X0T2P", "outro-segredo"));
    }

    @Test
    public void deveRecusarAssinaturaDeOutroPedido() {
        assertFalse(AssinaturaWebhook.ehValida(
                "ts=1704908010,v1=" + HASH_ESPERADO, ID_REQUISICAO, "ORD99999999999999999999999", SEGREDO));
    }

    @Test
    public void deveRecusarCabecalhoAusenteOuMalformado() {
        assertFalse(AssinaturaWebhook.ehValida(null, ID_REQUISICAO, "ORD1", SEGREDO));
        assertFalse(AssinaturaWebhook.ehValida("v1=" + HASH_ESPERADO, ID_REQUISICAO, "ORD1", SEGREDO));
        assertFalse(AssinaturaWebhook.ehValida("ts=1704908010", ID_REQUISICAO, "ORD1", SEGREDO));
    }
}

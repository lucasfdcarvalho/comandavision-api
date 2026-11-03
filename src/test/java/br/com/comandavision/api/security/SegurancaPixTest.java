package br.com.comandavision.api.security;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import br.com.comandavision.api.pix.PixController;
import br.com.comandavision.api.pix.PixService;
import br.com.comandavision.api.pix.WebhookMercadoPagoController;

@WebMvcTest({ PixController.class, WebhookMercadoPagoController.class })
@Import({
        SecurityConfig.class,
        AutenticacaoNaoRealizadaHandler.class,
        AcessoNegadoHandler.class
})
@TestPropertySource(properties = "mercadopago.webhook-secret=segredo-de-teste")
public class SegurancaPixTest {
    // Mesmo vetor do AssinaturaWebhookTest (calculado fora do Java).
    private static final String ASSINATURA = "ts=1704908010,v1=ecdde4afc88312478e7eda3f577698e9fdbe9c05f348be23357f04613dac89ac";
    private static final String ID_REQUISICAO = "bb56a2f1-6aae-46ac-982e-9dcd3581d08e";
    private static final String PEDIDO = "ORD01HRYFWNYRE1MR1E60MW3X0T2P";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PixService pixService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    public void deveExigirLoginParaGerarPix() throws Exception {
        mockMvc.perform(post("/api/comandas/10/pagamentos/pix")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valor\":10.00}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(pixService);
    }

    @Test
    public void deveAceitarPixLegadoSemCabecalhoDeIdempotencia() throws Exception {
        UUID usuarioId = UUID.fromString("b35bbcb0-1128-45a2-bd16-cb8d99be154f");
        mockMvc.perform(post("/api/comandas/10/pagamentos/pix")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valor\":10.00}")
                .with(jwt()
                        .jwt(token -> token.subject(usuarioId.toString()).claim("user_role", "FUNCIONARIO"))
                        .authorities(new SimpleGrantedAuthority("ROLE_FUNCIONARIO"))))
                .andExpect(status().isCreated());

        verify(pixService).gerar(10L, new BigDecimal("10.00"), null, usuarioId);
    }

    @Test
    public void deveAceitarWebhookComAssinaturaValidaSemLogin() throws Exception {
        mockMvc.perform(post("/webhooks/mercadopago")
                .param("data.id", PEDIDO)
                .param("type", "order")
                .header("x-signature", ASSINATURA)
                .header("x-request-id", ID_REQUISICAO)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"order\",\"action\":\"order.processed\",\"data\":{\"id\":\"" + PEDIDO + "\"}}"))
                .andExpect(status().isOk());

        verify(pixService).processarNotificacao(PEDIDO);
    }

    @Test
    public void deveRecusarWebhookComAssinaturaInvalida() throws Exception {
        mockMvc.perform(post("/webhooks/mercadopago")
                .param("data.id", PEDIDO)
                .param("type", "order")
                .header("x-signature", "ts=1704908010,v1=0000")
                .header("x-request-id", ID_REQUISICAO)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(pixService);
    }
}

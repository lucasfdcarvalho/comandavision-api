package br.com.comandavision.api.security;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.comandavision.api.pagamento.PagamentoController;
import br.com.comandavision.api.pagamento.PagamentoService;
import br.com.comandavision.api.pagamento.dto.CorrigirPagamentoRequest;

import java.util.UUID;

@WebMvcTest(PagamentoController.class)
@Import({
        SecurityConfig.class,
        AutenticacaoNaoRealizadaHandler.class,
        AcessoNegadoHandler.class
})
class SegurancaPagamentoTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PagamentoService pagamentoService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void devePermitirCorrecaoParaFuncionario() throws Exception {
        UUID usuarioId = UUID.fromString("b35bbcb0-1128-45a2-bd16-cb8d99be154f");
        mockMvc.perform(patch("/api/comandas/10/pagamentos/20/corrigir")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"Registro duplicado\"}")
                .with(jwt()
                        .jwt(token -> token.subject(usuarioId.toString()).claim("user_role", "FUNCIONARIO"))
                        .authorities(new SimpleGrantedAuthority("ROLE_FUNCIONARIO"))))
                .andExpect(status().isOk());

        verify(pagamentoService).corrigirRegistro(
                10L, 20L, new CorrigirPagamentoRequest("Registro duplicado"), usuarioId);
    }

    @Test
    void devePermitirCorrecaoParaDono() throws Exception {
        UUID usuarioId = UUID.fromString("35f72406-1291-45cb-a637-8404a687c931");
        mockMvc.perform(patch("/api/comandas/10/pagamentos/20/corrigir")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"Forma informada errada\"}")
                .with(jwt()
                        .jwt(token -> token.subject(usuarioId.toString()).claim("user_role", "DONO"))
                        .authorities(new SimpleGrantedAuthority("ROLE_DONO"))))
                .andExpect(status().isOk());

        verify(pagamentoService).corrigirRegistro(
                10L, 20L, new CorrigirPagamentoRequest("Forma informada errada"), usuarioId);
    }

    @Test
    void deveNegarCorrecaoSemAutenticacao() throws Exception {
        mockMvc.perform(patch("/api/comandas/10/pagamentos/20/corrigir")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"Registro duplicado\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(pagamentoService);
    }

    @Test
    void deveExigirMotivoValido() throws Exception {
        mockMvc.perform(patch("/api/comandas/10/pagamentos/20/corrigir")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"abc\"}")
                .with(jwt()
                        .jwt(token -> token.subject("35f72406-1291-45cb-a637-8404a687c931")
                                .claim("user_role", "FUNCIONARIO"))
                        .authorities(new SimpleGrantedAuthority("ROLE_FUNCIONARIO"))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(pagamentoService);
    }
}

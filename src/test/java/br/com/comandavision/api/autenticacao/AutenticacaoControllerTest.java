package br.com.comandavision.api.autenticacao;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.comandavision.api.security.AcessoNegadoHandler;
import br.com.comandavision.api.security.AutenticacaoNaoRealizadaHandler;
import br.com.comandavision.api.security.SecurityConfig;

@WebMvcTest(AutenticacaoController.class)
@Import({
        SecurityConfig.class,
        AutenticacaoNaoRealizadaHandler.class,
        AcessoNegadoHandler.class
})
public class AutenticacaoControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    public void deveRetornarUsuarioAutenticado() throws Exception {
        mockMvc.perform(
                get("/api/auth/me")
                        .with(jwt()
                                .jwt(token -> token
                                        .subject("550e8400-e29b-41d4-a716-446655440000")
                                        .claim("email", "dono@comandavision.com.br")
                                        .claim("user_role", "DONO"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_DONO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(
                        "550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(jsonPath("$.email").value(
                        "dono@comandavision.com.br"))
                .andExpect(jsonPath("$.papel").value("DONO"));
    }

    @Test
    public void deveNegarAcessoSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.erro").value("Unauthorized"))
                .andExpect(jsonPath("$.mensagem").value("É necessário estar autenticado para acessar este recurso"));
    }

    @Test
    public void devePermitirAcessoParaFuncionario() throws Exception {
        mockMvc.perform(
                get("/api/auth/me")
                        .with(jwt()
                                .jwt(token -> token
                                        .subject("660e8400-e29b-41d4-a716-446655440001")
                                        .claim("email", "funcionario@comandavision.com.br")
                                        .claim("user_role", "FUNCIONARIO"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_FUNCIONARIO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(
                        "660e8400-e29b-41d4-a716-446655440001"))
                .andExpect(jsonPath("$.email").value(
                        "funcionario@comandavision.com.br"))
                .andExpect(jsonPath("$.papel").value(
                        "FUNCIONARIO"));
    }

}

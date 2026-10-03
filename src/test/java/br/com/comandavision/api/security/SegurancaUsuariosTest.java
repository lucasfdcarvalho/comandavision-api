package br.com.comandavision.api.security;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.comandavision.api.usuario.UsuarioController;
import br.com.comandavision.api.usuario.UsuarioService;

@WebMvcTest(UsuarioController.class)
@Import({
        SecurityConfig.class,
        AutenticacaoNaoRealizadaHandler.class,
        AcessoNegadoHandler.class
})
public class SegurancaUsuariosTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    public void deveNegarGestaoDeUsuariosParaFuncionario() throws Exception {
        mockMvc.perform(get("/api/usuarios")
                .with(jwt()
                        .jwt(token -> token
                                .subject("2f1c7b1e-4d1a-4a8f-9b7e-1c2d3e4f5a6b")
                                .claim("user_role", "FUNCIONARIO"))
                        .authorities(new SimpleGrantedAuthority("ROLE_FUNCIONARIO"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verifyNoInteractions(usuarioService);
    }

    @Test
    public void devePermitirListarUsuariosParaDono() throws Exception {
        when(usuarioService.listar()).thenReturn(List.of());

        mockMvc.perform(get("/api/usuarios")
                .with(jwt()
                        .jwt(token -> token
                                .subject("2f1c7b1e-4d1a-4a8f-9b7e-1c2d3e4f5a6b")
                                .claim("user_role", "DONO"))
                        .authorities(new SimpleGrantedAuthority("ROLE_DONO"))))
                .andExpect(status().isOk());
    }

    @Test
    public void deveValidarDadosDoNovoUsuario() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"\",\"email\":\"invalido\",\"senha\":\"123\",\"papel\":\"FUNCIONARIO\"}")
                .with(jwt()
                        .jwt(token -> token
                                .subject("2f1c7b1e-4d1a-4a8f-9b7e-1c2d3e4f5a6b")
                                .claim("user_role", "DONO"))
                        .authorities(new SimpleGrantedAuthority("ROLE_DONO"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nome").exists())
                .andExpect(jsonPath("$.campos.email").exists())
                .andExpect(jsonPath("$.campos.senha").exists());

        verifyNoInteractions(usuarioService);
    }
}

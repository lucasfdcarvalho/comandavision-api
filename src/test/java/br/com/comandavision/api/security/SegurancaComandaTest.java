package br.com.comandavision.api.security;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.comandavision.api.comanda.ComandaController;
import br.com.comandavision.api.comanda.ComandaService;

@WebMvcTest(ComandaController.class)
@Import({
        SecurityConfig.class,
        AutenticacaoNaoRealizadaHandler.class,
        AcessoNegadoHandler.class
})
class SegurancaComandaTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComandaService comandaService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void devePermitirListarAReceberParaFuncionario() throws Exception {
        mockMvc.perform(get("/api/comandas/a-receber")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_FUNCIONARIO"))))
                .andExpect(status().isOk());

        verify(comandaService).listarAReceber();
    }

    @Test
    void devePermitirListarAReceberParaDono() throws Exception {
        mockMvc.perform(get("/api/comandas/a-receber")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DONO"))))
                .andExpect(status().isOk());

        verify(comandaService).listarAReceber();
    }

    @Test
    void deveNegarListagemAReceberSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/comandas/a-receber"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(comandaService);
    }
}

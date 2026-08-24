package br.com.comandavision.api.security;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.comandavision.api.dashboard.controller.DashboardController;
import br.com.comandavision.api.dashboard.dto.ResumoDashboardResponse;
import br.com.comandavision.api.dashboard.service.DashboardService;

@WebMvcTest(DashboardController.class)
@Import({
        SecurityConfig.class,
        AutenticacaoNaoRealizadaHandler.class,
        AcessoNegadoHandler.class
})
public class SegurancaDashboardTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    public void deveNegarAcessoAoDashboardSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/dashboard/resumo")
                .param("inicio", "2026-08-01")
                .param("fim", "2026-08-31"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value("É necessário estar autenticado para acessar este recurso"));
    }

    @Test
    public void deveNegarAcessoAoDashboardParaFuncionario() throws Exception {
        mockMvc.perform(get("/api/dashboard/resumo")
                .param("inicio", "2026-08-01")
                .param("fim", "2026-08-31")
                .with(jwt()
                        .jwt(token -> token
                                .subject("usuario-funcionario")
                                .claim("user_role", "FUNCIONARIO"))
                        .authorities(new SimpleGrantedAuthority("ROLE_FUNCIONARIO"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensagem").value("Você não possui permissão para acessar este recurso"));

        verifyNoInteractions(dashboardService);
    }

    @Test
    public void devePermitirAcessoAoDashboardParaDono() throws Exception {
        LocalDate inicio = LocalDate.of(2026, 8, 1);
        LocalDate fim = LocalDate.of(2026, 8, 31);

        ResumoDashboardResponse resposta = new ResumoDashboardResponse(
                new BigDecimal("5000.00"),
                1L,
                new BigDecimal("5000.00"),
                50L);

        when(dashboardService.buscarResumo(inicio, fim))
                .thenReturn(resposta);

        mockMvc.perform(
                get("/api/dashboard/resumo")
                        .param("inicio", "2026-08-01")
                        .param("fim", "2026-08-31")
                        .with(jwt()
                                .jwt(token -> token
                                        .subject("usuario-dono")
                                        .claim("user_role", "DONO"))
                                .authorities(new SimpleGrantedAuthority("ROLE_DONO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faturamento").value(5000.00))
                .andExpect(jsonPath("$.quantidadeVendas").value(1))
                .andExpect(jsonPath("$.ticketMedio").value(5000.00))
                .andExpect(jsonPath("$.quantidadeItensVendidos").value(50));

        verify(dashboardService).buscarResumo(inicio, fim);
    }
}

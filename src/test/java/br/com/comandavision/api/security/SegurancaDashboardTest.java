package br.com.comandavision.api.security;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.comandavision.api.dashboard.controller.DashboardController;
import br.com.comandavision.api.dashboard.dto.ComandaEncerradaResponse;
import br.com.comandavision.api.dashboard.dto.ResumoDashboardResponse;
import br.com.comandavision.api.dashboard.service.DashboardService;
import br.com.comandavision.api.comanda.StatusComanda;

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
    public void deveResponderNaoAutenticadoParaTokenExpiradoOuInvalido() throws Exception {
        when(jwtDecoder.decode(anyString()))
                .thenThrow(new BadJwtException("Jwt expired"));

        mockMvc.perform(get("/api/dashboard/resumo")
                .param("inicio", "2026-08-01")
                .param("fim", "2026-08-31")
                .header("Authorization", "Bearer token-expirado"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value("É necessário estar autenticado para acessar este recurso"));

        verifyNoInteractions(dashboardService);
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

    @Test
    public void devePermitirApenasAoDonoConsultarHistoricoGerencial() throws Exception {
        LocalDate inicio = LocalDate.of(2026, 8, 1);
        LocalDate fim = LocalDate.of(2026, 8, 31);
        ComandaEncerradaResponse resposta = new ComandaEncerradaResponse(
                42L,
                "Mesa 12",
                StatusComanda.FECHADA,
                OffsetDateTime.parse("2026-08-18T21:30:00-03:00"),
                new BigDecimal("128.50"),
                new BigDecimal("128.50"),
                true);
        when(dashboardService.buscarComandasEncerradas(inicio, fim)).thenReturn(List.of(resposta));

        mockMvc.perform(get("/api/dashboard/comandas")
                .param("inicio", "2026-08-01")
                .param("fim", "2026-08-31")
                .with(jwt()
                        .jwt(token -> token.subject("usuario-funcionario").claim("user_role", "FUNCIONARIO"))
                        .authorities(new SimpleGrantedAuthority("ROLE_FUNCIONARIO"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/dashboard/comandas")
                .param("inicio", "2026-08-01")
                .param("fim", "2026-08-31")
                .with(jwt()
                        .jwt(token -> token.subject("usuario-dono").claim("user_role", "DONO"))
                        .authorities(new SimpleGrantedAuthority("ROLE_DONO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(42))
                .andExpect(jsonPath("$[0].status").value("FECHADA"))
                .andExpect(jsonPath("$[0].total").value(128.50))
                .andExpect(jsonPath("$[0].computadaNosIndicadores").value(true));

        verify(dashboardService).buscarComandasEncerradas(inicio, fim);
    }

    @Test
    public void deveResponderRequisicaoInvalidaParaDataMalformada() throws Exception {
        mockMvc.perform(get("/api/dashboard/resumo")
                .param("inicio", "NaN-NaN-NaN")
                .param("fim", "2026-08-31")
                .with(jwt()
                        .jwt(token -> token
                                .subject("usuario-dono")
                                .claim("user_role", "DONO"))
                        .authorities(new SimpleGrantedAuthority("ROLE_DONO"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").value("O parâmetro 'inicio' está em um formato inválido"));

        verifyNoInteractions(dashboardService);
    }

    @Test
    public void deveResponderRequisicaoInvalidaSemData() throws Exception {
        mockMvc.perform(get("/api/dashboard/resumo")
                .param("fim", "2026-08-31")
                .with(jwt()
                        .jwt(token -> token
                                .subject("usuario-dono")
                                .claim("user_role", "DONO"))
                        .authorities(new SimpleGrantedAuthority("ROLE_DONO"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O parâmetro 'inicio' é obrigatório"));
    }
}

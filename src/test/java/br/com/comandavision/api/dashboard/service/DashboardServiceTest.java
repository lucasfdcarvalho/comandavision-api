package br.com.comandavision.api.dashboard.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.comandavision.api.dashboard.LimiteDashboardInvalidoException;
import br.com.comandavision.api.dashboard.PeriodoInvalidoException;
import br.com.comandavision.api.dashboard.dto.FaturamentoDiarioResponse;
import br.com.comandavision.api.dashboard.dto.FormaPagamentoResumoResponse;
import br.com.comandavision.api.dashboard.dto.ProdutoMaisVendidoResponse;
import br.com.comandavision.api.dashboard.dto.ResumoDashboardResponse;
import br.com.comandavision.api.dashboard.projection.FaturamentoDiarioProjection;
import br.com.comandavision.api.dashboard.projection.FormaPagamentoResumoProjection;
import br.com.comandavision.api.dashboard.projection.ProdutoMaisVendidoProjection;
import br.com.comandavision.api.dashboard.projection.ResumoDashboardProjection;
import br.com.comandavision.api.dashboard.repository.DashboardRepository;

@ExtendWith(MockitoExtension.class)
public class DashboardServiceTest {
    @Mock
    private DashboardRepository dashboardRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    public void deveLancarExcecaoQuandoPeriodoForInvalido() {
        LocalDate inicio = LocalDate.of(2026, 8, 31);
        LocalDate fim = LocalDate.of(2026, 8, 1);

        assertThrows(
                PeriodoInvalidoException.class,
                () -> dashboardService.buscarResumo(inicio, fim));

        verifyNoInteractions(dashboardRepository);
    }

    @Test
    public void deveBuscarResumoDoDashboard() {
        LocalDate inicio = LocalDate.of(2026, 8, 1);
        LocalDate fim = LocalDate.of(2026, 8, 31);

        OffsetDateTime inicioPeriodo = OffsetDateTime.parse(
                "2026-08-01T00:00:00-03:00");

        OffsetDateTime fimExclusivo = OffsetDateTime.parse(
                "2026-09-01T00:00:00-03:00");

        ResumoDashboardProjection resultado = mock(ResumoDashboardProjection.class);

        when(resultado.getFaturamento())
                .thenReturn(new BigDecimal("5000.00"));
        when(resultado.getQuantidadeVendas())
                .thenReturn(120L);
        when(resultado.getTicketMedio())
                .thenReturn(new BigDecimal("41.67"));
        when(resultado.getQuantidadeItensVendidos())
                .thenReturn(280L);

        when(dashboardRepository.buscarResumo(
                inicioPeriodo,
                fimExclusivo))
                .thenReturn(resultado);

        ResumoDashboardResponse resposta = dashboardService.buscarResumo(inicio, fim);

        assertEquals(
                new BigDecimal("5000.00"),
                resposta.faturamento());
        assertEquals(120L, resposta.quantidadeVendas());
        assertEquals(
                new BigDecimal("41.67"),
                resposta.ticketMedio());
        assertEquals(280L, resposta.quantidadeItensVendidos());

        verify(dashboardRepository).buscarResumo(
                inicioPeriodo,
                fimExclusivo);
    }

    @Test
    public void deveBuscarProdutosMaisVendidos() {
        LocalDate inicio = LocalDate.of(2026, 8, 1);
        LocalDate fim = LocalDate.of(2026, 8, 31);

        OffsetDateTime inicioPeriodo = OffsetDateTime.parse(
                "2026-08-01T00:00:00-03:00");

        OffsetDateTime fimExclusivo = OffsetDateTime.parse(
                "2026-09-01T00:00:00-03:00");

        ProdutoMaisVendidoProjection resultado = mock(ProdutoMaisVendidoProjection.class);

        when(resultado.getProdutoId()).thenReturn(1L);
        when(resultado.getProdutoNome())
                .thenReturn("Coca-Cola 350 ml");
        when(resultado.getQuantidadeVendida())
                .thenReturn(35L);
        when(resultado.getFaturamento())
                .thenReturn(new BigDecimal("227.50"));

        when(dashboardRepository.buscarProdutosMaisVendidos(
                inicioPeriodo,
                fimExclusivo,
                5))
                .thenReturn(List.of(resultado));

        List<ProdutoMaisVendidoResponse> resposta = dashboardService.buscarProdutosMaisVendidos(inicio, fim, 5);

        assertEquals(1, resposta.size());
        assertEquals(1L, resposta.get(0).produtoId());
        assertEquals(
                "Coca-Cola 350 ml",
                resposta.get(0).produtoNome());
        assertEquals(35L, resposta.get(0).quantidadeVendida());
        assertEquals(
                new BigDecimal("227.50"),
                resposta.get(0).faturamento());

        verify(dashboardRepository)
                .buscarProdutosMaisVendidos(
                        inicioPeriodo,
                        fimExclusivo,
                        5);
    }

    @Test
    public void deveLancarExcecaoQuandoLimiteForMenorQueUm() {
        LocalDate inicio = LocalDate.of(2026, 8, 1);
        LocalDate fim = LocalDate.of(2026, 8, 31);

        assertThrows(
                LimiteDashboardInvalidoException.class,
                () -> dashboardService.buscarProdutosMaisVendidos(
                        inicio,
                        fim,
                        0));

        verifyNoInteractions(dashboardRepository);
    }

    @Test
    public void deveLancarExcecaoQuandoLimiteForMaiorQueVinte() {
        LocalDate inicio = LocalDate.of(2026, 8, 1);
        LocalDate fim = LocalDate.of(2026, 8, 31);

        assertThrows(
                LimiteDashboardInvalidoException.class,
                () -> dashboardService.buscarProdutosMaisVendidos(
                        inicio,
                        fim,
                        21));

        verifyNoInteractions(dashboardRepository);
    }

    @Test
    public void deveCalcularPercentualPorFormaPagamento() {
        LocalDate inicio = LocalDate.of(2026, 8, 1);
        LocalDate fim = LocalDate.of(2026, 8, 31);

        OffsetDateTime inicioPeriodo = OffsetDateTime.parse(
                "2026-08-01T00:00:00-03:00");

        OffsetDateTime fimExclusivo = OffsetDateTime.parse(
                "2026-09-01T00:00:00-03:00");

        FormaPagamentoResumoProjection pix = mock(FormaPagamentoResumoProjection.class);

        when(pix.getForma()).thenReturn("PIX");
        when(pix.getQuantidadePagamentos()).thenReturn(20L);
        when(pix.getValorRecebido())
                .thenReturn(new BigDecimal("100.00"));

        FormaPagamentoResumoProjection dinheiro = mock(FormaPagamentoResumoProjection.class);

        when(dinheiro.getForma()).thenReturn("DINHEIRO");
        when(dinheiro.getQuantidadePagamentos()).thenReturn(10L);
        when(dinheiro.getValorRecebido())
                .thenReturn(new BigDecimal("50.00"));

        when(dashboardRepository.buscarResumoPorFormaPagamento(
                inicioPeriodo,
                fimExclusivo))
                .thenReturn(List.of(pix, dinheiro));

        List<FormaPagamentoResumoResponse> resposta = dashboardService.buscarResumoPorFormaPagamento(
                inicio,
                fim);

        assertEquals(2, resposta.size());

        assertEquals("PIX", resposta.get(0).forma().name());
        assertEquals(20L, resposta.get(0).quantidadePagamentos());
        assertEquals(
                new BigDecimal("100.00"),
                resposta.get(0).valorRecebido());
        assertEquals(
                new BigDecimal("66.67"),
                resposta.get(0).percentual());

        assertEquals("DINHEIRO", resposta.get(1).forma().name());
        assertEquals(
                new BigDecimal("33.33"),
                resposta.get(1).percentual());

        verify(dashboardRepository)
                .buscarResumoPorFormaPagamento(
                        inicioPeriodo,
                        fimExclusivo);
    }

    @Test
    public void deveRetornarPercentualZeroQuandoNaoHouverValorRecebido() {
        LocalDate inicio = LocalDate.of(2026, 8, 1);
        LocalDate fim = LocalDate.of(2026, 8, 31);

        OffsetDateTime inicioPeriodo = OffsetDateTime.parse(
                "2026-08-01T00:00:00-03:00");

        OffsetDateTime fimExclusivo = OffsetDateTime.parse(
                "2026-09-01T00:00:00-03:00");

        FormaPagamentoResumoProjection pix = mock(FormaPagamentoResumoProjection.class);

        when(pix.getForma()).thenReturn("PIX");
        when(pix.getQuantidadePagamentos()).thenReturn(0L);
        when(pix.getValorRecebido())
                .thenReturn(new BigDecimal("0.00"));

        when(dashboardRepository.buscarResumoPorFormaPagamento(
                inicioPeriodo,
                fimExclusivo))
                .thenReturn(List.of(pix));

        List<FormaPagamentoResumoResponse> resposta = dashboardService.buscarResumoPorFormaPagamento(
                inicio,
                fim);

        assertEquals(1, resposta.size());
        assertEquals(
                new BigDecimal("0.00"),
                resposta.get(0).percentual());

        verify(dashboardRepository)
                .buscarResumoPorFormaPagamento(
                        inicioPeriodo,
                        fimExclusivo);
    }

    @Test
    public void deveBuscarFaturamentoDiario() {
        LocalDate inicio = LocalDate.of(2026, 8, 1);
        LocalDate fim = LocalDate.of(2026, 8, 2);

        OffsetDateTime inicioPeriodo = OffsetDateTime.parse(
                "2026-08-01T00:00:00-03:00");

        OffsetDateTime fimExclusivo = OffsetDateTime.parse(
                "2026-08-03T00:00:00-03:00");

        FaturamentoDiarioProjection primeiroDia = mock(FaturamentoDiarioProjection.class);

        when(primeiroDia.getData())
                .thenReturn(LocalDate.of(2026, 8, 1));
        when(primeiroDia.getFaturamento())
                .thenReturn(new BigDecimal("1250.50"));
        when(primeiroDia.getQuantidadeVendas())
                .thenReturn(18L);

        FaturamentoDiarioProjection segundoDia = mock(FaturamentoDiarioProjection.class);

        when(segundoDia.getData())
                .thenReturn(LocalDate.of(2026, 8, 2));
        when(segundoDia.getFaturamento())
                .thenReturn(new BigDecimal("0.00"));
        when(segundoDia.getQuantidadeVendas())
                .thenReturn(0L);

        when(dashboardRepository.buscarFaturamentoDiario(
                inicioPeriodo,
                fimExclusivo))
                .thenReturn(List.of(primeiroDia, segundoDia));

        List<FaturamentoDiarioResponse> resposta = dashboardService.buscarFaturamentoDiario(
                inicio,
                fim);

        assertEquals(2, resposta.size());

        assertEquals(
                LocalDate.of(2026, 8, 1),
                resposta.get(0).data());
        assertEquals(
                new BigDecimal("1250.50"),
                resposta.get(0).faturamento());
        assertEquals(18L, resposta.get(0).quantidadeVendas());

        assertEquals(
                LocalDate.of(2026, 8, 2),
                resposta.get(1).data());
        assertEquals(
                new BigDecimal("0.00"),
                resposta.get(1).faturamento());
        assertEquals(0L, resposta.get(1).quantidadeVendas());

        verify(dashboardRepository).buscarFaturamentoDiario(
                inicioPeriodo,
                fimExclusivo);
    }

}

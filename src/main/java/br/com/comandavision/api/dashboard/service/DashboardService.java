package br.com.comandavision.api.dashboard.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.comandavision.api.dashboard.LimiteDashboardInvalidoException;
import br.com.comandavision.api.dashboard.PeriodoInvalidoException;
import br.com.comandavision.api.dashboard.dto.FaturamentoDiarioResponse;
import br.com.comandavision.api.dashboard.dto.FormaPagamentoResumoResponse;
import br.com.comandavision.api.dashboard.dto.ProdutoMaisVendidoResponse;
import br.com.comandavision.api.dashboard.dto.ResumoDashboardResponse;
import br.com.comandavision.api.dashboard.projection.FaturamentoDiarioProjection;
import br.com.comandavision.api.dashboard.projection.FormaPagamentoResumoProjection;
import br.com.comandavision.api.dashboard.projection.ResumoDashboardProjection;
import br.com.comandavision.api.dashboard.repository.DashboardRepository;
import br.com.comandavision.api.pagamento.FormaPagamento;

@Service
public class DashboardService {
    private static final ZoneId FUSO_HORARIO = ZoneId.of("America/Sao_Paulo");

    private final DashboardRepository dashboardRepository;

    public DashboardService(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    @Transactional(readOnly = true)
    public ResumoDashboardResponse buscarResumo(LocalDate inicio, LocalDate fim) {
        this.validarPeriodo(inicio, fim);

        OffsetDateTime inicioPeriodo = this.converterInicio(inicio);
        OffsetDateTime fimExclusivo = this.converterFimExclusivo(fim);

        ResumoDashboardProjection resultado = dashboardRepository.buscarResumo(inicioPeriodo, fimExclusivo);

        return new ResumoDashboardResponse(
                resultado.getFaturamento(),
                resultado.getQuantidadeVendas(),
                resultado.getTicketMedio(),
                resultado.getQuantidadeItensVendidos());
    }

    @Transactional(readOnly = true)
    public List<ProdutoMaisVendidoResponse> buscarProdutosMaisVendidos(
            LocalDate inicio,
            LocalDate fim,
            int limite) {

        this.validarPeriodo(inicio, fim);

        if (limite < 1 || limite > 20) {
            throw new LimiteDashboardInvalidoException(limite);
        }

        OffsetDateTime inicioPeriodo = this.converterInicio(inicio);
        OffsetDateTime fimExclusivo = this.converterFimExclusivo(fim);

        return dashboardRepository.buscarProdutosMaisVendidos(inicioPeriodo, fimExclusivo, limite)
                .stream()
                .map(resultado -> new ProdutoMaisVendidoResponse(
                        resultado.getProdutoId(),
                        resultado.getProdutoNome(),
                        resultado.getQuantidadeVendida(),
                        resultado.getFaturamento()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FormaPagamentoResumoResponse> buscarResumoPorFormaPagamento(LocalDate inicio, LocalDate fim) {

        this.validarPeriodo(inicio, fim);

        OffsetDateTime inicioPeriodo = this.converterInicio(inicio);
        OffsetDateTime fimExclusivo = this.converterFimExclusivo(fim);

        List<FormaPagamentoResumoProjection> resultados = dashboardRepository
                .buscarResumoPorFormaPagamento(inicioPeriodo, fimExclusivo);

        BigDecimal valorTotal = resultados.stream()
                .map(FormaPagamentoResumoProjection::getValorRecebido)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return resultados.stream()
                .map(resultado -> {
                    BigDecimal percentual;

                    if (valorTotal.compareTo(BigDecimal.ZERO) == 0) {
                        percentual = BigDecimal.ZERO.setScale(2);
                    } else {
                        percentual = resultado.getValorRecebido()
                                .multiply(BigDecimal.valueOf(100))
                                .divide(valorTotal, 2, RoundingMode.HALF_UP);
                    }

                    return new FormaPagamentoResumoResponse(
                            FormaPagamento.valueOf(resultado.getForma()),
                            resultado.getQuantidadePagamentos(),
                            resultado.getValorRecebido(),
                            percentual);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FaturamentoDiarioResponse> buscarFaturamentoDiario(LocalDate inicio, LocalDate fim) {

        this.validarPeriodo(inicio, fim);

        OffsetDateTime inicioPeriodo = this.converterInicio(inicio);
        OffsetDateTime fimExclusivo = this.converterFimExclusivo(fim);

        List<FaturamentoDiarioProjection> resultados = dashboardRepository.buscarFaturamentoDiario(inicioPeriodo,
                fimExclusivo);

        return resultados.stream()
                .map(resultado -> new FaturamentoDiarioResponse(
                        resultado.getData(),
                        resultado.getFaturamento(),
                        resultado.getQuantidadeVendas()))
                .toList();
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (fim.isBefore(inicio)) {
            throw new PeriodoInvalidoException(inicio, fim);
        }
    }

    private OffsetDateTime converterInicio(LocalDate inicio) {
        return inicio
                .atStartOfDay(FUSO_HORARIO)
                .toOffsetDateTime();
    }

    private OffsetDateTime converterFimExclusivo(LocalDate fim) {
        return fim
                .plusDays(1)
                .atStartOfDay(FUSO_HORARIO)
                .toOffsetDateTime();
    }
}

package br.com.comandavision.api.dashboard.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.comandavision.api.dashboard.PeriodoInvalidoException;
import br.com.comandavision.api.dashboard.dto.ResumoDashboardResponse;
import br.com.comandavision.api.dashboard.projection.ResumoDashboardProjection;
import br.com.comandavision.api.dashboard.repository.DashboardRepository;

@Service
public class DashboardService {
    private static final ZoneId FUSO_HORARIO = ZoneId.of("America/Sao_Paulo");

    private final DashboardRepository dashboardRepository;

    public DashboardService(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    @Transactional(readOnly = true)
    public ResumoDashboardResponse buscarResumo(LocalDate inicio, LocalDate fim) {
        if (fim.isBefore(inicio)) {
            throw new PeriodoInvalidoException(inicio, fim);
        }

        OffsetDateTime inicioPeriodo = inicio
                .atStartOfDay(FUSO_HORARIO)
                .toOffsetDateTime();

        OffsetDateTime fimExclusivo = fim
                .plusDays(1)
                .atStartOfDay(FUSO_HORARIO)
                .toOffsetDateTime();

        ResumoDashboardProjection resultado = dashboardRepository.buscarResumo(inicioPeriodo, fimExclusivo);

        return new ResumoDashboardResponse(
                resultado.getFaturamento(),
                resultado.getQuantidadeVendas(),
                resultado.getTicketMedio(),
                resultado.getQuantidadeItensVendidos());
    }
}

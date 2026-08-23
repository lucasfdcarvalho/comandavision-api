package br.com.comandavision.api.dashboard.projection;

import java.math.BigDecimal;

public interface ResumoDashboardProjection {
    BigDecimal getFaturamento();

    Long getQuantidadeVendas();

    BigDecimal getTicketMedio();

    Long getQuantidadeItensVendidos();
}

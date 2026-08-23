package br.com.comandavision.api.dashboard.dto;

import java.math.BigDecimal;

public record ResumoDashboardResponse(
        BigDecimal faturamento,
        long quantidadeVendas,
        BigDecimal ticketMedio,
        long quantidadeItensVendidos) {

}

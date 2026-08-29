package br.com.comandavision.api.dashboard.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumo dos principais indicadores do dashboard")
public record ResumoDashboardResponse(
                @Schema(description = "Faturamento total do período", example = "5000.00") BigDecimal faturamento,
                @Schema(description = "Quantidade de vendas realizadas", example = "120") long quantidadeVendas,
                @Schema(description = "Valor médio por venda", example = "41.67") BigDecimal ticketMedio,
                @Schema(description = "Quantidade total de itens vendidos", example = "280") long quantidadeItensVendidos) {

}

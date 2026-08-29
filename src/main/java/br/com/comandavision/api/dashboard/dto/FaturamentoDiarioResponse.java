package br.com.comandavision.api.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumo do faturamento de um dia")
public record FaturamentoDiarioResponse(
                @Schema(description = "Data do faturamento", example = "2026-08-15") LocalDate data,
                @Schema(description = "Valor faturado no dia", example = "1250.50") BigDecimal faturamento,
                @Schema(description = "Quantidade de vendas realizadas no dia", example = "18") long quantidadeVendas) {

}

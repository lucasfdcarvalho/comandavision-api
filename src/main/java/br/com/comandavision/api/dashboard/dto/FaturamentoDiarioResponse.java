package br.com.comandavision.api.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FaturamentoDiarioResponse(
        LocalDate data,
        BigDecimal faturamento,
        long quantidadeVendas) {

}

package br.com.comandavision.api.dashboard.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface FaturamentoDiarioProjection {
    LocalDate getData();

    BigDecimal getFaturamento();

    Long getQuantidadeVendas();
}

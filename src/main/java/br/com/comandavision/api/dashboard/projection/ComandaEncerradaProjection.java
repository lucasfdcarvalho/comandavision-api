package br.com.comandavision.api.dashboard.projection;

import java.math.BigDecimal;
import java.time.Instant;

public interface ComandaEncerradaProjection {
    Long getId();

    String getIdentificacao();

    String getStatus();

    Instant getEncerradaEm();

    BigDecimal getTotal();

    BigDecimal getTotalPago();

    Boolean getComputadaNosIndicadores();
}

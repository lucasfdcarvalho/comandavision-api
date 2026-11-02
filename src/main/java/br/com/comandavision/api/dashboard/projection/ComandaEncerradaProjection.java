package br.com.comandavision.api.dashboard.projection;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public interface ComandaEncerradaProjection {
    Long getId();

    String getIdentificacao();

    String getStatus();

    OffsetDateTime getEncerradaEm();

    BigDecimal getTotal();

    BigDecimal getTotalPago();

    Boolean getComputadaNosIndicadores();
}

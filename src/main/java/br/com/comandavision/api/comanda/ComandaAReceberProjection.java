package br.com.comandavision.api.comanda;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public interface ComandaAReceberProjection {
    Long getId();

    String getIdentificacao();

    OffsetDateTime getFechadaEm();

    BigDecimal getTotal();

    BigDecimal getTotalPago();

    BigDecimal getSaldoRestante();

    String getSituacaoPagamento();
}

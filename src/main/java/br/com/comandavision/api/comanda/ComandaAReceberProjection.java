package br.com.comandavision.api.comanda;

import java.math.BigDecimal;
import java.time.Instant;

public interface ComandaAReceberProjection {
    Long getId();

    String getIdentificacao();

    Instant getFechadaEm();

    BigDecimal getTotal();

    BigDecimal getTotalPago();

    BigDecimal getSaldoRestante();

    String getSituacaoPagamento();
}

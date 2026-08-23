package br.com.comandavision.api.dashboard.projection;

import java.math.BigDecimal;

public interface FormaPagamentoResumoProjection {
    String getForma();

    Long getQuantidadePagamentos();

    BigDecimal getValorRecebido();

}

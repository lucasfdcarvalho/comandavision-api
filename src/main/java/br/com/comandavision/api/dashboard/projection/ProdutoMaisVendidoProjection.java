package br.com.comandavision.api.dashboard.projection;

import java.math.BigDecimal;

public interface ProdutoMaisVendidoProjection {
    Long getProdutoId();

    String getProdutoNome();

    Long getQuantidadeVendida();

    BigDecimal getFaturamento();
}

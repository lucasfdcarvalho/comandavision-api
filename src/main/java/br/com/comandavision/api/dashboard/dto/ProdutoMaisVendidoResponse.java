package br.com.comandavision.api.dashboard.dto;

import java.math.BigDecimal;

public record ProdutoMaisVendidoResponse(
        Long produtoId,
        String produtoNome,
        long quantidadeVendida,
        BigDecimal faturamento) {

}

package br.com.comandavision.api.dashboard.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumo de vendas de um produto")
public record ProdutoMaisVendidoResponse(
                @Schema(description = "Identificador do produto", example = "1") Long produtoId,
                @Schema(description = "Nome do produto", example = "Coca-Cola 350 ml") String produtoNome,
                @Schema(description = "Quantidade vendida no período", example = "35") long quantidadeVendida,
                @Schema(description = "Faturamento gerado pelo produto", example = "227.50") BigDecimal faturamento) {

}

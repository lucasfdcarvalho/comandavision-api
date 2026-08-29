package br.com.comandavision.api.produto.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import br.com.comandavision.api.categoria.dto.CategoriaResponse;
import br.com.comandavision.api.produto.Produto;

@Schema(description = "Dados de um produto")
public record ProdutoResponse(
        @Schema(description = "Identificador do produto", example = "1", accessMode = Schema.AccessMode.READ_ONLY) Long id,
        @Schema(description = "Nome do produto", example = "Coca-Cola 350 ml") String nome,
        @Schema(description = "Descrição do produto", example = "Refrigerante de cola em lata") String descricao,
        @Schema(description = "Preço unitário do produto", example = "6.50") BigDecimal preco,
        @Schema(description = "Categoria à qual o produto pertence") CategoriaResponse categoria,
        @Schema(description = "Indica se o produto está ativo", example = "true") boolean ativo,
        @Schema(description = "Data e hora do cadastro", example = "2026-08-29T18:30:00-03:00", accessMode = Schema.AccessMode.READ_ONLY) OffsetDateTime criadoEm,
        @Schema(description = "Data e hora da última atualização", example = "2026-08-29T19:00:00-03:00", accessMode = Schema.AccessMode.READ_ONLY) OffsetDateTime atualizadoEm) {

    public static ProdutoResponse from(Produto produto) {
        return new ProdutoResponse(
                produto.getId(),
                produto.getNome(),
                produto.getDescricao(),
                produto.getPreco(),
                CategoriaResponse.from(produto.getCategoria()),
                produto.isAtivo(),
                produto.getCriadoEm(),
                produto.getAtualizadoEm());
    }

}

package br.com.comandavision.api.produto.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import br.com.comandavision.api.categoria.dto.CategoriaResponse;
import br.com.comandavision.api.produto.Produto;

public record ProdutoResponse(
        Long id,
        String nome,
        String descricao,
        BigDecimal preco,
        CategoriaResponse categoria,
        boolean ativo,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm) {

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

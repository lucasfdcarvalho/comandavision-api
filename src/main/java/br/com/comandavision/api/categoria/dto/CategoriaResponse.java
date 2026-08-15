package br.com.comandavision.api.categoria.dto;

import java.time.OffsetDateTime;

import br.com.comandavision.api.categoria.Categoria;

public record CategoriaResponse(
        Long id,
        String nome,
        String descricao,
        boolean ativa,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm) {

    public static CategoriaResponse from(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getNome(),
                categoria.getDescricao(),
                categoria.isAtiva(),
                categoria.getCriadoEm(),
                categoria.getAtualizadoEm());
    }

}

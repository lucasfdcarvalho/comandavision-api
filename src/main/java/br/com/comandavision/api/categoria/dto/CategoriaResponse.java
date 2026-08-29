package br.com.comandavision.api.categoria.dto;

import java.time.OffsetDateTime;

import br.com.comandavision.api.categoria.Categoria;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados de uma categoria")
public record CategoriaResponse(
        @Schema(description = "Identificador da categoria", example = "1", accessMode = Schema.AccessMode.READ_ONLY) Long id,
        @Schema(description = "Nome da categoria", example = "Bebidas") String nome,
        @Schema(description = "Descrição da categoria", example = "Refrigerantes, sucos, águas e outras bebidas") String descricao,
        @Schema(description = "Indica se a categoria está ativa", example = "true") boolean ativa,
        @Schema(description = "Data e hora do cadastro", example = "2026-08-29T18:30:00-03:00", accessMode = Schema.AccessMode.READ_ONLY) OffsetDateTime criadoEm,
        @Schema(description = "Data e hora da última atualização", example = "2026-08-29T19:00:00-03:00", accessMode = Schema.AccessMode.READ_ONLY) OffsetDateTime atualizadoEm) {

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

package br.com.comandavision.api.categoria.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para o cadastro de uma categoria")
public record CriarCategoriaRequest(
                @Schema(description = "Nome da categoria", example = "Bebidas", maxLength = 100, requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "O nome da categoria é obrigatório") @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres") String nome,
                @Schema(description = "Descrição da categoria", example = "Refrigerantes, sucos, águas e outras bebidas", maxLength = 255) @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres") String descricao) {

}

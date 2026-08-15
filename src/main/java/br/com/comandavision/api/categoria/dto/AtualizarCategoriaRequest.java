package br.com.comandavision.api.categoria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AtualizarCategoriaRequest(
        @NotBlank(message = "O nome da categoria é obrigatório") @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres") String nome,

        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres") String descricao,

        @NotNull(message = "A situação da categoria é obrigatória") Boolean ativa) {

}

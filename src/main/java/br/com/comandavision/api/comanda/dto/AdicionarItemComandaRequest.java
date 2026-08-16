package br.com.comandavision.api.comanda.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AdicionarItemComandaRequest(
        @NotNull(message = "O produto é obrigatório") @Positive(message = "O ID do produto deve ser um número positivo") Long produtoId,

        @NotNull(message = "A quantidade é obrigatória") @Positive(message = "A quantidade deve ser maior que zero") Integer quantidade,

        @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres") String observacao) {

}

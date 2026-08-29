package br.com.comandavision.api.comanda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para adicionar um item à comanda")
public record AdicionarItemComandaRequest(
                @Schema(description = "Identificador do produto", example = "1", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "O produto é obrigatório") @Positive(message = "O ID do produto deve ser um número positivo") Long produtoId,

                @Schema(description = "Quantidade do produto", example = "2", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "A quantidade é obrigatória") @Positive(message = "A quantidade deve ser maior que zero") Integer quantidade,

                @Schema(description = "Observação específica do item", example = "Sem gelo", maxLength = 500) @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres") String observacao) {

}

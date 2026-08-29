package br.com.comandavision.api.comanda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para atualização de um item da comanda")
public record AtualizarItemComandaRequest(
                @Schema(description = "Nova quantidade do produto", example = "3", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "A quantidade é obrigatória") @Positive(message = "A quantidade deve ser maior que zero") Integer quantidade,

                @Schema(description = "Nova observação do item", example = "Adicionar limão", maxLength = 500) @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres") String observacao) {

}

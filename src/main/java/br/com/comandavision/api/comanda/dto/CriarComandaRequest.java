package br.com.comandavision.api.comanda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para abertura de uma comanda")
public record CriarComandaRequest(
                @Schema(description = "Identificação da comanda", example = "Mesa 05", maxLength = 100, requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "A identificação da comanda é obrigatória") @Size(max = 100, message = "A identificação deve ter no máximo 100 caracteres") String identificacao,

                @Schema(description = "Observação geral da comanda", example = "Cliente solicitou atendimento prioritário", maxLength = 500) @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres") String observacao) {

}

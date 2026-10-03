package br.com.comandavision.api.usuario.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import br.com.comandavision.api.usuario.PapelUsuario;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Novo papel do usuário")
public record AlterarPapelRequest(
                @Schema(description = "Papel do usuário", example = "DONO", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "O papel é obrigatório") PapelUsuario papel) {

}

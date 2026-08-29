package br.com.comandavision.api.autenticacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados do usuário autenticado")
public record UsuarioAutenticadoResponse(
                @Schema(description = "Identificador do usuário no Supabase", example = "550e8400-e29b-41d4-a716-446655440000", accessMode = Schema.AccessMode.READ_ONLY) String usuarioId,
                @Schema(description = "E-mail do usuário", example = "dono@comandavision.com.br", accessMode = Schema.AccessMode.READ_ONLY) String email,
                @Schema(description = "Papel utilizado no controle de acesso", example = "DONO", allowableValues = {
                                "DONO", "FUNCIONARIO" }, accessMode = Schema.AccessMode.READ_ONLY) String papel) {

}

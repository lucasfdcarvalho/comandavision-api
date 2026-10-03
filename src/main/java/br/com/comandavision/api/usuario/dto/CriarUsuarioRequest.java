package br.com.comandavision.api.usuario.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import br.com.comandavision.api.usuario.PapelUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para cadastro de um usuário da equipe")
public record CriarUsuarioRequest(
                @Schema(description = "Nome do usuário", example = "Maria Souza", maxLength = 120, requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "O nome é obrigatório") @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres") String nome,

                @Schema(description = "E-mail usado no login", example = "maria@comandavision.com.br", maxLength = 255, requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "O e-mail é obrigatório") @Email(message = "Informe um e-mail válido") @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres") String email,

                @Schema(description = "Senha provisória, repassada pelo dono ao usuário", example = "troca123", minLength = 6, maxLength = 72, requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "A senha é obrigatória") @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres") String senha,

                @Schema(description = "Papel do usuário", example = "FUNCIONARIO", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "O papel é obrigatório") PapelUsuario papel) {

}

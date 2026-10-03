package br.com.comandavision.api.usuario.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

import br.com.comandavision.api.usuario.PapelUsuario;
import br.com.comandavision.api.usuario.supabase.UsuarioSupabase;

@Schema(description = "Usuário da equipe")
public record UsuarioResponse(
        @Schema(description = "Identificador do usuário no Supabase", example = "550e8400-e29b-41d4-a716-446655440000") UUID id,
        @Schema(description = "Nome do usuário", example = "Maria Souza", nullable = true) String nome,
        @Schema(description = "E-mail usado no login", example = "maria@comandavision.com.br") String email,
        @Schema(description = "Papel do usuário. Nulo quando ainda não tem papel (não consegue usar o app)", example = "FUNCIONARIO", nullable = true) PapelUsuario papel,
        @Schema(description = "Indica se o usuário pode entrar no app", example = "true") boolean ativo,
        @Schema(description = "Data e hora do último login", example = "2026-10-01T18:30:00-03:00", nullable = true) OffsetDateTime ultimoAcesso,
        @Schema(description = "Data e hora do cadastro", example = "2026-09-01T10:00:00-03:00") OffsetDateTime criadoEm) {

    public static UsuarioResponse from(UsuarioSupabase usuario, PapelUsuario papel) {
        return new UsuarioResponse(
                usuario.id(),
                usuario.nome(),
                usuario.email(),
                papel,
                usuario.estaAtivo(),
                usuario.ultimoAcesso(),
                usuario.criadoEm());
    }
}

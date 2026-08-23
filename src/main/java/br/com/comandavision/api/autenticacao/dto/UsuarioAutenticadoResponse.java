package br.com.comandavision.api.autenticacao.dto;

public record UsuarioAutenticadoResponse(
        String usuarioId,
        String email,
        String papel) {

}

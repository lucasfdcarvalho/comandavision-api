package br.com.comandavision.api.usuario;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class GestaoUsuariosIndisponivelException extends RuntimeException {
    public GestaoUsuariosIndisponivelException() {
        super("A gestão de usuários não está configurada no servidor");
    }
}

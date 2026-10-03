package br.com.comandavision.api.usuario;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class OperacaoUsuarioInvalidaException extends RuntimeException {
    public OperacaoUsuarioInvalidaException(String mensagem) {
        super(mensagem);
    }
}

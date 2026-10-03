package br.com.comandavision.api.usuario;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class DadosUsuarioInvalidosException extends RuntimeException {
    public DadosUsuarioInvalidosException(String mensagem) {
        super(mensagem);
    }
}

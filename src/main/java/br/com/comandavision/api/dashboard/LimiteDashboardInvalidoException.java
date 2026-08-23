package br.com.comandavision.api.dashboard;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class LimiteDashboardInvalidoException extends RuntimeException {
    public LimiteDashboardInvalidoException(int limite) {
        super("O limite deve estar entre 1 e 20. Valor recebido: " + limite);
    }
}

package br.com.comandavision.api.dashboard;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PeriodoInvalidoException extends RuntimeException {
    public PeriodoInvalidoException(LocalDate inicio, LocalDate fim) {
        super("A data final " + fim + " não pode ser anterior à data inicial " + inicio);
    }

}

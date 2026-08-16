package br.com.comandavision.api.comanda;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ComandaNaoEstaAbertaException extends RuntimeException {
    public ComandaNaoEstaAbertaException(Long id, StatusComanda status) {
        super("A comanda de ID " + id + " não está aberta. Status atual: " + status);
    }
}

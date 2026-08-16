package br.com.comandavision.api.comanda;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ComandaNaoEncontradaException extends RuntimeException {
    public ComandaNaoEncontradaException(Long id) {
        super("Comanda não encontrada com o ID: " + id);
    }
}

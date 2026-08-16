package br.com.comandavision.api.comanda;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ItemComandaNaoEncontradoException extends RuntimeException {
    public ItemComandaNaoEncontradoException(Long itemId, Long comandaId) {
        super("O item de ID " + itemId + " não foi encontrado na comanda de ID " + comandaId);
    }
}

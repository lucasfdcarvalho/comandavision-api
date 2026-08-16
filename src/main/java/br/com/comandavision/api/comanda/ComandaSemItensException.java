package br.com.comandavision.api.comanda;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ComandaSemItensException extends RuntimeException {
    public ComandaSemItensException(Long id) {
        super("A comanda de ID " + id + " não pode ser fechada porque não possui itens");
    }
}

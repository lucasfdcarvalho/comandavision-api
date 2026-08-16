package br.com.comandavision.api.produto;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ProdutoInativoException extends RuntimeException {
    public ProdutoInativoException(Long id) {
        super("O produto de ID " + id + " está inativo e não pode ser adicionado à comanda");
    }
}

package br.com.comandavision.api.categoria;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class CategoriaInativaException extends RuntimeException {
    public CategoriaInativaException(Long id) {
        super("A categoria de ID " + id + " está inativa");
    }
}

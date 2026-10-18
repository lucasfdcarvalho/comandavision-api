package br.com.comandavision.api.pix;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class FalhaMercadoPagoException extends RuntimeException {
    public FalhaMercadoPagoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}

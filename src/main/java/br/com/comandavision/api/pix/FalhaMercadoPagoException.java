package br.com.comandavision.api.pix;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class FalhaMercadoPagoException extends RuntimeException {
    private final boolean resultadoIncerto;

    public FalhaMercadoPagoException(String mensagem, Throwable causa) {
        this(mensagem, causa, true);
    }

    public FalhaMercadoPagoException(String mensagem, Throwable causa, boolean resultadoIncerto) {
        super(mensagem, causa);
        this.resultadoIncerto = resultadoIncerto;
    }

    public boolean resultadoIncerto() {
        return resultadoIncerto;
    }
}

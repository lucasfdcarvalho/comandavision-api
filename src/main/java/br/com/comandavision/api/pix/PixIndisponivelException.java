package br.com.comandavision.api.pix;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class PixIndisponivelException extends RuntimeException {
    public PixIndisponivelException() {
        super("O pagamento por Pix não está configurado no servidor");
    }
}

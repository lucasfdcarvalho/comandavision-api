package br.com.comandavision.api.pagamento;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ChaveIdempotenciaInvalidaException extends IllegalArgumentException {
    public ChaveIdempotenciaInvalidaException(String mensagem) {
        super(mensagem);
    }
}

package br.com.comandavision.api.pagamento;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ChaveIdempotenciaEmConflitoException extends IllegalStateException {
    public ChaveIdempotenciaEmConflitoException() {
        super("Esta Idempotency-Key já foi usada com dados diferentes. Inicie uma nova tentativa");
    }
}

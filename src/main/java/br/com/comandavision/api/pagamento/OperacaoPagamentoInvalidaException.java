package br.com.comandavision.api.pagamento;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class OperacaoPagamentoInvalidaException extends IllegalStateException {
    public OperacaoPagamentoInvalidaException(String mensagem) {
        super(mensagem);
    }
}

package br.com.comandavision.api.pagamento;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PagamentoNaoEncontradoException extends RuntimeException {
    public PagamentoNaoEncontradoException(Long pagamentoId, Long comandaId) {
        super("O pagamento de ID " + pagamentoId + " não foi encontrado na comanda de ID " + comandaId);
    }
}

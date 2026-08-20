package br.com.comandavision.api.pagamento;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import br.com.comandavision.api.comanda.StatusComanda;

@ResponseStatus(HttpStatus.CONFLICT)
public class ComandaNaoPodeReceberPagamentoException extends RuntimeException {
    public ComandaNaoPodeReceberPagamentoException(Long comandaId, StatusComanda status) {
        super("A comanda de ID " + comandaId + " não pode receber pagamentos. Status atual: " + status);
    }
}

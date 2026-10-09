package br.com.comandavision.api.comanda;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ComandaIdentificacaoDuplicadaException extends RuntimeException {
    public static final String INDICE_UNICO = "uk_comandas_abertas_identificacao";

    public ComandaIdentificacaoDuplicadaException() {
        super("Já existe uma comanda aberta com essa descrição.");
    }

    public ComandaIdentificacaoDuplicadaException(Throwable causa) {
        super("Já existe uma comanda aberta com essa descrição.", causa);
    }
}

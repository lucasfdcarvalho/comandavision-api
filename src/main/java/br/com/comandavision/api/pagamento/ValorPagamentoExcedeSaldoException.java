package br.com.comandavision.api.pagamento;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ValorPagamentoExcedeSaldoException extends RuntimeException {
    public ValorPagamentoExcedeSaldoException(BigDecimal valor, BigDecimal saldoRestante) {
        super("O valor do pagamento, R$ " + valor.toPlainString() + ", ultrapassa o saldo restante de R$ "
                + saldoRestante.toPlainString());
    }
}

package br.com.comandavision.api.pix;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class CobrancaPixPendenteException extends RuntimeException {
    public CobrancaPixPendenteException(BigDecimal valorPendente) {
        super("Há um Pix de R$ " + valorPendente.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',')
                + " aguardando pagamento nesta comanda. Cancele o Pix ou aguarde a confirmação");
    }
}

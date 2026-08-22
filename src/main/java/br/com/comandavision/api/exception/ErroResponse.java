package br.com.comandavision.api.exception;

import java.time.OffsetDateTime;
import java.util.Map;

public record ErroResponse(
        int status,
        String erro,
        String mensagem,
        OffsetDateTime dataHora,
        Map<String, String> campos) {

}

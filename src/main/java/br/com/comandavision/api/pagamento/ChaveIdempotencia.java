package br.com.comandavision.api.pagamento;

import java.util.Locale;
import java.util.UUID;

public final class ChaveIdempotencia {
    private ChaveIdempotencia() {
    }

    public static String validar(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new ChaveIdempotenciaInvalidaException(
                    "Informe o cabeçalho Idempotency-Key com um UUID para registrar o pagamento");
        }

        String normalizada = valor.trim().toLowerCase(Locale.ROOT);
        try {
            UUID uuid = UUID.fromString(normalizada);
            if (!uuid.toString().equals(normalizada)) {
                throw new IllegalArgumentException("UUID fora do formato canônico");
            }
            return normalizada;
        } catch (IllegalArgumentException exception) {
            throw new ChaveIdempotenciaInvalidaException(
                    "O cabeçalho Idempotency-Key deve conter um UUID válido");
        }
    }
}

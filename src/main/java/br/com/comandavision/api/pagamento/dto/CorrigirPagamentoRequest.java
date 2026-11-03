package br.com.comandavision.api.pagamento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CorrigirPagamentoRequest(
        @NotBlank(message = "O motivo da correção é obrigatório")
        @Size(min = 5, max = 500, message = "O motivo deve ter entre 5 e 500 caracteres")
        String motivo) {
}

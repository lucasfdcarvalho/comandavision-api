package br.com.comandavision.api.pagamento.dto;

import java.math.BigDecimal;

import br.com.comandavision.api.pagamento.FormaPagamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para registro de um pagamento")
public record RegistrarPagamentoRequest(
                @Schema(description = "Forma utilizada no pagamento", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "A forma de pagamento é obrigatória") FormaPagamento forma,

                @Schema(description = "Valor do pagamento", example = "50.00", minimum = "0.01", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "O valor é obrigatório") @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero") @Digits(integer = 8, fraction = 2, message = "O valor deve ter no máximo 8 dígitos inteiros e 2 decimais") BigDecimal valor,

                @Schema(description = "Referência fornecida pelo meio de pagamento", example = "TXID-PIX-12345", maxLength = 255) @Size(max = 255, message = "A referência externa deve ter no máximo 255 caracteres") String referenciaExterna) {

}

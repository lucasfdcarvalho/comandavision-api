package br.com.comandavision.api.pix.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados para gerar uma cobrança Pix")
public record GerarPixRequest(
                @Schema(description = "Valor a cobrar no Pix", example = "50.00", minimum = "0.01", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "O valor é obrigatório") @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero") @Digits(integer = 8, fraction = 2, message = "O valor deve ter no máximo 8 dígitos inteiros e 2 decimais") BigDecimal valor) {

}

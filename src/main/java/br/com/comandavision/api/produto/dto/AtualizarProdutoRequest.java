package br.com.comandavision.api.produto.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para atualização de um produto")
public record AtualizarProdutoRequest(
                @Schema(description = "Nome do produto", example = "Coca-Cola 350 ml", maxLength = 120, requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "O nome do produto é obrigatório") @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres") String nome,

                @Schema(description = "Descrição do produto", example = "Refrigerante de cola em lata", maxLength = 500) @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres") String descricao,

                @Schema(description = "Preço unitário do produto", example = "6.50", minimum = "0.01", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "O preço é obrigatório") @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero") @Digits(integer = 8, fraction = 2, message = "O preço deve ter no máximo 8 dígitos inteiros e 2 decimais") BigDecimal preco,

                @Schema(description = "Identificador da categoria do produto", example = "1", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "A categoria é obrigatória") @Positive(message = "O ID da categoria deve ser um número positivo") Long categoriaId,

                @Schema(description = "Indica se o produto está ativo", example = "true", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "A situação do produto é obrigatória") Boolean ativo) {

}

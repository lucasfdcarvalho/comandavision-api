package br.com.comandavision.api.produto.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AtualizarProdutoRequest(
        @NotBlank(message = "O nome do produto é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String nome,

        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
        String descricao,

        @NotNull(message = "O preço é obrigatório")
        @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero")
        @Digits(integer = 8, fraction = 2, message = "O preço deve ter no máximo 8 dígitos inteiros e 2 decimais")
        BigDecimal preco,

        @NotNull(message = "A categoria é obrigatória")
        @Positive(message = "O ID da categoria deve ser um número positivo")
        Long categoriaId,

        @NotNull(message = "A situação do produto é obrigatória")
        Boolean ativo) {

}

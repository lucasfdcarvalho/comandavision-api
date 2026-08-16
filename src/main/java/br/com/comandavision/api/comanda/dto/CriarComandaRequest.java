package br.com.comandavision.api.comanda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarComandaRequest(
        @NotBlank(message = "A identificação da comanda é obrigatória") @Size(max = 100, message = "A identificação deve ter no máximo 100 caracteres") String identificacao,

        @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres") String observacao) {

}

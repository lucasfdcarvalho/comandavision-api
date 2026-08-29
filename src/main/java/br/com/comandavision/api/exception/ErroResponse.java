package br.com.comandavision.api.exception;

import java.time.OffsetDateTime;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta padronizada utilizada nos erros da API")
public record ErroResponse(
                @Schema(description = "Código HTTP do erro", example = "400", accessMode = Schema.AccessMode.READ_ONLY) int status,
                @Schema(description = "Nome correspondente ao código HTTP", example = "Bad Request", accessMode = Schema.AccessMode.READ_ONLY) String erro,
                @Schema(description = "Mensagem explicando o erro", example = "Os dados informados são inválidos", accessMode = Schema.AccessMode.READ_ONLY) String mensagem,
                @Schema(description = "Data e hora em que o erro ocorreu", example = "2026-08-29T20:30:00-03:00", accessMode = Schema.AccessMode.READ_ONLY) OffsetDateTime dataHora,
                @Schema(description = "Erros de validação organizados pelo nome do campo. Pode ser nulo quando não houver erros de campos", example = "{\"nome\":\"O nome da categoria é obrigatório\"}", accessMode = Schema.AccessMode.READ_ONLY) Map<String, String> campos) {

}

package br.com.comandavision.api.comanda.dto;

import java.time.OffsetDateTime;

import br.com.comandavision.api.comanda.Comanda;
import br.com.comandavision.api.comanda.StatusComanda;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados resumidos de uma comanda")
public record ComandaResponse(
        @Schema(description = "Identificador da comanda", example = "1", accessMode = Schema.AccessMode.READ_ONLY) Long id,
        @Schema(description = "Identificação da comanda", example = "Mesa 05") String identificacao,
        @Schema(description = "Situação atual da comanda", example = "ABERTA") StatusComanda status,
        @Schema(description = "Observação geral da comanda", example = "Cliente solicitou atendimento prioritário") String observacao,
        @Schema(description = "Data e hora de abertura", example = "2026-08-29T18:30:00-03:00", accessMode = Schema.AccessMode.READ_ONLY) OffsetDateTime abertaEm,
        @Schema(description = "Data e hora de fechamento", example = "2026-08-29T20:00:00-03:00", accessMode = Schema.AccessMode.READ_ONLY) OffsetDateTime fechadaEm,
        @Schema(description = "Data e hora da última atualização", example = "2026-08-29T20:00:00-03:00", accessMode = Schema.AccessMode.READ_ONLY) OffsetDateTime atualizadoEm) {
    public static ComandaResponse from(Comanda comanda) {
        return new ComandaResponse(
                comanda.getId(),
                comanda.getIdentificacao(),
                comanda.getStatus(),
                comanda.getObservacao(),
                comanda.getAbertaEm(),
                comanda.getFechadaEm(),
                comanda.getAtualizadoEm());
    }
}

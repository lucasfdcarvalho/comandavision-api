package br.com.comandavision.api.comanda.dto;

import java.util.List;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

import br.com.comandavision.api.comanda.Comanda;
import br.com.comandavision.api.comanda.ItemComanda;
import br.com.comandavision.api.comanda.StatusComanda;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados detalhados da comanda, incluindo itens e total")
public record ComandaDetalhadaResponse(
                @Schema(description = "Identificador da comanda", example = "1", accessMode = Schema.AccessMode.READ_ONLY) Long id,
                @Schema(description = "Identificação da comanda", example = "Mesa 05") String identificacao,
                @Schema(description = "Situação atual da comanda", example = "ABERTA") StatusComanda status,
                @Schema(description = "Observação geral da comanda", example = "Cliente solicitou atendimento prioritário") String observacao,
                @Schema(description = "Data e hora de abertura", example = "2026-08-29T18:30:00-03:00", accessMode = Schema.AccessMode.READ_ONLY) OffsetDateTime abertaEm,
                @Schema(description = "Data e hora de fechamento", example = "2026-08-29T20:00:00-03:00", accessMode = Schema.AccessMode.READ_ONLY) OffsetDateTime fechadaEm,
                @Schema(description = "Data e hora da última atualização", example = "2026-08-29T20:00:00-03:00", accessMode = Schema.AccessMode.READ_ONLY) OffsetDateTime atualizadoEm,
                @Schema(description = "Itens registrados na comanda", accessMode = Schema.AccessMode.READ_ONLY) List<ItemComandaResponse> itens,
                @Schema(description = "Valor total calculado da comanda", example = "39.00", accessMode = Schema.AccessMode.READ_ONLY) BigDecimal total) {
        public static ComandaDetalhadaResponse from(
                        Comanda comanda,
                        List<ItemComanda> itens) {

                List<ItemComandaResponse> itensResponse = itens.stream()
                                .map(ItemComandaResponse::from)
                                .toList();

                BigDecimal total = itens.stream()
                                .map(ItemComanda::calcularSubtotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                return new ComandaDetalhadaResponse(
                                comanda.getId(),
                                comanda.getIdentificacao(),
                                comanda.getStatus(),
                                comanda.getObservacao(),
                                comanda.getAbertaEm(),
                                comanda.getFechadaEm(),
                                comanda.getAtualizadoEm(),
                                itensResponse,
                                total);
        }
}

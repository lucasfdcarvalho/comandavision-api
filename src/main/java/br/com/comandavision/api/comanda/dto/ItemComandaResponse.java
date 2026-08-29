package br.com.comandavision.api.comanda.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import br.com.comandavision.api.comanda.ItemComanda;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados de um item da comanda")
public record ItemComandaResponse(
        @Schema(description = "Identificador do item", example = "10", accessMode = Schema.AccessMode.READ_ONLY) Long id,
        @Schema(description = "Identificador da comanda", example = "1") Long comandaId,
        @Schema(description = "Identificador do produto", example = "3") Long produtoId,
        @Schema(description = "Nome do produto", example = "Coca-Cola 350 ml") String produtoNome,
        @Schema(description = "Quantidade do produto", example = "2") int quantidade,
        @Schema(description = "Preço unitário registrado no momento da inclusão", example = "6.50") BigDecimal precoUnitario,
        @Schema(description = "Subtotal do item", example = "13.00", accessMode = Schema.AccessMode.READ_ONLY) BigDecimal subtotal,
        @Schema(description = "Observação do item", example = "Sem gelo") String observacao,
        @Schema(description = "Data e hora da inclusão", example = "2026-08-29T18:40:00-03:00", accessMode = Schema.AccessMode.READ_ONLY) OffsetDateTime criadoEm,
        @Schema(description = "Data e hora da última atualização", example = "2026-08-29T18:50:00-03:00", accessMode = Schema.AccessMode.READ_ONLY) OffsetDateTime atualizadoEm) {
    public static ItemComandaResponse from(ItemComanda item) {
        return new ItemComandaResponse(
                item.getId(),
                item.getComanda().getId(),
                item.getProduto().getId(),
                item.getProduto().getNome(),
                item.getQuantidade(),
                item.getPrecoUnitario(),
                item.calcularSubtotal(),
                item.getObservacao(),
                item.getCriadoEm(),
                item.getAtualizadoEm());
    }
}

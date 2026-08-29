package br.com.comandavision.api.pagamento.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import br.com.comandavision.api.pagamento.FormaPagamento;
import br.com.comandavision.api.pagamento.Pagamento;
import br.com.comandavision.api.pagamento.StatusPagamento;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados de um pagamento")
public record PagamentoResponse(
        @Schema(description = "Identificador do pagamento", example = "1") Long id,
        @Schema(description = "Identificador da comanda", example = "10") Long comandaId,
        @Schema(description = "Forma utilizada no pagamento") FormaPagamento forma,
        @Schema(description = "Situação atual do pagamento") StatusPagamento status,
        @Schema(description = "Valor pago", example = "50.00") BigDecimal valor,
        @Schema(description = "Referência externa do pagamento", example = "TXID-PIX-12345") String referenciaExterna,
        @Schema(description = "Data e hora do pagamento", example = "2026-08-29T20:00:00-03:00", accessMode = Schema.AccessMode.READ_ONLY) OffsetDateTime pagoEm,
        @Schema(description = "Data e hora de criação", example = "2026-08-29T20:00:00-03:00", accessMode = Schema.AccessMode.READ_ONLY) OffsetDateTime criadoEm,
        @Schema(description = "Data e hora de atualização", example = "2026-08-29T20:00:00-03:00", accessMode = Schema.AccessMode.READ_ONLY) OffsetDateTime atualizadoEm) {
    public static PagamentoResponse from(Pagamento pagamento) {
        return new PagamentoResponse(
                pagamento.getId(),
                pagamento.getComanda().getId(),
                pagamento.getForma(),
                pagamento.getStatus(),
                pagamento.getValor(),
                pagamento.getReferenciaExterna(),
                pagamento.getPagoEm(),
                pagamento.getCriadoEm(),
                pagamento.getAtualizadoEm());
    }
}

package br.com.comandavision.api.pagamento.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import br.com.comandavision.api.pagamento.FormaPagamento;
import br.com.comandavision.api.pagamento.Pagamento;
import br.com.comandavision.api.pagamento.StatusPagamento;

public record PagamentoResponse(
        Long id,
        Long comandaId,
        FormaPagamento forma,
        StatusPagamento status,
        BigDecimal valor,
        String referenciaExterna,
        OffsetDateTime pagoEm,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm) {
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

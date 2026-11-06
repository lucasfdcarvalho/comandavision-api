package br.com.comandavision.api.comanda.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import br.com.comandavision.api.comanda.ComandaAReceberProjection;
import br.com.comandavision.api.pagamento.SituacaoPagamento;

public record ComandaAReceberResponse(
        Long id,
        String identificacao,
        OffsetDateTime fechadaEm,
        BigDecimal total,
        BigDecimal totalPago,
        BigDecimal saldoRestante,
        SituacaoPagamento situacaoPagamento) {

    public static ComandaAReceberResponse from(ComandaAReceberProjection projection) {
        return new ComandaAReceberResponse(
                projection.getId(),
                projection.getIdentificacao(),
                projection.getFechadaEm(),
                projection.getTotal(),
                projection.getTotalPago(),
                projection.getSaldoRestante(),
                SituacaoPagamento.valueOf(projection.getSituacaoPagamento()));
    }
}

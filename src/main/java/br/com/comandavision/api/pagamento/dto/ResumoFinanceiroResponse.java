package br.com.comandavision.api.pagamento.dto;

import java.math.BigDecimal;

import br.com.comandavision.api.pagamento.SituacaoPagamento;

public record ResumoFinanceiroResponse(
        Long comandaId,
        BigDecimal totalComanda,
        BigDecimal totalPago,
        BigDecimal saldoRestante,
        SituacaoPagamento situacao) {

}

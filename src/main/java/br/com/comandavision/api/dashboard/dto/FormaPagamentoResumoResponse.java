package br.com.comandavision.api.dashboard.dto;

import java.math.BigDecimal;

import br.com.comandavision.api.pagamento.FormaPagamento;

public record FormaPagamentoResumoResponse(
        FormaPagamento forma,
        long quantidadePagamentos,
        BigDecimal valorRecebido,
        BigDecimal percentual) {

}

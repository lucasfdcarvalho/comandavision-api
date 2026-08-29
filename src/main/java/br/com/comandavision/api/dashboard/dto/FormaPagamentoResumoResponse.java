package br.com.comandavision.api.dashboard.dto;

import java.math.BigDecimal;

import br.com.comandavision.api.pagamento.FormaPagamento;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumo das vendas por forma de pagamento")
public record FormaPagamentoResumoResponse(
                @Schema(description = "Forma de pagamento") FormaPagamento forma,
                @Schema(description = "Quantidade de pagamentos confirmados", example = "40") long quantidadePagamentos,
                @Schema(description = "Valor recebido", example = "3200.00") BigDecimal valorRecebido,
                @Schema(description = "Participação percentual no faturamento", example = "64.00") BigDecimal percentual) {

}

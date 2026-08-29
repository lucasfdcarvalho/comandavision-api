package br.com.comandavision.api.pagamento.dto;

import java.math.BigDecimal;

import br.com.comandavision.api.pagamento.SituacaoPagamento;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumo financeiro de uma comanda")
public record ResumoFinanceiroResponse(
                @Schema(description = "Identificador da comanda", example = "10") Long comandaId,
                @Schema(description = "Valor total da comanda", example = "75.00") BigDecimal totalComanda,
                @Schema(description = "Total de pagamentos confirmados", example = "50.00") BigDecimal totalPago,
                @Schema(description = "Valor que ainda precisa ser pago", example = "25.00") BigDecimal saldoRestante,
                @Schema(description = "Situação financeira da comanda") SituacaoPagamento situacao) {

}

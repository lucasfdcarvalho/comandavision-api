package br.com.comandavision.api.pix.mercadopago;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Recorte do pedido (order) da API de Orders do Mercado Pago.
@JsonIgnoreProperties(ignoreUnknown = true)
public record PedidoMercadoPago(
        String id,
        String status,
        @JsonProperty("status_detail") String detalheStatus,
        @JsonProperty("transactions") Transacoes transacoes) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Transacoes(@JsonProperty("payments") List<PagamentoPedido> pagamentos) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PagamentoPedido(
            String id,
            String status,
            @JsonProperty("payment_method") MetodoPagamento metodo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MetodoPagamento(
            String id,
            @JsonProperty("qr_code") String qrCode,
            @JsonProperty("qr_code_base64") String qrCodeBase64) {
    }

    private MetodoPagamento metodoPix() {
        if (transacoes == null || transacoes.pagamentos() == null) {
            return null;
        }
        return transacoes.pagamentos().stream()
                .map(PagamentoPedido::metodo)
                .filter(metodo -> metodo != null && metodo.qrCode() != null)
                .findFirst()
                .orElse(null);
    }

    public String qrCode() {
        MetodoPagamento metodo = metodoPix();
        return metodo == null ? null : metodo.qrCode();
    }

    public String qrCodeBase64() {
        MetodoPagamento metodo = metodoPix();
        return metodo == null ? null : metodo.qrCodeBase64();
    }

    // Status da order: https://www.mercadopago.com.br/developers/pt/docs/checkout-api-orders/payment-management/status/order-status
    public boolean foiPago() {
        return "processed".equals(status);
    }

    public boolean foiDevolvido() {
        return "refunded".equals(status) || "charged_back".equals(status);
    }

    public boolean foiEncerradoSemPagamento() {
        return "canceled".equals(status) || "failed".equals(status) || "expired".equals(status);
    }
}

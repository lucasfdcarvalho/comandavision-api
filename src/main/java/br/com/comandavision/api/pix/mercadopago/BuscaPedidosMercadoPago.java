package br.com.comandavision.api.pix.mercadopago;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BuscaPedidosMercadoPago(List<PedidoMercadoPago> data) {
}

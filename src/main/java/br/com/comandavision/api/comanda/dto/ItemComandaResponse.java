package br.com.comandavision.api.comanda.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import br.com.comandavision.api.comanda.ItemComanda;

public record ItemComandaResponse(
        Long id,
        Long comandaId,
        Long produtoId,
        String produtoNome,
        int quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal,
        String observacao,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm) {
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

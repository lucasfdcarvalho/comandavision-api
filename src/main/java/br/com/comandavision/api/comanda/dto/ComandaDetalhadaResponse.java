package br.com.comandavision.api.comanda.dto;

import java.util.List;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

import br.com.comandavision.api.comanda.Comanda;
import br.com.comandavision.api.comanda.ItemComanda;
import br.com.comandavision.api.comanda.StatusComanda;

public record ComandaDetalhadaResponse(
        Long id,
        String identificacao,
        StatusComanda status,
        String observacao,
        OffsetDateTime abertaEm,
        OffsetDateTime fechadaEm,
        OffsetDateTime atualizadoEm,
        List<ItemComandaResponse> itens,
        BigDecimal total) {
    public static ComandaDetalhadaResponse from(
            Comanda comanda,
            List<ItemComanda> itens) {

        List<ItemComandaResponse> itensResponse = itens.stream()
                .map(ItemComandaResponse::from)
                .toList();

        BigDecimal total = itens.stream()
                .map(ItemComanda::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ComandaDetalhadaResponse(
                comanda.getId(),
                comanda.getIdentificacao(),
                comanda.getStatus(),
                comanda.getObservacao(),
                comanda.getAbertaEm(),
                comanda.getFechadaEm(),
                comanda.getAtualizadoEm(),
                itensResponse,
                total);
    }
}

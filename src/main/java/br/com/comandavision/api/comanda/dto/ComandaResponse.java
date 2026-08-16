package br.com.comandavision.api.comanda.dto;

import java.time.OffsetDateTime;

import br.com.comandavision.api.comanda.Comanda;
import br.com.comandavision.api.comanda.StatusComanda;

public record ComandaResponse(
        Long id,
        String identificacao,
        StatusComanda status,
        String observacao,
        OffsetDateTime abertaEm,
        OffsetDateTime fechadaEm,
        OffsetDateTime atualizadoEm) {
    public static ComandaResponse from(Comanda comanda) {
        return new ComandaResponse(
                comanda.getId(),
                comanda.getIdentificacao(),
                comanda.getStatus(),
                comanda.getObservacao(),
                comanda.getAbertaEm(),
                comanda.getFechadaEm(),
                comanda.getAtualizadoEm());
    }
}

package br.com.comandavision.api.dashboard.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import br.com.comandavision.api.comanda.StatusComanda;

public record ComandaEncerradaResponse(
        Long id,
        String identificacao,
        StatusComanda status,
        OffsetDateTime encerradaEm,
        BigDecimal total,
        BigDecimal totalPago,
        boolean computadaNosIndicadores) {
}

package br.com.comandavision.api.pix.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import br.com.comandavision.api.pagamento.StatusPagamento;
import br.com.comandavision.api.pix.CobrancaPix;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cobrança Pix de uma comanda")
public record CobrancaPixResponse(
        @Schema(description = "Identificador do pagamento ligado ao Pix", example = "12") Long pagamentoId,
        @Schema(description = "Identificador da comanda", example = "10") Long comandaId,
        @Schema(description = "Valor cobrado", example = "50.00") BigDecimal valor,
        @Schema(description = "PENDENTE enquanto aguarda; CONFIRMADO quando pago; CANCELADO se expirou ou foi cancelado") StatusPagamento status,
        @Schema(description = "Código Pix copia e cola") String qrCode,
        @Schema(description = "Imagem PNG do QR Code em Base64") String qrCodeBase64,
        @Schema(description = "Data e hora em que o QR Code deixa de valer", example = "2026-10-20T20:30:00-03:00") OffsetDateTime expiraEm) {

    public static CobrancaPixResponse from(CobrancaPix cobranca) {
        return new CobrancaPixResponse(
                cobranca.getPagamento().getId(),
                cobranca.getPagamento().getComanda().getId(),
                cobranca.getPagamento().getValor(),
                cobranca.getPagamento().getStatus(),
                cobranca.getQrCode(),
                cobranca.getQrCodeBase64(),
                cobranca.getExpiraEm());
    }
}

package br.com.comandavision.api.pix;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.comandavision.api.exception.ErroResponse;
import br.com.comandavision.api.pix.dto.CobrancaPixResponse;
import br.com.comandavision.api.pix.dto.GerarPixRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/comandas/{comandaId}/pagamentos")
@Tag(name = "Pix", description = "Cobranças Pix pelo Mercado Pago")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
        @ApiResponse(responseCode = "403", description = "Usuário sem permissão", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
        @ApiResponse(responseCode = "502", description = "Falha ao falar com o Mercado Pago", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
        @ApiResponse(responseCode = "503", description = "Pix não configurado no servidor", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
})
public class PixController {

    private final PixService pixService;

    public PixController(PixService pixService) {
        this.pixService = pixService;
    }

    @Operation(summary = "Gerar Pix", description = "Gera o QR Code de um Pix para a comanda fechada. O pagamento fica PENDENTE até o cliente pagar")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pix gerado"),
            @ApiResponse(responseCode = "409", description = "Comanda não fechada, valor acima do saldo ou outro Pix pendente", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping("/pix")
    @ResponseStatus(HttpStatus.CREATED)
    public CobrancaPixResponse gerar(
            @PathVariable Long comandaId,
            @RequestHeader(name = "Idempotency-Key", required = false) String chaveIdempotencia,
            @Valid @RequestBody GerarPixRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return pixService.gerar(
                comandaId,
                request.valor(),
                chaveIdempotencia,
                UUID.fromString(jwt.getSubject()));
    }

    @Operation(summary = "Consultar Pix", description = "Confere no Mercado Pago se o Pix foi pago e devolve a situação atual")
    @ApiResponse(responseCode = "200", description = "Situação do Pix")
    @GetMapping("/{pagamentoId}/pix")
    public CobrancaPixResponse consultar(@PathVariable Long comandaId, @PathVariable Long pagamentoId) {
        return pixService.consultar(comandaId, pagamentoId);
    }

    @Operation(summary = "Cancelar Pix", description = "Cancela um Pix que ainda não foi pago")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pix cancelado"),
            @ApiResponse(responseCode = "409", description = "Pix já pago ou já encerrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PatchMapping("/{pagamentoId}/pix/cancelar")
    public CobrancaPixResponse cancelar(@PathVariable Long comandaId, @PathVariable Long pagamentoId) {
        return pixService.cancelar(comandaId, pagamentoId);
    }
}

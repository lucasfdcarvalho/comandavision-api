package br.com.comandavision.api.pagamento;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import br.com.comandavision.api.exception.ErroResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.comandavision.api.pagamento.dto.PagamentoResponse;
import br.com.comandavision.api.pagamento.dto.CorrigirPagamentoRequest;
import br.com.comandavision.api.pagamento.dto.RegistrarPagamentoRequest;
import br.com.comandavision.api.pagamento.dto.ResumoFinanceiroResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/comandas/{comandaId}/pagamentos")
@Tag(name = "Pagamentos", description = "Operações para gerenciamento dos pagamentos das comandas")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
        @ApiResponse(responseCode = "403", description = "Usuário sem permissão", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
})
public class PagamentoController {
    private final PagamentoService pagamentoService;

    public PagamentoController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    @Operation(summary = "Registrar pagamento", description = "Registra um pagamento para uma comanda fechada")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pagamento registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou pagamento não permitido", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Comanda não encontrada", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PagamentoResponse registrar(@PathVariable Long comandaId,
            @RequestHeader(name = "Idempotency-Key", required = false) String chaveIdempotencia,
            @Valid @RequestBody RegistrarPagamentoRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return pagamentoService.registrar(
                comandaId,
                request,
                chaveIdempotencia,
                UUID.fromString(jwt.getSubject()));
    }

    @Operation(summary = "Listar pagamentos", description = "Retorna os pagamentos registrados para uma comanda")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagamentos retornados com sucesso"),
            @ApiResponse(responseCode = "404", description = "Comanda não encontrada", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping
    public List<PagamentoResponse> listar(@PathVariable Long comandaId) {
        return pagamentoService.listar(comandaId);
    }

    @Operation(summary = "Corrigir pagamento manual", description = "Anula um registro manual incorreto sem movimentar dinheiro")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro de pagamento corrigido com sucesso"),
            @ApiResponse(responseCode = "400", description = "O registro não pode ser corrigido", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Comanda ou pagamento não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PatchMapping("/{pagamentoId}/corrigir")
    public PagamentoResponse corrigirRegistro(
            @PathVariable Long comandaId,
            @PathVariable Long pagamentoId,
            @Valid @RequestBody CorrigirPagamentoRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return pagamentoService.corrigirRegistro(
                comandaId,
                pagamentoId,
                request,
                UUID.fromString(jwt.getSubject()));
    }

    @Operation(summary = "Compatibilidade: estornar pagamento manual", description = "Rota temporária para versões anteriores do aplicativo. Mantém auditoria e não movimenta dinheiro")
    @PatchMapping("/{pagamentoId}/estornar")
    public PagamentoResponse corrigirRegistroLegado(
            @PathVariable Long comandaId,
            @PathVariable Long pagamentoId,
            @Valid @RequestBody(required = false) CorrigirPagamentoRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        CorrigirPagamentoRequest requestAuditado = request == null
                ? new CorrigirPagamentoRequest("Correção registrada por versão anterior do aplicativo")
                : request;
        return pagamentoService.corrigirRegistro(
                comandaId,
                pagamentoId,
                requestAuditado,
                UUID.fromString(jwt.getSubject()));
    }

    @Operation(summary = "Consultar resumo financeiro", description = "Retorna o total da comanda, o valor pago e o saldo restante")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumo financeiro retornado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Comanda não encontrada", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/resumo")
    public ResumoFinanceiroResponse buscarResumo(@PathVariable Long comandaId) {
        return pagamentoService.buscarResumo(comandaId);
    }
}

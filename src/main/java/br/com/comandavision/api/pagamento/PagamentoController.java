package br.com.comandavision.api.pagamento;

import java.util.List;

import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.comandavision.api.pagamento.dto.PagamentoResponse;
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
            @Valid @RequestBody RegistrarPagamentoRequest request) {
        return pagamentoService.registrar(comandaId, request);
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

    @Operation(summary = "Estornar pagamento", description = "Estorna um pagamento registrado na comanda")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagamento estornado com sucesso"),
            @ApiResponse(responseCode = "400", description = "O pagamento não pode ser estornado", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Comanda ou pagamento não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PatchMapping("/{pagamentoId}/estornar")
    public PagamentoResponse estornar(@PathVariable Long comandaId, @PathVariable Long pagamentoId) {

        return pagamentoService.estornar(comandaId, pagamentoId);
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

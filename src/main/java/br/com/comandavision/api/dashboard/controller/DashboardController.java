package br.com.comandavision.api.dashboard.controller;

import java.time.LocalDate;
import java.util.List;

import br.com.comandavision.api.exception.ErroResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.comandavision.api.dashboard.dto.FaturamentoDiarioResponse;
import br.com.comandavision.api.dashboard.dto.FormaPagamentoResumoResponse;
import br.com.comandavision.api.dashboard.dto.ProdutoMaisVendidoResponse;
import br.com.comandavision.api.dashboard.dto.ResumoDashboardResponse;
import br.com.comandavision.api.dashboard.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Indicadores gerenciais disponíveis somente para o dono")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
        @ApiResponse(responseCode = "403", description = "Usuário não possui o perfil DONO", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
})
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "Consultar resumo do dashboard", description = "Retorna faturamento, vendas, ticket médio e itens vendidos no período")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumo retornado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Período informado é inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/resumo")
    public ResumoDashboardResponse buscarResumo(
            @Parameter(description = "Data inicial no formato AAAA-MM-DD", example = "2026-08-01", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,

            @Parameter(description = "Data final no formato AAAA-MM-DD", example = "2026-08-31", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {

        return dashboardService.buscarResumo(inicio, fim);
    }

    @Operation(summary = "Consultar produtos mais vendidos", description = "Retorna os produtos com maior quantidade vendida no período")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produtos retornados com sucesso"),
            @ApiResponse(responseCode = "400", description = "Período ou limite informado é inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/produtos-mais-vendidos")
    public List<ProdutoMaisVendidoResponse> buscarProdutosMaisVendidos(
            @Parameter(description = "Data inicial no formato AAAA-MM-DD", example = "2026-08-01", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,

            @Parameter(description = "Data final no formato AAAA-MM-DD", example = "2026-08-31", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,

            @Parameter(description = "Quantidade máxima de produtos retornados", example = "5") @RequestParam(defaultValue = "5") int limite) {

        return dashboardService.buscarProdutosMaisVendidos(inicio, fim, limite);
    }

    @Operation(summary = "Consultar vendas por forma de pagamento", description = "Retorna quantidade, valor recebido e participação de cada forma de pagamento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumo por forma de pagamento retornado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Período informado é inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/formas-pagamento")
    public List<FormaPagamentoResumoResponse> buscarResumoPorFormaPagamento(
            @Parameter(description = "Data inicial no formato AAAA-MM-DD", example = "2026-08-01", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,

            @Parameter(description = "Data final no formato AAAA-MM-DD", example = "2026-08-31", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {

        return dashboardService.buscarResumoPorFormaPagamento(inicio, fim);
    }

    @Operation(summary = "Consultar faturamento diário", description = "Retorna o faturamento e a quantidade de vendas de cada dia do período")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Faturamento diário retornado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Período informado é inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/faturamento-diario")
    public List<FaturamentoDiarioResponse> buscarFaturamentoDiario(
            @Parameter(description = "Data inicial no formato AAAA-MM-DD", example = "2026-08-01", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,

            @Parameter(description = "Data final no formato AAAA-MM-DD", example = "2026-08-31", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {

        return dashboardService.buscarFaturamentoDiario(inicio, fim);
    }
}

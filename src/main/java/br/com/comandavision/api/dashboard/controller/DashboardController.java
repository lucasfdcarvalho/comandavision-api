package br.com.comandavision.api.dashboard.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.comandavision.api.dashboard.dto.FormaPagamentoResumoResponse;
import br.com.comandavision.api.dashboard.dto.ProdutoMaisVendidoResponse;
import br.com.comandavision.api.dashboard.dto.ResumoDashboardResponse;
import br.com.comandavision.api.dashboard.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/resumo")
    public ResumoDashboardResponse buscarResumo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,

            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {

        return dashboardService.buscarResumo(inicio, fim);
    }

    @GetMapping("/produtos-mais-vendidos")
    public List<ProdutoMaisVendidoResponse> buscarProdutosMaisVendidos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,

            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,

            @RequestParam(defaultValue = "5") int limite) {

        return dashboardService.buscarProdutosMaisVendidos(inicio, fim, limite);
    }

    @GetMapping("/formas-pagamento")
    public List<FormaPagamentoResumoResponse> buscarResumoPorFormaPagamento(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,

            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {

        return dashboardService.buscarResumoPorFormaPagamento(inicio, fim);
    }
}

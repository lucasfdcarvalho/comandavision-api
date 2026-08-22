package br.com.comandavision.api.pagamento;

import java.util.List;

import org.springframework.http.HttpStatus;
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
public class PagamentoController {
    private final PagamentoService pagamentoService;

    public PagamentoController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PagamentoResponse registrar(@PathVariable Long comandaId,
            @Valid @RequestBody RegistrarPagamentoRequest request) {
        return pagamentoService.registrar(comandaId, request);
    }

    @GetMapping
    public List<PagamentoResponse> listar(@PathVariable Long comandaId) {
        return pagamentoService.listar(comandaId);
    }

    @PatchMapping("/{pagamentoId}/estornar")
    public PagamentoResponse estornar(@PathVariable Long comandaId, @PathVariable Long pagamentoId) {

        return pagamentoService.estornar(comandaId, pagamentoId);
    }

    @GetMapping("/resumo")
    public ResumoFinanceiroResponse buscarResumo(@PathVariable Long comandaId) {
        return pagamentoService.buscarResumo(comandaId);
    }
}

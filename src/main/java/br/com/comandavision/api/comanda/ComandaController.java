package br.com.comandavision.api.comanda;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.comandavision.api.comanda.dto.AdicionarItemComandaRequest;
import br.com.comandavision.api.comanda.dto.AtualizarItemComandaRequest;
import br.com.comandavision.api.comanda.dto.ComandaResponse;
import br.com.comandavision.api.comanda.dto.CriarComandaRequest;
import br.com.comandavision.api.comanda.dto.ItemComandaResponse;
import br.com.comandavision.api.comanda.dto.ComandaDetalhadaResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/comandas")
public class ComandaController {
    private final ComandaService comandaService;

    public ComandaController(ComandaService comandaService) {
        this.comandaService = comandaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComandaResponse abrir(@Valid @RequestBody CriarComandaRequest request) {
        return this.comandaService.abrir(request);
    }

    @GetMapping
    public List<ComandaResponse> listar() {
        return this.comandaService.listar();
    }

    @GetMapping("/{id}")
    public ComandaDetalhadaResponse buscarPorId(@PathVariable Long id) {
        return this.comandaService.buscarPorId(id);
    }

    @PostMapping("/{comandaId}/itens")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemComandaResponse adicionarItem(
            @PathVariable Long comandaId,
            @Valid @RequestBody AdicionarItemComandaRequest request) {

        return comandaService.adicionarItem(comandaId, request);
    }

    @PutMapping("/{comandaId}/itens/{itemId}")
    public ItemComandaResponse atualizarItem(
            @PathVariable Long comandaId,
            @PathVariable Long itemId,
            @Valid @RequestBody AtualizarItemComandaRequest request) {

        return comandaService.atualizarItem(
                comandaId,
                itemId,
                request);
    }

    @DeleteMapping("/{comandaId}/itens/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerItem(
            @PathVariable Long comandaId,
            @PathVariable Long itemId) {

        comandaService.removerItem(comandaId, itemId);
    }
}

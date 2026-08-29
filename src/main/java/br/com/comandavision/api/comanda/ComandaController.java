package br.com.comandavision.api.comanda;

import java.util.List;
import org.springframework.http.HttpStatus;
import br.com.comandavision.api.exception.ErroResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@Tag(name = "Comandas", description = "Operações para gerenciamento das comandas e seus itens")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
        @ApiResponse(responseCode = "403", description = "Usuário sem permissão", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
})
public class ComandaController {
    private final ComandaService comandaService;

    public ComandaController(ComandaService comandaService) {
        this.comandaService = comandaService;
    }

    @Operation(summary = "Abrir comanda", description = "Abre uma nova comanda para registrar o consumo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Comanda aberta com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da comanda inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComandaResponse abrir(@Valid @RequestBody CriarComandaRequest request) {
        return this.comandaService.abrir(request);
    }

    @Operation(summary = "Listar comandas", description = "Retorna todas as comandas cadastradas")
    @ApiResponse(responseCode = "200", description = "Comandas retornadas com sucesso")
    @GetMapping
    public List<ComandaResponse> listar() {
        return this.comandaService.listar();
    }

    @Operation(summary = "Buscar comanda por ID", description = "Retorna a comanda com seus itens e o valor total")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comanda encontrada"),
            @ApiResponse(responseCode = "404", description = "Comanda não encontrada", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}")
    public ComandaDetalhadaResponse buscarPorId(@PathVariable Long id) {
        return this.comandaService.buscarPorId(id);
    }

    @Operation(summary = "Adicionar item à comanda", description = "Adiciona um produto e sua quantidade a uma comanda aberta")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Item adicionado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou comanda não permite alterações", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Comanda ou produto não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping("/{comandaId}/itens")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemComandaResponse adicionarItem(
            @PathVariable Long comandaId,
            @Valid @RequestBody AdicionarItemComandaRequest request) {

        return comandaService.adicionarItem(comandaId, request);
    }

    @Operation(summary = "Atualizar item da comanda", description = "Atualiza a quantidade e a observação de um item")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou comanda não permite alterações", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Comanda ou item não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
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

    @Operation(summary = "Remover item da comanda", description = "Remove um item de uma comanda aberta")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Item removido com sucesso"),
            @ApiResponse(responseCode = "400", description = "A comanda não permite alterações", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Comanda ou item não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @DeleteMapping("/{comandaId}/itens/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerItem(
            @PathVariable Long comandaId,
            @PathVariable Long itemId) {

        comandaService.removerItem(comandaId, itemId);
    }

    @Operation(summary = "Fechar comanda", description = "Fecha a comanda e impede novas alterações em seus itens")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comanda fechada com sucesso"),
            @ApiResponse(responseCode = "400", description = "A comanda não pode ser fechada", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Comanda não encontrada", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PatchMapping("/{id}/fechar")
    public ComandaDetalhadaResponse fechar(@PathVariable Long id) {
        return comandaService.fechar(id);
    }

    @Operation(summary = "Cancelar comanda", description = "Cancela uma comanda existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comanda cancelada com sucesso"),
            @ApiResponse(responseCode = "400", description = "A comanda não pode ser cancelada", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Comanda não encontrada", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PatchMapping("/{id}/cancelar")
    public ComandaDetalhadaResponse cancelar(@PathVariable Long id) {
        return comandaService.cancelar(id);
    }
}

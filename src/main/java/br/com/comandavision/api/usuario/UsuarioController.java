package br.com.comandavision.api.usuario;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.comandavision.api.exception.ErroResponse;
import br.com.comandavision.api.usuario.dto.AlterarPapelRequest;
import br.com.comandavision.api.usuario.dto.CriarUsuarioRequest;
import br.com.comandavision.api.usuario.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuários", description = "Gestão da equipe, exclusiva para o papel DONO")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
        @ApiResponse(responseCode = "403", description = "Usuário sem permissão", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
        @ApiResponse(responseCode = "503", description = "Gestão de usuários não configurada no servidor", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
})
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Listar usuários", description = "Retorna todas as contas com papel e situação de acesso")
    @ApiResponse(responseCode = "200", description = "Usuários retornados com sucesso")
    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    @Operation(summary = "Cadastrar usuário", description = "Cria a conta com senha provisória e já atribui o papel")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados do usuário inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criar(@Valid @RequestBody CriarUsuarioRequest request) {
        return usuarioService.criar(request);
    }

    @Operation(summary = "Alterar papel", description = "Define o papel do usuário. Vale a partir da próxima renovação do login dele")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Papel alterado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "409", description = "Alteração do próprio papel ou remoção do último DONO", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PatchMapping("/{id}/papel")
    public UsuarioResponse alterarPapel(
            @PathVariable UUID id,
            @Valid @RequestBody AlterarPapelRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return usuarioService.alterarPapel(id, request, UUID.fromString(jwt.getSubject()));
    }

    @Operation(summary = "Desativar usuário", description = "Bloqueia o login sem apagar a conta nem o histórico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário desativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "409", description = "Tentativa de desativar a própria conta", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PatchMapping("/{id}/desativar")
    public UsuarioResponse desativar(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return usuarioService.desativar(id, UUID.fromString(jwt.getSubject()));
    }

    @Operation(summary = "Reativar usuário", description = "Libera novamente o login de um usuário desativado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário reativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PatchMapping("/{id}/reativar")
    public UsuarioResponse reativar(@PathVariable UUID id) {
        return usuarioService.reativar(id);
    }
}

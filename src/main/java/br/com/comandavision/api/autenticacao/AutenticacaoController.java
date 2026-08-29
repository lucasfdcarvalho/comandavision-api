package br.com.comandavision.api.autenticacao;

import br.com.comandavision.api.exception.ErroResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.comandavision.api.autenticacao.dto.UsuarioAutenticadoResponse;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Informações do usuário autenticado")
public class AutenticacaoController {
    @Operation(summary = "Consultar usuário autenticado", description = "Retorna o identificador, e-mail e papel do usuário representado pelo token JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário autenticado retornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário sem um papel permitido", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/me")
    public UsuarioAutenticadoResponse buscarUsuarioAutenticado(@AuthenticationPrincipal Jwt jwt) {

        return new UsuarioAutenticadoResponse(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("user_role"));
    }
}

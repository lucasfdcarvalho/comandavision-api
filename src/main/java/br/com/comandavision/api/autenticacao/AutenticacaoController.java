package br.com.comandavision.api.autenticacao;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.comandavision.api.autenticacao.dto.UsuarioAutenticadoResponse;

@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {
    @GetMapping("/me")
    public UsuarioAutenticadoResponse buscarUsuarioAutenticado(@AuthenticationPrincipal Jwt jwt) {

        return new UsuarioAutenticadoResponse(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("user_role"));
    }
}

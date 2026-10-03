package br.com.comandavision.api.usuario.supabase;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import br.com.comandavision.api.usuario.DadosUsuarioInvalidosException;
import br.com.comandavision.api.usuario.EmailJaCadastradoException;
import br.com.comandavision.api.usuario.GestaoUsuariosIndisponivelException;

// Cliente da Admin API do Supabase Auth. Usa a service role key, que só existe no backend.
@Component
public class SupabaseAdminClient {
    // "Banir" por ~100 anos é a forma do Supabase de bloquear o login sem apagar a conta.
    private static final String DURACAO_BLOQUEIO = "876000h";
    private static final String SEM_BLOQUEIO = "none";

    private final RestClient restClient;
    private final boolean configurado;

    public SupabaseAdminClient(
            @Value("${supabase.project-url}") String urlProjeto,
            @Value("${supabase.service-role-key:}") String chaveServico) {

        this.configurado = chaveServico != null && !chaveServico.isBlank();
        this.restClient = RestClient.builder()
                .baseUrl(urlProjeto + "/auth/v1/admin")
                .defaultHeader("apikey", chaveServico)
                .defaultHeader("Authorization", "Bearer " + chaveServico)
                .build();
    }

    public List<UsuarioSupabase> listarUsuarios() {
        verificarConfiguracao();

        ListaUsuariosSupabase resposta = restClient.get()
                .uri("/users?page=1&per_page=1000")
                .retrieve()
                .body(ListaUsuariosSupabase.class);

        return resposta == null || resposta.users() == null ? List.of() : resposta.users();
    }

    public UsuarioSupabase buscarUsuario(UUID id) {
        verificarConfiguracao();

        try {
            return restClient.get()
                    .uri("/users/{id}", id)
                    .retrieve()
                    .body(UsuarioSupabase.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                return null;
            }
            throw exception;
        }
    }

    public UsuarioSupabase criarUsuario(String nome, String email, String senha) {
        verificarConfiguracao();

        Map<String, Object> corpo = Map.of(
                "email", email,
                "password", senha,
                "email_confirm", true,
                "user_metadata", Map.of("nome", nome));

        try {
            return restClient.post()
                    .uri("/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .body(UsuarioSupabase.class);
        } catch (RestClientResponseException exception) {
            String resposta = exception.getResponseBodyAsString();

            if (resposta.contains("email_exists") || resposta.contains("already been registered")) {
                throw new EmailJaCadastradoException();
            }
            if (resposta.contains("weak_password")) {
                throw new DadosUsuarioInvalidosException("A senha é fraca. Use pelo menos 6 caracteres, misturando letras e números");
            }
            if (resposta.contains("email_address_invalid") || resposta.contains("validation_failed")) {
                throw new DadosUsuarioInvalidosException("O e-mail informado é inválido");
            }
            throw exception;
        }
    }

    public void excluirUsuario(UUID id) {
        verificarConfiguracao();

        restClient.delete()
                .uri("/users/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }

    public UsuarioSupabase definirBloqueio(UUID id, boolean bloqueado) {
        verificarConfiguracao();

        return restClient.put()
                .uri("/users/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("ban_duration", bloqueado ? DURACAO_BLOQUEIO : SEM_BLOQUEIO))
                .retrieve()
                .body(UsuarioSupabase.class);
    }

    private void verificarConfiguracao() {
        if (!configurado) {
            // Falta a variável de ambiente SUPABASE_SERVICE_ROLE_KEY.
            throw new GestaoUsuariosIndisponivelException();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ListaUsuariosSupabase(List<UsuarioSupabase> users) {
    }
}

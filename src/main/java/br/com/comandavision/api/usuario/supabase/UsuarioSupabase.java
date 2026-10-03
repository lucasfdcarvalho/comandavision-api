package br.com.comandavision.api.usuario.supabase;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Recorte do usuário devolvido pela Admin API do Supabase Auth.
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsuarioSupabase(
        UUID id,
        String email,
        @JsonProperty("user_metadata") Map<String, Object> metadados,
        @JsonProperty("banned_until") OffsetDateTime banidoAte,
        @JsonProperty("last_sign_in_at") OffsetDateTime ultimoAcesso,
        @JsonProperty("created_at") OffsetDateTime criadoEm) {

    public String nome() {
        Object nome = metadados == null ? null : metadados.get("nome");
        return nome instanceof String texto && !texto.isBlank() ? texto : null;
    }

    public boolean estaAtivo() {
        return banidoAte == null || banidoAte.isBefore(OffsetDateTime.now());
    }
}

package br.com.comandavision.api.usuario;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios_papeis")
public class UsuarioPapel {
    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;

    // Coluna do tipo enum do Postgres `comandavision.papel_usuario`.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private PapelUsuario papel;

    @Column(name = "criado_em", insertable = false, updatable = false)
    private OffsetDateTime criadoEm;

    protected UsuarioPapel() {
    }

    public UsuarioPapel(UUID usuarioId, PapelUsuario papel) {
        this.usuarioId = usuarioId;
        this.papel = papel;
    }

    public UUID getUsuarioId() {
        return this.usuarioId;
    }

    public PapelUsuario getPapel() {
        return this.papel;
    }

    public void setPapel(PapelUsuario papel) {
        this.papel = papel;
    }
}

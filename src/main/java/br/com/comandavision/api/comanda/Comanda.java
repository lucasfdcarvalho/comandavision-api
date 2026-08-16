package br.com.comandavision.api.comanda;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;

@Entity
@Table(name = "comandas")
public class Comanda {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String identificacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusComanda status = StatusComanda.ABERTA;

    @Column(length = 500)
    private String observacao;

    @CreationTimestamp
    @Column(name = "aberta_em", nullable = false, updatable = false)
    private OffsetDateTime abertaEm;

    @Column(name = "fechada_em")
    private OffsetDateTime fechadaEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    protected Comanda() {
    }

    public Comanda(String identificacao, String observacao) {
        this.identificacao = identificacao;
        this.observacao = observacao;
    }

    public Long getId() {
        return id;
    }

    public String getIdentificacao() {
        return identificacao;
    }

    public StatusComanda getStatus() {
        return status;
    }

    public String getObservacao() {
        return observacao;
    }

    public OffsetDateTime getAbertaEm() {
        return abertaEm;
    }

    public OffsetDateTime getFechadaEm() {
        return fechadaEm;
    }

    public OffsetDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setIdentificacao(String identificacao) {
        this.identificacao = identificacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public void fechar() {
        if (this.status != StatusComanda.ABERTA) {
            throw new IllegalStateException("Somente uma comanda aberta pode ser fechada");
        }

        this.status = StatusComanda.FECHADA;
        this.fechadaEm = OffsetDateTime.now();
    }

    public void cancelar() {
        if (this.status != StatusComanda.ABERTA) {
            throw new IllegalStateException("Somente uma comanda aberta pode ser cancelada");
        }

        this.status = StatusComanda.CANCELADA;
        this.fechadaEm = OffsetDateTime.now();
    }

    public boolean estaAberta() {
        return this.status == StatusComanda.ABERTA;
    }

}

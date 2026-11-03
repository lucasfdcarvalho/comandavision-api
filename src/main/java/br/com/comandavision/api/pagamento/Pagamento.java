package br.com.comandavision.api.pagamento;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import br.com.comandavision.api.comanda.Comanda;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "pagamentos")
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comanda_id", nullable = false)
    private Comanda comanda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FormaPagamento forma;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPagamento status = StatusPagamento.PENDENTE;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "referencia_externa", length = 255)
    private String referenciaExterna;

    @Column(name = "chave_idempotencia", length = 128)
    private String chaveIdempotencia;

    @Column(name = "pago_em")
    private OffsetDateTime pagoEm;

    @Column(name = "motivo_correcao", length = 500)
    private String motivoCorrecao;

    @Column(name = "corrigido_por")
    private UUID corrigidoPor;

    @Column(name = "corrigido_em")
    private OffsetDateTime corrigidoEm;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    protected Pagamento() {
    }

    public Pagamento(Comanda comanda, FormaPagamento forma, BigDecimal valor) {
        this(comanda, forma, valor, null);
    }

    public Pagamento(Comanda comanda, FormaPagamento forma, BigDecimal valor, String chaveIdempotencia) {
        this.comanda = comanda;
        this.forma = forma;
        this.valor = valor;
        this.chaveIdempotencia = chaveIdempotencia;
    }

    public Long getId() {
        return this.id;
    }

    public Comanda getComanda() {
        return this.comanda;
    }

    public FormaPagamento getForma() {
        return this.forma;
    }

    public StatusPagamento getStatus() {
        return this.status;
    }

    public BigDecimal getValor() {
        return this.valor;
    }

    public String getReferenciaExterna() {
        return this.referenciaExterna;
    }

    public String getChaveIdempotencia() {
        return this.chaveIdempotencia;
    }

    public OffsetDateTime getPagoEm() {
        return this.pagoEm;
    }

    public String getMotivoCorrecao() {
        return this.motivoCorrecao;
    }

    public UUID getCorrigidoPor() {
        return this.corrigidoPor;
    }

    public OffsetDateTime getCorrigidoEm() {
        return this.corrigidoEm;
    }

    public OffsetDateTime getCriadoEm() {
        return this.criadoEm;
    }

    public OffsetDateTime getAtualizadoEm() {
        return this.atualizadoEm;
    }

    public void confirmar(String referenciaExterna) {
        if (this.status != StatusPagamento.PENDENTE) {
            throw new OperacaoPagamentoInvalidaException(
                    "Somente um pagamento pendente pode ser confirmado");
        }

        this.status = StatusPagamento.CONFIRMADO;
        this.referenciaExterna = referenciaExterna;
        this.pagoEm = OffsetDateTime.now();
    }

    public void cancelar() {
        if (this.status != StatusPagamento.PENDENTE) {
            throw new OperacaoPagamentoInvalidaException(
                    "Somente um pagamento pendente pode ser cancelado");
        }

        this.status = StatusPagamento.CANCELADO;
    }

    public void corrigirRegistro(String motivo, UUID usuarioId) {
        if (this.status != StatusPagamento.CONFIRMADO) {
            throw new OperacaoPagamentoInvalidaException(
                    "Somente um pagamento confirmado pode ser corrigido");
        }
        if (motivo == null || motivo.isBlank() || usuarioId == null) {
            throw new OperacaoPagamentoInvalidaException(
                    "Informe o motivo e o usuário responsável pela correção");
        }

        this.status = StatusPagamento.ESTORNADO;
        this.motivoCorrecao = motivo.trim();
        this.corrigidoPor = usuarioId;
        this.corrigidoEm = OffsetDateTime.now();
    }

    public void confirmarDevolucaoPeloProvedor() {
        if (this.status != StatusPagamento.CONFIRMADO) {
            throw new OperacaoPagamentoInvalidaException(
                    "Somente um pagamento confirmado pode receber a devolução do provedor");
        }

        this.status = StatusPagamento.ESTORNADO;
    }

    public boolean estaConfirmado() {
        return this.status == StatusPagamento.CONFIRMADO;
    }

}

package br.com.comandavision.api.pix;

import java.time.OffsetDateTime;

import br.com.comandavision.api.pagamento.Pagamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "cobrancas_pix")
public class CobrancaPix {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pagamento_id", nullable = false, unique = true)
    private Pagamento pagamento;

    @Column(name = "pedido_mercado_pago", nullable = false, unique = true, length = 64)
    private String pedidoMercadoPago;

    @Column(name = "qr_code", nullable = false)
    private String qrCode;

    @Column(name = "qr_code_base64", nullable = false)
    private String qrCodeBase64;

    @Column(name = "expira_em", nullable = false)
    private OffsetDateTime expiraEm;

    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime criadoEm;

    protected CobrancaPix() {
    }

    public CobrancaPix(Pagamento pagamento, String pedidoMercadoPago, String qrCode, String qrCodeBase64,
            OffsetDateTime expiraEm) {
        this.pagamento = pagamento;
        this.pedidoMercadoPago = pedidoMercadoPago;
        this.qrCode = qrCode;
        this.qrCodeBase64 = qrCodeBase64;
        this.expiraEm = expiraEm;
    }

    public Long getId() {
        return this.id;
    }

    public Pagamento getPagamento() {
        return this.pagamento;
    }

    public String getPedidoMercadoPago() {
        return this.pedidoMercadoPago;
    }

    public String getQrCode() {
        return this.qrCode;
    }

    public String getQrCodeBase64() {
        return this.qrCodeBase64;
    }

    public OffsetDateTime getExpiraEm() {
        return this.expiraEm;
    }

    public boolean expirou(OffsetDateTime agora) {
        return this.expiraEm.isBefore(agora);
    }
}

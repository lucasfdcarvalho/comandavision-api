package br.com.comandavision.api.pagamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.comandavision.api.comanda.Comanda;

public class PagamentoTest {
    @Test
    public void deveComecarComStatusPendente() {
        Comanda comanda = new Comanda("Mesa 5", null);
        Pagamento pagamento = new Pagamento(comanda, FormaPagamento.PIX, new BigDecimal("20.00"));
        assertEquals(StatusPagamento.PENDENTE, pagamento.getStatus());
        assertNull(pagamento.getPagoEm());
    }

    @Test
    public void deveConfirmarPagamentoPendente() {
        Comanda comanda = new Comanda("Mesa 5", null);
        Pagamento pagamento = new Pagamento(comanda, FormaPagamento.PIX, new BigDecimal("20.00"));
        pagamento.confirmar("TRANSACAO-123");
        assertEquals(StatusPagamento.CONFIRMADO, pagamento.getStatus());
        assertEquals("TRANSACAO-123", pagamento.getReferenciaExterna());
        assertNotNull(pagamento.getPagoEm());
        assertTrue(pagamento.estaConfirmado());
    }

    @Test
    public void deveCancelarPagamentoPendente() {
        Comanda comanda = new Comanda("Mesa 5", null);
        Pagamento pagamento = new Pagamento(comanda, FormaPagamento.PIX, new BigDecimal("20.00"));
        pagamento.cancelar();
        assertEquals(StatusPagamento.CANCELADO, pagamento.getStatus());
        assertNull(pagamento.getPagoEm());
    }

    @Test
    public void naoDeveConfirmarPagamentoJaConfirmado() {
        Comanda comanda = new Comanda("Mesa 5", null);
        Pagamento pagamento = new Pagamento(comanda, FormaPagamento.PIX, new BigDecimal("20.00"));
        pagamento.confirmar("TRANSACAO-123");
        assertThrows(IllegalStateException.class, () -> pagamento.confirmar("OUTRA-REFERENCIA"));
    }

    @Test
    public void deveCorrigirPagamentoConfirmadoComAuditoria() {
        Comanda comanda = new Comanda("Mesa 5", null);
        Pagamento pagamento = new Pagamento(comanda, FormaPagamento.PIX, new BigDecimal("20.00"));
        pagamento.confirmar("TRANSACAO-123");
        var dataDoPagamento = pagamento.getPagoEm();

        UUID usuarioId = UUID.fromString("b35bbcb0-1128-45a2-bd16-cb8d99be154f");
        pagamento.corrigirRegistro("Pagamento duplicado", usuarioId);

        assertEquals(StatusPagamento.ESTORNADO, pagamento.getStatus());
        assertEquals(dataDoPagamento, pagamento.getPagoEm());
        assertFalse(pagamento.estaConfirmado());
        assertEquals("Pagamento duplicado", pagamento.getMotivoCorrecao());
        assertEquals(usuarioId, pagamento.getCorrigidoPor());
        assertNotNull(pagamento.getCorrigidoEm());
    }

    @Test
    public void deveSepararDevolucaoDoProvedorDeCorrecaoManual() {
        Comanda comanda = new Comanda("Mesa 5", null);
        Pagamento pagamento = new Pagamento(comanda, FormaPagamento.PIX, new BigDecimal("20.00"));
        pagamento.confirmar("TRANSACAO-123");

        pagamento.confirmarDevolucaoPeloProvedor();

        assertEquals(StatusPagamento.ESTORNADO, pagamento.getStatus());
        assertNull(pagamento.getMotivoCorrecao());
        assertNull(pagamento.getCorrigidoPor());
        assertNull(pagamento.getCorrigidoEm());
    }
}

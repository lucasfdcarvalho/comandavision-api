package br.com.comandavision.api.comanda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ComandaTest {
    @Test
    public void deveCriarComandaComStatusAberta() {
        Comanda comanda = new Comanda("Mesa 5", "Cliente próximo à janela");

        assertEquals(StatusComanda.ABERTA, comanda.getStatus());
        assertNull(comanda.getFechadaEm());
    }

    @Test
    public void deveFecharComandaAberta() {
        Comanda comanda = new Comanda("Mesa 5", null);

        comanda.fechar();

        assertEquals(StatusComanda.FECHADA, comanda.getStatus());
        assertNotNull(comanda.getFechadaEm());
    }

    @Test
    public void deveCancelarComandaAberta() {
        Comanda comanda = new Comanda("Mesa 5", null);

        comanda.cancelar();

        assertEquals(StatusComanda.CANCELADA, comanda.getStatus());
        assertNotNull(comanda.getFechadaEm());
    }

    @Test
    public void naoDeveFecharComandaJaFechada() {
        Comanda comanda = new Comanda("Mesa 5", null);
        comanda.fechar();

        assertThrows(IllegalStateException.class, () -> comanda.fechar());
    }
}

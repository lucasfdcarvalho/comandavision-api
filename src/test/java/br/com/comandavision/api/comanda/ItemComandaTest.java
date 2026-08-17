package br.com.comandavision.api.comanda;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import br.com.comandavision.api.categoria.Categoria;
import br.com.comandavision.api.produto.Produto;

public class ItemComandaTest {
    @Test
    public void deveCalcularSubtotalDoItem() {
        Categoria categoria = new Categoria("Bebidas", null);

        Produto produto = new Produto(
                categoria,
                "Coca-Cola",
                null,
                new BigDecimal("6.00"));

        Comanda comanda = new Comanda("Mesa 5", null);

        ItemComanda item = new ItemComanda(
                comanda,
                produto,
                3,
                null);

        assertEquals(
                new BigDecimal("18.00"),
                item.calcularSubtotal());
    }

    @Test
    public void deveManterPrecoRegistradoQuandoProdutoMudarDePreco() {
        Categoria categoria = new Categoria("Bebidas", null);

        Produto produto = new Produto(
                categoria,
                "Coca-Cola",
                null,
                new BigDecimal("6.00"));

        Comanda comanda = new Comanda("Mesa 5", null);

        ItemComanda item = new ItemComanda(
                comanda,
                produto,
                2,
                null);

        produto.setPreco(new BigDecimal("7.00"));

        assertEquals(
                new BigDecimal("6.00"),
                item.getPrecoUnitario());

        assertEquals(
                new BigDecimal("12.00"),
                item.calcularSubtotal());
    }
}

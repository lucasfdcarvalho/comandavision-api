package br.com.comandavision.api.produto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import br.com.comandavision.api.produto.dto.CriarProdutoRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

public class ProdutoRequestValidacaoTest {

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    public static void configurar() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    public static void finalizar() {
        fabrica.close();
    }

    @Test
    public void deveAceitarProdutoSemImagem() {
        Set<ConstraintViolation<CriarProdutoRequest>> violacoes = validador.validate(requestComImagem(null));

        assertTrue(violacoes.isEmpty());
    }

    @Test
    public void deveAceitarImagemComUrlHttps() {
        Set<ConstraintViolation<CriarProdutoRequest>> violacoes = validador.validate(
                requestComImagem("https://xyz.supabase.co/storage/v1/object/public/produtos/coca-cola.jpg"));

        assertTrue(violacoes.isEmpty());
    }

    @Test
    public void deveRejeitarImagemComUrlInvalida() {
        Set<ConstraintViolation<CriarProdutoRequest>> violacoes = validador.validate(
                requestComImagem("file:///storage/emulated/0/foto.jpg"));

        assertEquals(1, violacoes.size());
        assertEquals("imagemUrl", violacoes.iterator().next().getPropertyPath().toString());
    }

    private CriarProdutoRequest requestComImagem(String imagemUrl) {
        return new CriarProdutoRequest(
                "Coca-Cola 350 ml",
                "Refrigerante de cola em lata",
                new BigDecimal("6.50"),
                1L,
                imagemUrl);
    }
}

package br.com.comandavision.api.produto;

import java.util.List;

import org.springframework.data.domain.Sort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.comandavision.api.categoria.Categoria;
import br.com.comandavision.api.categoria.CategoriaNaoEncontradaException;
import br.com.comandavision.api.categoria.CategoriaRepository;
import br.com.comandavision.api.produto.dto.AtualizarProdutoRequest;
import br.com.comandavision.api.produto.dto.CriarProdutoRequest;
import br.com.comandavision.api.produto.dto.ProdutoResponse;

@ExtendWith(MockitoExtension.class)
public class ProdutoServiceTest {
    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private ProdutoService produtoService;

    @Test
    public void deveCriarProduto() {
        Categoria categoria = new Categoria(
                "Bebidas",
                "Refrigerantes, sucos e águas");

        CriarProdutoRequest request = new CriarProdutoRequest(
                "Coca-Cola 350 ml",
                "Refrigerante de cola em lata",
                new BigDecimal("6.50"),
                1L);

        when(categoriaRepository.findById(1L))
                .thenReturn(Optional.of(categoria));

        when(produtoRepository.save(any(Produto.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        ProdutoResponse resposta = produtoService.criar(request);

        assertEquals("Coca-Cola 350 ml", resposta.nome());
        assertEquals(
                "Refrigerante de cola em lata",
                resposta.descricao());
        assertEquals(
                new BigDecimal("6.50"),
                resposta.preco());
        assertEquals(
                "Bebidas",
                resposta.categoria().nome());
        assertTrue(resposta.ativo());

        verify(categoriaRepository).findById(1L);
        verify(produtoRepository).save(any(Produto.class));
    }

    @Test
    public void deveLancarExcecaoAoCriarProdutoComCategoriaInexistente() {
        CriarProdutoRequest request = new CriarProdutoRequest(
                "Coca-Cola 350 ml",
                "Refrigerante de cola em lata",
                new BigDecimal("6.50"),
                99L);

        when(categoriaRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                CategoriaNaoEncontradaException.class,
                () -> produtoService.criar(request));

        verify(categoriaRepository).findById(99L);
        verifyNoInteractions(produtoRepository);
    }

    @Test
    public void deveListarProdutosEmOrdemAlfabetica() {
        Categoria categoria = new Categoria(
                "Bebidas",
                "Refrigerantes, sucos e águas");

        Produto agua = new Produto(
                categoria,
                "Água mineral",
                "Garrafa de água mineral",
                new BigDecimal("3.50"));

        Produto refrigerante = new Produto(
                categoria,
                "Coca-Cola 350 ml",
                "Refrigerante de cola em lata",
                new BigDecimal("6.50"));

        Sort ordenacao = Sort.by(
                Sort.Direction.ASC,
                "nome");

        when(produtoRepository.findAll(ordenacao))
                .thenReturn(List.of(agua, refrigerante));

        List<ProdutoResponse> resposta = produtoService.listar();

        assertEquals(2, resposta.size());

        assertEquals("Água mineral", resposta.get(0).nome());
        assertEquals(
                new BigDecimal("3.50"),
                resposta.get(0).preco());

        assertEquals(
                "Coca-Cola 350 ml",
                resposta.get(1).nome());
        assertEquals(
                new BigDecimal("6.50"),
                resposta.get(1).preco());

        verify(produtoRepository).findAll(ordenacao);
    }

    @Test
    public void deveBuscarProdutoPorId() {
        Categoria categoria = new Categoria(
                "Bebidas",
                "Refrigerantes, sucos e águas");

        Produto produto = new Produto(
                categoria,
                "Coca-Cola 350 ml",
                "Refrigerante de cola em lata",
                new BigDecimal("6.50"));

        when(produtoRepository.findById(1L))
                .thenReturn(Optional.of(produto));

        ProdutoResponse resposta = produtoService.buscarPorId(1L);

        assertEquals("Coca-Cola 350 ml", resposta.nome());
        assertEquals(
                "Refrigerante de cola em lata",
                resposta.descricao());
        assertEquals(
                new BigDecimal("6.50"),
                resposta.preco());
        assertEquals(
                "Bebidas",
                resposta.categoria().nome());
        assertTrue(resposta.ativo());

        verify(produtoRepository).findById(1L);
    }

    @Test
    public void deveLancarExcecaoQuandoProdutoNaoForEncontrado() {
        when(produtoRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ProdutoNaoEncontradoException.class,
                () -> produtoService.buscarPorId(99L));

        verify(produtoRepository).findById(99L);
    }

    @Test
    public void deveAtualizarProduto() {
        Categoria categoriaAtual = new Categoria(
                "Bebidas",
                "Refrigerantes, sucos e águas");

        Categoria novaCategoria = new Categoria(
                "Bebidas sem álcool",
                "Bebidas não alcoólicas");

        Produto produto = new Produto(
                categoriaAtual,
                "Refrigerante",
                "Refrigerante em lata",
                new BigDecimal("6.00"));

        AtualizarProdutoRequest request = new AtualizarProdutoRequest(
                "Coca-Cola 350 ml",
                "Refrigerante de cola em lata",
                new BigDecimal("6.50"),
                2L,
                false);

        when(produtoRepository.findById(1L))
                .thenReturn(Optional.of(produto));

        when(categoriaRepository.findById(2L))
                .thenReturn(Optional.of(novaCategoria));

        ProdutoResponse resposta = produtoService.atualizar(1L, request);

        assertEquals("Coca-Cola 350 ml", resposta.nome());
        assertEquals(
                "Refrigerante de cola em lata",
                resposta.descricao());
        assertEquals(
                new BigDecimal("6.50"),
                resposta.preco());
        assertEquals(
                "Bebidas sem álcool",
                resposta.categoria().nome());
        assertFalse(resposta.ativo());

        verify(produtoRepository).findById(1L);
        verify(categoriaRepository).findById(2L);
    }

    @Test
    public void deveLancarExcecaoAoAtualizarProdutoInexistente() {
        AtualizarProdutoRequest request = new AtualizarProdutoRequest(
                "Coca-Cola 350 ml",
                "Refrigerante de cola em lata",
                new BigDecimal("6.50"),
                1L,
                true);

        when(produtoRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ProdutoNaoEncontradoException.class,
                () -> produtoService.atualizar(99L, request));

        verify(produtoRepository).findById(99L);
        verifyNoInteractions(categoriaRepository);
    }

    @Test
    public void deveLancarExcecaoAoAtualizarProdutoComCategoriaInexistente() {
        Categoria categoriaAtual = new Categoria(
                "Bebidas",
                "Refrigerantes, sucos e águas");

        Produto produto = new Produto(
                categoriaAtual,
                "Refrigerante",
                "Refrigerante em lata",
                new BigDecimal("6.00"));

        AtualizarProdutoRequest request = new AtualizarProdutoRequest(
                "Coca-Cola 350 ml",
                "Refrigerante de cola em lata",
                new BigDecimal("6.50"),
                99L,
                true);

        when(produtoRepository.findById(1L))
                .thenReturn(Optional.of(produto));

        when(categoriaRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                CategoriaNaoEncontradaException.class,
                () -> produtoService.atualizar(1L, request));

        assertEquals("Refrigerante", produto.getNome());
        assertEquals(
                new BigDecimal("6.00"),
                produto.getPreco());

        verify(produtoRepository).findById(1L);
        verify(categoriaRepository).findById(99L);
    }
}

package br.com.comandavision.api.categoria;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.comandavision.api.categoria.dto.AtualizarCategoriaRequest;
import br.com.comandavision.api.categoria.dto.CategoriaResponse;
import br.com.comandavision.api.categoria.dto.CriarCategoriaRequest;

@ExtendWith(MockitoExtension.class)
public class CategoriaServiceTest {
    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    @Test
    public void deveCriarCategoria() {
        CriarCategoriaRequest request = new CriarCategoriaRequest(
                "Bebidas",
                "Refrigerantes, sucos e águas");

        when(categoriaRepository.save(any(Categoria.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        CategoriaResponse resposta = categoriaService.criar(request);

        assertEquals("Bebidas", resposta.nome());
        assertEquals(
                "Refrigerantes, sucos e águas",
                resposta.descricao());
        assertTrue(resposta.ativa());

        verify(categoriaRepository).save(any(Categoria.class));
    }

    @Test
    public void deveListarCategorias() {
        Categoria bebidas = new Categoria(
                "Bebidas",
                "Refrigerantes, sucos e águas");

        Categoria porcoes = new Categoria(
                "Porções",
                "Porções e acompanhamentos");

        when(categoriaRepository.findAll())
                .thenReturn(List.of(bebidas, porcoes));

        List<CategoriaResponse> resposta = categoriaService.listar();

        assertEquals(2, resposta.size());

        assertEquals("Bebidas", resposta.get(0).nome());
        assertEquals(
                "Refrigerantes, sucos e águas",
                resposta.get(0).descricao());

        assertEquals("Porções", resposta.get(1).nome());
        assertEquals(
                "Porções e acompanhamentos",
                resposta.get(1).descricao());

        verify(categoriaRepository).findAll();
    }

    @Test
    public void deveBuscarCategoriaPorId() {
        Categoria categoria = new Categoria(
                "Bebidas",
                "Refrigerantes, sucos e águas");

        when(categoriaRepository.findById(1L))
                .thenReturn(Optional.of(categoria));

        CategoriaResponse resposta = categoriaService.buscarPorId(1L);

        assertEquals("Bebidas", resposta.nome());
        assertEquals(
                "Refrigerantes, sucos e águas",
                resposta.descricao());
        assertTrue(resposta.ativa());

        verify(categoriaRepository).findById(1L);
    }

    @Test
    public void deveLancarExcecaoQuandoCategoriaNaoForEncontrada() {
        when(categoriaRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                CategoriaNaoEncontradaException.class,
                () -> categoriaService.buscarPorId(99L));

        verify(categoriaRepository).findById(99L);
    }

    @Test
    public void deveAtualizarCategoria() {
        Categoria categoria = new Categoria(
                "Bebidas",
                "Refrigerantes, sucos e águas");

        AtualizarCategoriaRequest request = new AtualizarCategoriaRequest(
                "Bebidas não alcoólicas",
                "Sucos, águas e refrigerantes",
                false);

        when(categoriaRepository.findById(1L))
                .thenReturn(Optional.of(categoria));

        CategoriaResponse resposta = categoriaService.atualizar(1L, request);

        assertEquals(
                "Bebidas não alcoólicas",
                resposta.nome());
        assertEquals(
                "Sucos, águas e refrigerantes",
                resposta.descricao());
        assertFalse(resposta.ativa());

        verify(categoriaRepository).findById(1L);
    }

    @Test
    public void deveLancarExcecaoAoAtualizarCategoriaInexistente() {
        AtualizarCategoriaRequest request = new AtualizarCategoriaRequest(
                "Bebidas",
                "Sucos e refrigerantes",
                true);

        when(categoriaRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                CategoriaNaoEncontradaException.class,
                () -> categoriaService.atualizar(99L, request));

        verify(categoriaRepository).findById(99L);
    }
}

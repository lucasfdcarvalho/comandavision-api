package br.com.comandavision.api.comanda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.comandavision.api.categoria.Categoria;
import br.com.comandavision.api.comanda.dto.AdicionarItemComandaRequest;
import br.com.comandavision.api.comanda.dto.AtualizarItemComandaRequest;
import br.com.comandavision.api.comanda.dto.ComandaDetalhadaResponse;
import br.com.comandavision.api.produto.Produto;
import br.com.comandavision.api.produto.ProdutoRepository;

@ExtendWith(MockitoExtension.class)
public class ComandaServiceTest {
    @Mock
    private ComandaRepository comandaRepository;

    @Mock
    private ItemComandaRepository itemComandaRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @InjectMocks
    private ComandaService comandaService;

    @Test
    public void naoDeveFecharComandaSemItens() {
        Long comandaId = 10L;
        Comanda comanda = new Comanda("Mesa 10", null);

        when(comandaRepository.findById(comandaId))
                .thenReturn(Optional.of(comanda));

        when(itemComandaRepository.findByComandaIdOrderByCriadoEmAsc(comandaId))
                .thenReturn(List.of());

        assertThrows(ComandaSemItensException.class, () -> comandaService.fechar(comandaId));
    }

    @Test
    void naoDeveAdicionarItemEmComandaFechada() {
        Long comandaId = 10L;

        Comanda comanda = new Comanda("Mesa 10", null);
        comanda.fechar();

        AdicionarItemComandaRequest request = new AdicionarItemComandaRequest(
                5L,
                2,
                null);

        when(comandaRepository.findById(comandaId))
                .thenReturn(Optional.of(comanda));

        assertThrows(
                ComandaNaoEstaAbertaException.class,
                () -> comandaService.adicionarItem(
                        comandaId,
                        request));

        verifyNoInteractions(
                produtoRepository,
                itemComandaRepository);
    }

    @Test
    void naoDeveAtualizarItemQueNaoPertenceAComanda() {
        Long comandaId = 10L;
        Long itemId = 50L;

        Comanda comanda = new Comanda("Mesa 10", null);

        AtualizarItemComandaRequest request = new AtualizarItemComandaRequest(
                3,
                "Observação alterada");

        when(comandaRepository.findById(comandaId))
                .thenReturn(Optional.of(comanda));

        when(itemComandaRepository
                .findByIdAndComandaId(itemId, comandaId))
                .thenReturn(Optional.empty());

        assertThrows(
                ItemComandaNaoEncontradoException.class,
                () -> comandaService.atualizarItem(
                        comandaId,
                        itemId,
                        request));
    }

    @Test
    void deveFecharComandaComItens() {
        Long comandaId = 10L;

        Comanda comanda = new Comanda("Mesa 10", null);

        Categoria categoria = new Categoria("Bebidas", null);

        Produto produto = new Produto(
                categoria,
                "Coca-Cola",
                null,
                new BigDecimal("6.00"));

        ItemComanda item = new ItemComanda(
                comanda,
                produto,
                2,
                null);

        when(comandaRepository.findById(comandaId))
                .thenReturn(Optional.of(comanda));

        when(itemComandaRepository
                .findByComandaIdOrderByCriadoEmAsc(comandaId))
                .thenReturn(List.of(item));

        ComandaDetalhadaResponse resposta = comandaService.fechar(comandaId);

        assertEquals(
                StatusComanda.FECHADA,
                resposta.status());

        assertNotNull(resposta.fechadaEm());

        assertEquals(
                new BigDecimal("12.00"),
                resposta.total());

        assertEquals(1, resposta.itens().size());
    }
}

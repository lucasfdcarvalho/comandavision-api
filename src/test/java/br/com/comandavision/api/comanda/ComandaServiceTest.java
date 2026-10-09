package br.com.comandavision.api.comanda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;
import java.sql.SQLException;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.comandavision.api.categoria.Categoria;
import br.com.comandavision.api.categoria.CategoriaInativaException;
import br.com.comandavision.api.comanda.dto.AdicionarItemComandaRequest;
import br.com.comandavision.api.comanda.dto.AtualizarItemComandaRequest;
import br.com.comandavision.api.comanda.dto.ComandaDetalhadaResponse;
import br.com.comandavision.api.comanda.dto.CriarComandaRequest;
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
    void deveAbrirComandaComIdentificacaoNovaSemEspacosNasExtremidades() {
        when(comandaRepository.saveAndFlush(any(Comanda.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = comandaService.abrir(new CriarComandaRequest("  Mesa 01  ", "Aniversário"));

        assertEquals("Mesa 01", resposta.identificacao());
        assertEquals("Aniversário", resposta.observacao());
        assertEquals(StatusComanda.ABERTA, resposta.status());
        verify(comandaRepository).existeAbertaComIdentificacao("Mesa 01");
    }

    @Test
    void deveRecusarIdentificacaoJaAbertaAntesDeSalvar() {
        when(comandaRepository.existeAbertaComIdentificacao("Mesa 01")).thenReturn(true);

        var erro = assertThrows(ComandaIdentificacaoDuplicadaException.class,
                () -> comandaService.abrir(new CriarComandaRequest("Mesa 01", null)));

        assertEquals("Já existe uma comanda aberta com essa descrição.", erro.getMessage());
        verify(comandaRepository, never()).saveAndFlush(any(Comanda.class));
    }

    @Test
    void deveTraduzirConflitoDoIndiceQuandoOutraPessoaAbreAoMesmoTempo() {
        var causa = new ConstraintViolationException("duplicada", new SQLException("duplicada", "23505"),
                ComandaIdentificacaoDuplicadaException.INDICE_UNICO);
        when(comandaRepository.saveAndFlush(any(Comanda.class)))
                .thenThrow(new DataIntegrityViolationException("duplicada", causa));

        var erro = assertThrows(ComandaIdentificacaoDuplicadaException.class,
                () -> comandaService.abrir(new CriarComandaRequest("Mesa 01", null)));

        assertEquals("Já existe uma comanda aberta com essa descrição.", erro.getMessage());
    }

    @Test
    void naoDeveConfundirOutrasViolacoesDeIntegridadeComIdentificacaoDuplicada() {
        var causa = new ConstraintViolationException("outra regra", new SQLException("outra regra", "23514"),
                "ck_comandas_status");
        var erroEsperado = new DataIntegrityViolationException("outra regra", causa);
        when(comandaRepository.saveAndFlush(any(Comanda.class))).thenThrow(erroEsperado);

        assertEquals(erroEsperado, assertThrows(DataIntegrityViolationException.class,
                () -> comandaService.abrir(new CriarComandaRequest("Mesa 01", null))));
    }

    @Test
    void deveReconhecerRestricaoPelosDadosEstruturadosMesmoComMensagemLocalizada() {
        var erroServidor = new ServerErrorMessage("SERROR\0C23505\0nuk_comandas_abertas_identificacao\0MIdentificação duplicada\0\0");
        var causa = new PSQLException(erroServidor);
        when(comandaRepository.saveAndFlush(any(Comanda.class)))
                .thenThrow(new DataIntegrityViolationException("falha", causa));

        assertThrows(ComandaIdentificacaoDuplicadaException.class,
                () -> comandaService.abrir(new CriarComandaRequest("Mesa 01", null)));
    }

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

    @Test
    void naoDeveAdicionarItemDeCategoriaInativa() {
        Long comandaId = 10L;
        Comanda comanda = new Comanda("Mesa 10", null);

        Categoria categoria = new Categoria(
                "Sazonais",
                "Produtos de temporada");
        categoria.setAtiva(false);

        Produto produto = new Produto(
                categoria,
                "Quentão",
                "Copo 300 ml",
                new BigDecimal("12.00"));

        AdicionarItemComandaRequest request = new AdicionarItemComandaRequest(
                5L,
                1,
                null);

        when(comandaRepository.findById(comandaId))
                .thenReturn(Optional.of(comanda));

        when(produtoRepository.findById(5L))
                .thenReturn(Optional.of(produto));

        assertThrows(
                CategoriaInativaException.class,
                () -> comandaService.adicionarItem(comandaId, request));

        verifyNoInteractions(itemComandaRepository);
    }
}

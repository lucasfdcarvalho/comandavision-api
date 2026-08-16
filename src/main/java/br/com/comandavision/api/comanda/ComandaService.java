package br.com.comandavision.api.comanda;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;

import br.com.comandavision.api.comanda.dto.AdicionarItemComandaRequest;
import br.com.comandavision.api.comanda.dto.ComandaResponse;
import br.com.comandavision.api.comanda.dto.CriarComandaRequest;
import br.com.comandavision.api.comanda.dto.ItemComandaResponse;
import br.com.comandavision.api.comanda.dto.ComandaDetalhadaResponse;
import br.com.comandavision.api.comanda.dto.AtualizarItemComandaRequest;
import br.com.comandavision.api.produto.Produto;
import br.com.comandavision.api.produto.ProdutoInativoException;
import br.com.comandavision.api.produto.ProdutoNaoEncontradoException;
import br.com.comandavision.api.produto.ProdutoRepository;

import org.springframework.transaction.annotation.Transactional;

@Service
public class ComandaService {
    private final ComandaRepository comandaRepository;
    private final ItemComandaRepository itemComandaRepository;
    private final ProdutoRepository produtoRepository;

    public ComandaService(ComandaRepository comandaRepository, ItemComandaRepository itemComandaRepository,
            ProdutoRepository produtoRepository) {
        this.comandaRepository = comandaRepository;
        this.itemComandaRepository = itemComandaRepository;
        this.produtoRepository = produtoRepository;
    }

    @Transactional
    public ComandaResponse abrir(CriarComandaRequest request) {
        Comanda comanda = new Comanda(request.identificacao(), request.observacao());

        Comanda comandaSalva = this.comandaRepository.save(comanda);

        return ComandaResponse.from(comandaSalva);
    }

    @Transactional(readOnly = true)
    public List<ComandaResponse> listar() {
        return this.comandaRepository.findAll(Sort.by(Sort.Direction.DESC, "abertaEm")).stream()
                .map(ComandaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ComandaDetalhadaResponse buscarPorId(Long id) {
        Comanda comanda = comandaRepository.findById(id)
                .orElseThrow(() -> new ComandaNaoEncontradaException(id));

        List<ItemComanda> itens = itemComandaRepository.findByComandaIdOrderByCriadoEmAsc(id);

        return ComandaDetalhadaResponse.from(comanda, itens);
    }

    @Transactional
    public ItemComandaResponse adicionarItem(
            Long comandaId,
            AdicionarItemComandaRequest request) {

        Comanda comanda = comandaRepository.findById(comandaId)
                .orElseThrow(() -> new ComandaNaoEncontradaException(comandaId));

        if (!comanda.estaAberta()) {
            throw new ComandaNaoEstaAbertaException(
                    comanda.getId(),
                    comanda.getStatus());
        }

        Produto produto = produtoRepository.findById(request.produtoId())
                .orElseThrow(() -> new ProdutoNaoEncontradoException(request.produtoId()));

        if (!produto.isAtivo()) {
            throw new ProdutoInativoException(produto.getId());
        }

        ItemComanda item = new ItemComanda(
                comanda,
                produto,
                request.quantidade(),
                request.observacao());

        ItemComanda itemSalvo = itemComandaRepository.save(item);

        return ItemComandaResponse.from(itemSalvo);
    }

    @Transactional
    public ItemComandaResponse atualizarItem(
            Long comandaId,
            Long itemId,
            AtualizarItemComandaRequest request) {

        Comanda comanda = comandaRepository.findById(comandaId)
                .orElseThrow(() -> new ComandaNaoEncontradaException(comandaId));

        if (!comanda.estaAberta()) {
            throw new ComandaNaoEstaAbertaException(
                    comanda.getId(),
                    comanda.getStatus());
        }

        ItemComanda item = itemComandaRepository
                .findByIdAndComandaId(itemId, comandaId)
                .orElseThrow(() -> new ItemComandaNaoEncontradoException(
                        itemId,
                        comandaId));

        item.setQuantidade(request.quantidade());
        item.setObservacao(request.observacao());

        return ItemComandaResponse.from(item);
    }

    @Transactional
    public void removerItem(Long comandaId, Long itemId) {
        Comanda comanda = comandaRepository.findById(comandaId)
                .orElseThrow(() -> new ComandaNaoEncontradaException(comandaId));

        if (!comanda.estaAberta()) {
            throw new ComandaNaoEstaAbertaException(
                    comanda.getId(),
                    comanda.getStatus());
        }

        ItemComanda item = itemComandaRepository
                .findByIdAndComandaId(itemId, comandaId)
                .orElseThrow(() -> new ItemComandaNaoEncontradoException(
                        itemId,
                        comandaId));

        itemComandaRepository.delete(item);
    }

    @Transactional
    public ComandaDetalhadaResponse fechar(Long id) {
        Comanda comanda = comandaRepository.findById(id)
                .orElseThrow(() -> new ComandaNaoEncontradaException(id));

        if (!comanda.estaAberta()) {
            throw new ComandaNaoEstaAbertaException(
                    comanda.getId(),
                    comanda.getStatus());
        }

        List<ItemComanda> itens = itemComandaRepository.findByComandaIdOrderByCriadoEmAsc(id);

        if (itens.isEmpty()) {
            throw new ComandaSemItensException(id);
        }

        comanda.fechar();

        return ComandaDetalhadaResponse.from(comanda, itens);
    }

    @Transactional
    public ComandaDetalhadaResponse cancelar(Long id) {
        Comanda comanda = comandaRepository.findById(id)
                .orElseThrow(() -> new ComandaNaoEncontradaException(id));

        if (!comanda.estaAberta()) {
            throw new ComandaNaoEstaAbertaException(
                    comanda.getId(),
                    comanda.getStatus());
        }

        List<ItemComanda> itens = itemComandaRepository.findByComandaIdOrderByCriadoEmAsc(id);

        comanda.cancelar();

        return ComandaDetalhadaResponse.from(comanda, itens);
    }
}

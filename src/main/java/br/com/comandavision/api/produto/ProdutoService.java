package br.com.comandavision.api.produto;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.comandavision.api.categoria.Categoria;
import br.com.comandavision.api.categoria.CategoriaNaoEncontradaException;
import br.com.comandavision.api.categoria.CategoriaRepository;
import br.com.comandavision.api.produto.dto.AtualizarProdutoRequest;
import br.com.comandavision.api.produto.dto.CriarProdutoRequest;
import br.com.comandavision.api.produto.dto.ProdutoResponse;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;

    public ProdutoService(ProdutoRepository produtoRepository, CategoriaRepository categoriaRepository) {
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional
    public ProdutoResponse criar(CriarProdutoRequest request) {
        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new CategoriaNaoEncontradaException(request.categoriaId()));

        Produto produto = new Produto(
                categoria,
                request.nome(),
                request.descricao(),
                request.preco());

        Produto produtoSalvo = produtoRepository.save(produto);

        return ProdutoResponse.from(produtoSalvo);
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponse> listar() {
        return produtoRepository.findAll(Sort.by(Sort.Direction.ASC, "nome"))
                .stream()
                .map(ProdutoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscarPorId(Long id) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(id));

        return ProdutoResponse.from(produto);
    }

    @Transactional
    public ProdutoResponse atualizar(Long id, AtualizarProdutoRequest request) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(id));

        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new CategoriaNaoEncontradaException(request.categoriaId()));

        produto.setNome(request.nome());
        produto.setDescricao(request.descricao());
        produto.setPreco(request.preco());
        produto.setCategoria(categoria);
        produto.setAtivo(request.ativo());

        return ProdutoResponse.from(produto);
    }
}

package br.com.comandavision.api.categoria;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.comandavision.api.categoria.dto.CategoriaResponse;
import br.com.comandavision.api.categoria.dto.CriarCategoriaRequest;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional
    public CategoriaResponse criar(CriarCategoriaRequest request) {
        Categoria categoria = new Categoria(request.nome(), request.descricao());

        Categoria categoriaSalva = categoriaRepository.save(categoria);

        return CategoriaResponse.from(categoriaSalva);
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAll().stream()
                .map(CategoriaResponse::from)
                .toList();
    }
}

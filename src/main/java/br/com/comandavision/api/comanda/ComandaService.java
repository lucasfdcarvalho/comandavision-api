package br.com.comandavision.api.comanda;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;

import br.com.comandavision.api.comanda.dto.ComandaResponse;
import br.com.comandavision.api.comanda.dto.CriarComandaRequest;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComandaService {
    private final ComandaRepository comandaRepository;

    public ComandaService(ComandaRepository comandaRepository) {
        this.comandaRepository = comandaRepository;
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
    public ComandaResponse buscarPorId(Long id) {
        Comanda comanda = this.comandaRepository.findById(id).orElseThrow(() -> new ComandaNaoEncontradaException(id));
        return ComandaResponse.from(comanda);
    }
}

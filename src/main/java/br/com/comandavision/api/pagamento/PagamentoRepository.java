package br.com.comandavision.api.pagamento;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    List<Pagamento> findByComandaIdOrderByCriadoEmAsc(Long comandaId);

    Optional<Pagamento> findByIdAndComandaId(Long pagamentoId, Long comandaId);
}

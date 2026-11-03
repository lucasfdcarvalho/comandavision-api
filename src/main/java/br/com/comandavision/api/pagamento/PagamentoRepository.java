package br.com.comandavision.api.pagamento;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    List<Pagamento> findByComandaIdOrderByCriadoEmAsc(Long comandaId);

    Optional<Pagamento> findByIdAndComandaId(Long pagamentoId, Long comandaId);

    Optional<Pagamento> findByChaveIdempotencia(String chaveIdempotencia);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Pagamento> findWithLockById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Pagamento> findWithLockByIdAndComandaId(Long id, Long comandaId);
}

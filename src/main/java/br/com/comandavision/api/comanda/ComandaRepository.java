package br.com.comandavision.api.comanda;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface ComandaRepository extends JpaRepository<Comanda, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select comanda from Comanda comanda where comanda.id = :id")
    Optional<Comanda> findByIdForUpdate(@Param("id") Long id);

    // Usa a mesma expressão do índice único, incluindo registros anteriores à migração.
    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM comandavision.comandas
                WHERE status = 'ABERTA'
                  AND lower(btrim(identificacao)) = lower(btrim(:identificacao))
            )
            """, nativeQuery = true)
    boolean existeAbertaComIdentificacao(@Param("identificacao") String identificacao);
}

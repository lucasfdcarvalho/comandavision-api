package br.com.comandavision.api.comanda;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemComandaRepository extends JpaRepository<ItemComanda, Long> {
    List<ItemComanda> findByComandaIdOrderByCriadoEmAsc(Long comandaId);

    Optional<ItemComanda> findByIdAndComandaId(Long itemId, Long comandaId);
}

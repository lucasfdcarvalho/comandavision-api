package br.com.comandavision.api.comanda;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemComandaRepository extends JpaRepository<ItemComanda, Long> {
    List<ItemComanda> findByComandaIdOrderByCriadoEmAsc(Long comandaId);
}

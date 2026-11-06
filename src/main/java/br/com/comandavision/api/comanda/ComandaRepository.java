package br.com.comandavision.api.comanda;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
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

    @Query(value = """
            WITH totais_itens AS (
                SELECT item.comanda_id,
                       SUM(item.quantidade * item.preco_unitario) AS total
                FROM comandavision.itens_comanda item
                GROUP BY item.comanda_id
            ),
            totais_pagamentos AS (
                SELECT pagamento.comanda_id,
                       SUM(pagamento.valor) AS total_pago
                FROM comandavision.pagamentos pagamento
                WHERE pagamento.status = 'CONFIRMADO'
                GROUP BY pagamento.comanda_id
            )
            SELECT comanda.id AS id,
                   comanda.identificacao AS identificacao,
                   comanda.fechada_em AS fechadaEm,
                   itens.total AS total,
                   COALESCE(pagamentos.total_pago, 0) AS totalPago,
                   itens.total - COALESCE(pagamentos.total_pago, 0) AS saldoRestante,
                   CASE
                       WHEN COALESCE(pagamentos.total_pago, 0) > 0 THEN 'PARCIAL'
                       ELSE 'NAO_PAGO'
                   END AS situacaoPagamento
            FROM comandavision.comandas comanda
            JOIN totais_itens itens ON itens.comanda_id = comanda.id
            LEFT JOIN totais_pagamentos pagamentos ON pagamentos.comanda_id = comanda.id
            WHERE comanda.status = 'FECHADA'
              AND itens.total - COALESCE(pagamentos.total_pago, 0) > 0
            ORDER BY comanda.fechada_em DESC NULLS LAST, comanda.id DESC
            """, nativeQuery = true)
    List<ComandaAReceberProjection> listarAReceber();
}

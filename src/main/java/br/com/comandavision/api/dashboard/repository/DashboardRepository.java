package br.com.comandavision.api.dashboard.repository;

import java.time.OffsetDateTime;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import br.com.comandavision.api.comanda.Comanda;
import br.com.comandavision.api.dashboard.projection.FaturamentoDiarioProjection;
import br.com.comandavision.api.dashboard.projection.FormaPagamentoResumoProjection;
import br.com.comandavision.api.dashboard.projection.ProdutoMaisVendidoProjection;
import br.com.comandavision.api.dashboard.projection.ResumoDashboardProjection;

public interface DashboardRepository extends Repository<Comanda, Long> {
    @Query(value = """
            WITH totais_comanda AS (
                SELECT
                    c.id AS comanda_id,
                    SUM(ic.preco_unitario * ic.quantidade) AS total_comanda,
                    SUM(ic.quantidade) AS quantidade_itens
                FROM comandavision.comandas c
                INNER JOIN comandavision.itens_comanda ic
                    ON ic.comanda_id = c.id
                WHERE c.status = 'FECHADA'
                  AND c.fechada_em >= :inicio
                  AND c.fechada_em < :fimExclusivo
                GROUP BY c.id
            ),
            pagamentos_confirmados AS (
                SELECT
                    p.comanda_id,
                    SUM(p.valor) AS total_pago
                FROM comandavision.pagamentos p
                WHERE p.status = 'CONFIRMADO'
                GROUP BY p.comanda_id
            ),
            vendas AS (
                SELECT
                    tc.total_comanda,
                    tc.quantidade_itens
                FROM totais_comanda tc
                INNER JOIN pagamentos_confirmados pc
                    ON pc.comanda_id = tc.comanda_id
                WHERE pc.total_pago >= tc.total_comanda
            )
            SELECT
                COALESCE(SUM(v.total_comanda), 0) AS "faturamento",
                COUNT(*) AS "quantidadeVendas",
                CASE
                    WHEN COUNT(*) = 0 THEN 0
                    ELSE ROUND(SUM(v.total_comanda) / COUNT(*), 2)
                END AS "ticketMedio",
                COALESCE(SUM(v.quantidade_itens), 0) AS "quantidadeItensVendidos"
            FROM vendas v
            """, nativeQuery = true)
    ResumoDashboardProjection buscarResumo(
            @Param("inicio") OffsetDateTime inicio,
            @Param("fimExclusivo") OffsetDateTime fimExclusivo);

    @Query(value = """
            WITH totais_comanda AS (
                SELECT
                    c.id AS comanda_id,
                    SUM(ic.preco_unitario * ic.quantidade) AS total_comanda
                FROM comandavision.comandas c
                INNER JOIN comandavision.itens_comanda ic
                    ON ic.comanda_id = c.id
                WHERE c.status = 'FECHADA'
                  AND c.fechada_em >= :inicio
                  AND c.fechada_em < :fimExclusivo
                GROUP BY c.id
            ),
            pagamentos_confirmados AS (
                SELECT
                    p.comanda_id,
                    SUM(p.valor) AS total_pago
                FROM comandavision.pagamentos p
                WHERE p.status = 'CONFIRMADO'
                GROUP BY p.comanda_id
            ),
            vendas_validas AS (
                SELECT tc.comanda_id
                FROM totais_comanda tc
                INNER JOIN pagamentos_confirmados pc
                    ON pc.comanda_id = tc.comanda_id
                WHERE pc.total_pago >= tc.total_comanda
            )
            SELECT
                p.id AS "produtoId",
                p.nome AS "produtoNome",
                SUM(ic.quantidade) AS "quantidadeVendida",
                SUM(ic.preco_unitario * ic.quantidade) AS "faturamento"
            FROM vendas_validas vv
            INNER JOIN comandavision.itens_comanda ic
                ON ic.comanda_id = vv.comanda_id
            INNER JOIN comandavision.produtos p
                ON p.id = ic.produto_id
            GROUP BY p.id, p.nome
            ORDER BY
                SUM(ic.quantidade) DESC,
                SUM(ic.preco_unitario * ic.quantidade) DESC,
                p.nome ASC
            LIMIT :limite
            """, nativeQuery = true)
    List<ProdutoMaisVendidoProjection> buscarProdutosMaisVendidos(
            @Param("inicio") OffsetDateTime inicio,
            @Param("fimExclusivo") OffsetDateTime fimExclusivo,
            @Param("limite") int limite);

    @Query(value = """
            WITH totais_comanda AS (
                SELECT
                    c.id AS comanda_id,
                    SUM(ic.preco_unitario * ic.quantidade) AS total_comanda
                FROM comandavision.comandas c
                INNER JOIN comandavision.itens_comanda ic
                    ON ic.comanda_id = c.id
                WHERE c.status = 'FECHADA'
                  AND c.fechada_em >= :inicio
                  AND c.fechada_em < :fimExclusivo
                GROUP BY c.id
            ),
            pagamentos_confirmados AS (
                SELECT
                    p.comanda_id,
                    SUM(p.valor) AS total_pago
                FROM comandavision.pagamentos p
                WHERE p.status = 'CONFIRMADO'
                GROUP BY p.comanda_id
            ),
            vendas_validas AS (
                SELECT tc.comanda_id
                FROM totais_comanda tc
                INNER JOIN pagamentos_confirmados pc
                    ON pc.comanda_id = tc.comanda_id
                WHERE pc.total_pago >= tc.total_comanda
            )
            SELECT
                p.forma AS "forma",
                COUNT(p.id) AS "quantidadePagamentos",
                SUM(p.valor) AS "valorRecebido"
            FROM vendas_validas vv
            INNER JOIN comandavision.pagamentos p
                ON p.comanda_id = vv.comanda_id
               AND p.status = 'CONFIRMADO'
            GROUP BY p.forma
            ORDER BY
                SUM(p.valor) DESC,
                p.forma ASC
            """, nativeQuery = true)
    List<FormaPagamentoResumoProjection> buscarResumoPorFormaPagamento(
            @Param("inicio") OffsetDateTime inicio,
            @Param("fimExclusivo") OffsetDateTime fimExclusivo);

    @Query(value = """
            WITH dias AS (
                SELECT CAST(
                    generate_series(
                        CAST(:inicio AS DATE),
                        CAST(:fimExclusivo AS DATE) - INTERVAL '1 day',
                        INTERVAL '1 day'
                    ) AS DATE
                ) AS data
            ),
            totais_comanda AS (
                SELECT
                    c.id AS comanda_id,
                    CAST(
                        c.fechada_em AT TIME ZONE 'America/Sao_Paulo'
                        AS DATE
                    ) AS data,
                    SUM(ic.preco_unitario * ic.quantidade) AS total_comanda
                FROM comandavision.comandas c
                INNER JOIN comandavision.itens_comanda ic
                    ON ic.comanda_id = c.id
                WHERE c.status = 'FECHADA'
                  AND c.fechada_em >= :inicio
                  AND c.fechada_em < :fimExclusivo
                GROUP BY c.id, c.fechada_em
            ),
            pagamentos_confirmados AS (
                SELECT
                    p.comanda_id,
                    SUM(p.valor) AS total_pago
                FROM comandavision.pagamentos p
                WHERE p.status = 'CONFIRMADO'
                GROUP BY p.comanda_id
            ),
            vendas_validas AS (
                SELECT
                    tc.comanda_id,
                    tc.data,
                    tc.total_comanda
                FROM totais_comanda tc
                INNER JOIN pagamentos_confirmados pc
                    ON pc.comanda_id = tc.comanda_id
                WHERE pc.total_pago >= tc.total_comanda
            )
            SELECT
                d.data AS "data",
                COALESCE(SUM(vv.total_comanda), 0) AS "faturamento",
                COUNT(vv.comanda_id) AS "quantidadeVendas"
            FROM dias d
            LEFT JOIN vendas_validas vv
                ON vv.data = d.data
            GROUP BY d.data
            ORDER BY d.data ASC
            """, nativeQuery = true)
    List<FaturamentoDiarioProjection> buscarFaturamentoDiario(
            @Param("inicio") OffsetDateTime inicio,
            @Param("fimExclusivo") OffsetDateTime fimExclusivo);
}

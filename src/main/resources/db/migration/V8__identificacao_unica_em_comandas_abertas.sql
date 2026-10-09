-- Flyway executa esta migração em transação. O bloqueio impede novas escritas
-- entre o diagnóstico de duplicidades e a criação do índice; leituras continuam.
LOCK TABLE comandavision.comandas IN SHARE ROW EXCLUSIVE MODE;

DO $$
DECLARE
    duplicidades TEXT;
BEGIN
    SELECT string_agg(
        format('%s (IDs: %s)', identificacao_normalizada, ids),
        '; ' ORDER BY identificacao_normalizada
    ) INTO duplicidades
    FROM (
        SELECT lower(btrim(identificacao)) AS identificacao_normalizada,
               array_agg(id ORDER BY id) AS ids
        FROM comandavision.comandas
        WHERE status = 'ABERTA'
        GROUP BY lower(btrim(identificacao))
        HAVING count(*) > 1
    ) AS conflitos;

    IF duplicidades IS NOT NULL THEN
        RAISE EXCEPTION USING
            ERRCODE = '23505',
            MESSAGE = 'Existem comandas abertas com a mesma identificação. Nenhum registro foi alterado.',
            DETAIL = duplicidades,
            HINT = 'Revise os IDs com a consulta db/diagnostico/comandas_abertas_duplicadas.sql antes de aplicar a V8.';
    END IF;
END $$;

CREATE UNIQUE INDEX uk_comandas_abertas_identificacao
    ON comandavision.comandas (lower(btrim(identificacao)))
    WHERE status = 'ABERTA';

-- Diagnóstico somente de leitura; não altera nem remove comandas.
SELECT lower(btrim(identificacao)) AS identificacao_normalizada,
       count(*) AS quantidade,
       array_agg(id ORDER BY id) AS ids,
       array_agg(identificacao ORDER BY id) AS identificacoes_originais
FROM comandavision.comandas
WHERE status = 'ABERTA'
GROUP BY lower(btrim(identificacao))
HAVING count(*) > 1
ORDER BY identificacao_normalizada;

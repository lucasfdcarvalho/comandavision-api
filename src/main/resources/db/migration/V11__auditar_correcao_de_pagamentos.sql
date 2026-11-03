-- A correção anula apenas um registro manual incorreto. Não representa devolução de dinheiro.
ALTER TABLE comandavision.pagamentos
    ADD COLUMN motivo_correcao VARCHAR(500),
    ADD COLUMN corrigido_por UUID,
    ADD COLUMN corrigido_em TIMESTAMP WITH TIME ZONE;

ALTER TABLE comandavision.pagamentos
    ADD CONSTRAINT ck_pagamentos_auditoria_correcao
    CHECK (
        (motivo_correcao IS NULL AND corrigido_por IS NULL AND corrigido_em IS NULL)
        OR
        (status = 'ESTORNADO'
            AND motivo_correcao IS NOT NULL AND btrim(motivo_correcao) <> ''
            AND corrigido_por IS NOT NULL AND corrigido_em IS NOT NULL)
    );

-- A chave identifica uma intenção de pagamento criada pelo aplicativo.
-- Registros antigos permanecem válidos com NULL; novas requisições enviam UUID.
ALTER TABLE comandavision.pagamentos
    ADD COLUMN chave_idempotencia VARCHAR(128);

CREATE UNIQUE INDEX uk_pagamentos_chave_idempotencia
    ON comandavision.pagamentos (chave_idempotencia)
    WHERE chave_idempotencia IS NOT NULL;

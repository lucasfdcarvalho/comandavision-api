CREATE TYPE comandavision.papel_usuario AS ENUM (
    'DONO',
    'FUNCIONARIO'
);

CREATE TABLE comandavision.usuarios_papeis (
    usuario_id UUID NOT NULL,
    papel comandavision.papel_usuario NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_usuarios_papeis
        PRIMARY KEY (usuario_id),

    CONSTRAINT fk_usuarios_papeis_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES auth.users (id)
        ON DELETE CASCADE
);

ALTER TABLE comandavision.usuarios_papeis
    ENABLE ROW LEVEL SECURITY;

REVOKE ALL
    ON TABLE comandavision.usuarios_papeis
    FROM anon, authenticated, PUBLIC;
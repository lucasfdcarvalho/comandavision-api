GRANT USAGE
    ON SCHEMA comandavision
    TO supabase_auth_admin;

GRANT SELECT
    ON TABLE comandavision.usuarios_papeis
    TO supabase_auth_admin;

CREATE POLICY permitir_leitura_papeis_auth_admin
    ON comandavision.usuarios_papeis
    FOR SELECT
    TO supabase_auth_admin
    USING (true);

CREATE OR REPLACE FUNCTION public.custom_access_token_hook(event JSONB)
RETURNS JSONB
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    claims JSONB;
    papel_usuario TEXT;
BEGIN
    SELECT up.papel::TEXT
    INTO papel_usuario
    FROM comandavision.usuarios_papeis up
    WHERE up.usuario_id = (event ->> 'user_id')::UUID;

    claims := event -> 'claims';

    IF papel_usuario IS NOT NULL THEN
        claims := jsonb_set(
            claims,
            '{user_role}',
            to_jsonb(papel_usuario)
        );
    ELSE
        claims := jsonb_set(
            claims,
            '{user_role}',
            'null'::JSONB
        );
    END IF;

    RETURN jsonb_set(
        event,
        '{claims}',
        claims
    );
END;
$$;

REVOKE EXECUTE
    ON FUNCTION public.custom_access_token_hook(JSONB)
    FROM PUBLIC, anon, authenticated;

GRANT EXECUTE
    ON FUNCTION public.custom_access_token_hook(JSONB)
    TO supabase_auth_admin;
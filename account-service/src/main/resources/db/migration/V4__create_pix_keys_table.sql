-- ============================================================
-- V4 — Chaves PIX por conta
-- ============================================================

CREATE TABLE IF NOT EXISTS pix_keys (
    id          BIGSERIAL PRIMARY KEY,
    key_type    VARCHAR(20)  NOT NULL,
    key_value   VARCHAR(255) NOT NULL,
    account_id  BIGINT       NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    owner_email VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),

    -- Uma chave PIX é única globalmente: o destinatário é resolvido apenas pelo valor
    CONSTRAINT pix_keys_value_unique UNIQUE (key_value),
    CONSTRAINT pix_keys_type_check CHECK (key_type IN ('CPF', 'EMAIL', 'PHONE', 'RANDOM'))
);

CREATE INDEX IF NOT EXISTS idx_pix_keys_key_value   ON pix_keys (key_value);
CREATE INDEX IF NOT EXISTS idx_pix_keys_account_id  ON pix_keys (account_id);
CREATE INDEX IF NOT EXISTS idx_pix_keys_owner_email ON pix_keys (owner_email);
CREATE INDEX IF NOT EXISTS idx_pix_keys_type        ON pix_keys (key_type);

COMMENT ON TABLE  pix_keys            IS 'Chaves PIX associadas às contas dos usuários';
COMMENT ON COLUMN pix_keys.key_value  IS 'Valor normalizado da chave (CPF/telefone apenas dígitos, e-mail minúsculo, UUID minúsculo)';
COMMENT ON COLUMN pix_keys.key_type   IS 'CPF, EMAIL, PHONE ou RANDOM';

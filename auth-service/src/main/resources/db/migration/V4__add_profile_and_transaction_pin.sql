-- ============================================================
-- V4 — Perfil do usuário e PIN de transação
-- ============================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS phone            VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS transaction_pin  VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_users_phone ON users (phone);

COMMENT ON COLUMN users.phone           IS 'Telefone do usuário (apenas dígitos)';
COMMENT ON COLUMN users.transaction_pin IS 'Hash BCrypt do PIN de transação (4 dígitos) — nunca armazenar em texto puro';

-- Adiciona coluna de versão para optimistic locking (JPA @Version)
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- ============================================================
-- FinCore Banking — Permissões por banco de dados
--
-- Estratégia: cada usuário de serviço é OWNER do schema public
-- do seu banco. Isso garante que:
--   - Flyway cria/gerencia flyway_schema_history sem erros
--   - Tabelas criadas por migrations pertencem ao serviço
--   - Não há "permission denied" em reinicializações
-- ============================================================

-- ────────────────────────────────────────────────────────────
-- fincore_auth  →  auth_user
-- ────────────────────────────────────────────────────────────
\c fincore_auth

-- Usuário de serviço é OWNER do schema: resolve Flyway + migrations
ALTER SCHEMA public OWNER TO auth_user;

-- Garante todos os privilégios no schema e objetos existentes
GRANT ALL PRIVILEGES ON SCHEMA public TO auth_user;
GRANT ALL PRIVILEGES ON ALL TABLES    IN SCHEMA public TO auth_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO auth_user;

-- Objetos criados futuramente (pelo próprio auth_user) já são seus.
-- ALTER DEFAULT PRIVILEGES garante acesso caso algum objeto seja
-- criado por fincore_admin via script manual.
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT ALL ON TABLES TO auth_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT ALL ON SEQUENCES TO auth_user;

-- Timeouts de segurança
ALTER ROLE auth_user SET statement_timeout               = '30s';
ALTER ROLE auth_user SET lock_timeout                    = '10s';
ALTER ROLE auth_user SET idle_in_transaction_session_timeout = '60s';

-- ────────────────────────────────────────────────────────────
-- fincore_account  →  account_user
-- ────────────────────────────────────────────────────────────
\c fincore_account

ALTER SCHEMA public OWNER TO account_user;
GRANT ALL PRIVILEGES ON SCHEMA public TO account_user;
GRANT ALL PRIVILEGES ON ALL TABLES    IN SCHEMA public TO account_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO account_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES    TO account_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO account_user;

ALTER ROLE account_user SET statement_timeout               = '30s';
ALTER ROLE account_user SET lock_timeout                    = '10s';
ALTER ROLE account_user SET idle_in_transaction_session_timeout = '60s';

-- ────────────────────────────────────────────────────────────
-- fincore_txn  →  txn_user
-- ────────────────────────────────────────────────────────────
\c fincore_txn

ALTER SCHEMA public OWNER TO txn_user;
GRANT ALL PRIVILEGES ON SCHEMA public TO txn_user;
GRANT ALL PRIVILEGES ON ALL TABLES    IN SCHEMA public TO txn_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO txn_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES    TO txn_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO txn_user;

ALTER ROLE txn_user SET statement_timeout               = '30s';
ALTER ROLE txn_user SET lock_timeout                    = '10s';
ALTER ROLE txn_user SET idle_in_transaction_session_timeout = '60s';

-- ────────────────────────────────────────────────────────────
-- fincore_notification  →  notification_user
-- ────────────────────────────────────────────────────────────
\c fincore_notification

ALTER SCHEMA public OWNER TO notification_user;
GRANT ALL PRIVILEGES ON SCHEMA public TO notification_user;
GRANT ALL PRIVILEGES ON ALL TABLES    IN SCHEMA public TO notification_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO notification_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES    TO notification_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO notification_user;

ALTER ROLE notification_user SET statement_timeout               = '30s';
ALTER ROLE notification_user SET lock_timeout                    = '10s';
ALTER ROLE notification_user SET idle_in_transaction_session_timeout = '60s';

-- ────────────────────────────────────────────────────────────
-- fincore_fraud  →  fraud_user
-- ────────────────────────────────────────────────────────────
\c fincore_fraud

ALTER SCHEMA public OWNER TO fraud_user;
GRANT ALL PRIVILEGES ON SCHEMA public TO fraud_user;
GRANT ALL PRIVILEGES ON ALL TABLES    IN SCHEMA public TO fraud_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO fraud_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES    TO fraud_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO fraud_user;

ALTER ROLE fraud_user SET statement_timeout               = '30s';
ALTER ROLE fraud_user SET lock_timeout                    = '10s';
ALTER ROLE fraud_user SET idle_in_transaction_session_timeout = '60s';

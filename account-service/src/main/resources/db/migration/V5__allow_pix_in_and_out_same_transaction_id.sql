-- ============================================================
-- V5 — Permite duas linhas de extrato por transferência PIX
-- ============================================================
-- Um PIX gera um lançamento PIX_OUT no remetente e um PIX_IN no
-- destinatário. Ambos compartilham o mesmo transaction_id para que
-- o extrato dos dois lados possa ser correlacionado.

ALTER TABLE transactions DROP CONSTRAINT IF EXISTS transactions_id_unique;

CREATE INDEX IF NOT EXISTS idx_transactions_transaction_id ON transactions (transaction_id);

COMMENT ON COLUMN transactions.transaction_id IS 'Identificador da transação; pode se repetir em PIX_OUT/PIX_IN do mesmo PIX';

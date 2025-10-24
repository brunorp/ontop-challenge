-- Remove idempotency_key column and its index from transactions table
DROP INDEX IF EXISTS idx_transactions_idempotency_key;
ALTER TABLE transactions DROP COLUMN IF EXISTS idempotency_key;


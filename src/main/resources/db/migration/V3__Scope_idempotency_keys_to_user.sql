-- Keys were globally unique, so one user's key could replay another user's stored response.
-- Existing rows all have user_id = 0, which no real user has (ids start at 1), so they can never
-- match a new (user_id, key) lookup. They are kept and removed by the hourly 48-hour purge.
ALTER TABLE idempotency_keys DROP CONSTRAINT IF EXISTS idempotency_keys_idempotency_key_key;
ALTER TABLE idempotency_keys ADD CONSTRAINT uk_idempotency_keys_user_key UNIQUE (user_id, idempotency_key);

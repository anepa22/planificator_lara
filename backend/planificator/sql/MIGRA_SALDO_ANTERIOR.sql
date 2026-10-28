-- Saldo de Del período anterior cargado una sola vez,
-- para el período que abre el 1 de octubre de 2026.
-- Idempotente. La API también crea la tabla al arrancar.

BEGIN;

CREATE TABLE IF NOT EXISTS vacation_opening_carry (
    user_id UUID PRIMARY KEY REFERENCES app_users(id) ON DELETE CASCADE,
    days INTEGER NOT NULL CHECK (days >= 0)
);

COMMIT;

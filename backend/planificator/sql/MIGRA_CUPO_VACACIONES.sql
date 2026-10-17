-- Fecha de ingreso y días de vacaciones acordados por fuera de la ley.
-- Idempotente. La API también aplica este cambio al arrancar.

ALTER TABLE app_users ADD COLUMN IF NOT EXISTS hire_date DATE NULL;

CREATE TABLE IF NOT EXISTS vacation_agreed_days (
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    period_year INTEGER NOT NULL,
    days INTEGER NOT NULL CHECK (days >= 0),
    PRIMARY KEY (user_id, period_year)
);

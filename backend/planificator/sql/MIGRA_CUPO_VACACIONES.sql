-- Fecha de ingreso y ajuste manual del cupo de vacaciones.
-- Idempotente: se puede correr más de una vez. La API lo aplica al arrancar.

ALTER TABLE app_users ADD COLUMN IF NOT EXISTS hire_date DATE NULL;

CREATE TABLE IF NOT EXISTS vacation_adjustments (
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    period_year INTEGER NOT NULL,
    days INTEGER NOT NULL CHECK (days >= 0),
    PRIMARY KEY (user_id, period_year)
);

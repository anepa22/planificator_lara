-- Obligar cambio de contraseña en altas nuevas y tras un reset de admin.
-- Las cuentas que ya existían siguen con su clave; no se las fuerza.

ALTER TABLE app_users
  ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN;

UPDATE app_users
SET must_change_password = FALSE
WHERE must_change_password IS NULL;

ALTER TABLE app_users
  ALTER COLUMN must_change_password SET DEFAULT TRUE;

ALTER TABLE app_users
  ALTER COLUMN must_change_password SET NOT NULL;

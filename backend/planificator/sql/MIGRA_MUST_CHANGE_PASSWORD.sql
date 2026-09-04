-- Obligar cambio de contraseña en el primer ingreso (y tras un reset).
ALTER TABLE app_users
  ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT TRUE;

-- Quienes ya pueden entrar (clave provisoria 123456 u otra puesta por un admin)
-- tienen que cambiarla en el próximo login.
UPDATE app_users
SET must_change_password = TRUE
WHERE can_login = TRUE AND is_active = TRUE;

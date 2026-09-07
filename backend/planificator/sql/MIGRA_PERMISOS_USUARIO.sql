-- Permisos adicionales asignados directamente a un usuario.
-- Los permisos efectivos son la unión de estos permisos y los heredados por roles.

BEGIN;

CREATE TABLE IF NOT EXISTS user_permissions (
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    permission_id VARCHAR(50) NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, permission_id)
);

CREATE INDEX IF NOT EXISTS ix_user_permissions_permission
    ON user_permissions(permission_id);

COMMIT;

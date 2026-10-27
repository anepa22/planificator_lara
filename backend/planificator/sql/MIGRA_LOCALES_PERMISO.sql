-- Permiso para crear locales y darlos de alta o de baja.
-- Idempotente. La API también lo crea al arrancar.
-- El otorgamiento corre solo la primera vez: un arranque o una
-- segunda ejecución no se lo devuelve a un rol del que se sacó.

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM permissions WHERE id = 'locations_manage') THEN
        INSERT INTO permissions (id, code, name) VALUES
            ('locations_manage', 'locations:manage', 'Gestionar locales');

        INSERT INTO role_permissions (role_id, permission_id)
        SELECT rp.role_id, 'locations_manage'
        FROM role_permissions rp
        JOIN permissions p ON p.id = rp.permission_id
        WHERE p.code = 'users:manage'
        ON CONFLICT DO NOTHING;

        INSERT INTO user_permissions (user_id, permission_id)
        SELECT up.user_id, 'locations_manage'
        FROM user_permissions up
        JOIN permissions p ON p.id = up.permission_id
        WHERE p.code = 'users:manage'
        ON CONFLICT DO NOTHING;

        INSERT INTO role_permissions (role_id, permission_id) VALUES
            ('admin', 'locations_manage')
        ON CONFLICT DO NOTHING;
    ELSE
        UPDATE permissions
        SET code = 'locations:manage', name = 'Gestionar locales'
        WHERE id = 'locations_manage';
    END IF;
END $$;

COMMIT;

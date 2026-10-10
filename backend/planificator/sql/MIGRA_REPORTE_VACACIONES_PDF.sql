-- Permiso para imprimir el reporte de vacaciones en PDF.
-- Queda en Supervisor y Administrador. No se copia al resto de los roles.

BEGIN;

INSERT INTO permissions (id, code, name)
VALUES ('vacations_print', 'vacations:print', 'Imprimir reporte de vacaciones')
ON CONFLICT (id) DO UPDATE SET code = EXCLUDED.code, name = EXCLUDED.name;

INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('admin', 'vacations_print'),
    ('editor', 'vacations_print')
ON CONFLICT DO NOTHING;

COMMIT;

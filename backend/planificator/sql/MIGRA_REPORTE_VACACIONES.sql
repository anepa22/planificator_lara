-- Permiso para consultar el reporte de vacaciones.
-- Quien ya carga vacaciones lo recibe; también queda en Supervisor y Administrador.

BEGIN;

INSERT INTO permissions (id, code, name)
VALUES ('vacations_report', 'vacations:report', 'Ver reporte de vacaciones')
ON CONFLICT (id) DO UPDATE SET code = EXCLUDED.code, name = EXCLUDED.name;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, 'vacations_report'
FROM role_permissions rp
JOIN permissions p ON p.id = rp.permission_id
WHERE p.code = 'vacations:write'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('admin', 'vacations_report'),
    ('editor', 'vacations_report')
ON CONFLICT DO NOTHING;

COMMIT;

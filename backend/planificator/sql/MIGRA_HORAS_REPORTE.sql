-- Permiso para ver el reporte de horas asignadas.
-- Idempotente. La API también lo crea al arrancar.
-- El otorgamiento corre solo la primera vez: un arranque o una
-- segunda ejecución no se lo devuelve a un rol del que se sacó.

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM permissions WHERE id = 'hours_report') THEN
        INSERT INTO permissions (id, code, name) VALUES
            ('hours_report', 'hours:report', 'Ver reporte de horas');

        INSERT INTO role_permissions (role_id, permission_id) VALUES
            ('admin', 'hours_report'),
            ('editor', 'hours_report')
        ON CONFLICT DO NOTHING;
    ELSE
        UPDATE permissions
        SET code = 'hours:report', name = 'Ver reporte de horas'
        WHERE id = 'hours_report';
    END IF;
END $$;

COMMIT;

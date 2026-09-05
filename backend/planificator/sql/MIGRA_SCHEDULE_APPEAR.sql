-- Quien figura en Horarios se decide por permiso, no por el rol Asistente.
-- Idempotente.

INSERT INTO permissions (id, code, name)
VALUES ('schedule_appear', 'schedule:appear', 'Aparecer en horarios')
ON CONFLICT (id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
VALUES ('personal', 'schedule_appear')
ON CONFLICT DO NOTHING;

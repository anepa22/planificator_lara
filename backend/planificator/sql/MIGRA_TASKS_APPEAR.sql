-- Quien figura como responsable de una tarea se decide por permiso.
-- No se asigna a Administrador. Idempotente.

INSERT INTO permissions (id, code, name)
VALUES ('tasks_appear', 'tasks:appear', 'Aparecer en tareas')
ON CONFLICT (id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
VALUES
  ('personal', 'tasks_appear'),
  ('editor', 'tasks_appear')
ON CONFLICT DO NOTHING;

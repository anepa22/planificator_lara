-- Retiro automático de tareas verificadas.
-- Los días son corridos y se cuentan desde el último ingreso a Verificada.

CREATE TABLE IF NOT EXISTS task_settings (
  id SMALLINT PRIMARY KEY CHECK (id = 1),
  verified_retention_days INTEGER NOT NULL DEFAULT 7
    CHECK (verified_retention_days BETWEEN 1 AND 3650),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO task_settings (id, verified_retention_days)
VALUES (1, 7)
ON CONFLICT (id) DO NOTHING;

INSERT INTO permissions (id, code, name)
VALUES (
  'tasks_retention',
  'tasks:retention',
  'Configurar retiro de tareas verificadas'
)
ON CONFLICT (id) DO UPDATE
SET code = EXCLUDED.code, name = EXCLUDED.name;

INSERT INTO role_permissions (role_id, permission_id) VALUES
  ('editor', 'tasks_retention'),
  ('admin', 'tasks_retention')
ON CONFLICT DO NOTHING;

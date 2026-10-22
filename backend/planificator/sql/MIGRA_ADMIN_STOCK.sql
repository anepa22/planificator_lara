-- Perfil Admin de Stock y marca de las tareas.
-- Idempotente. La API también aplica este cambio al arrancar.

ALTER TABLE tasks ADD COLUMN IF NOT EXISTS stock BOOLEAN NOT NULL DEFAULT FALSE;

INSERT INTO permissions (id, code, name) VALUES
  ('tasks_stock', 'tasks:stock', 'Administrar tareas de stock')
ON CONFLICT (id) DO UPDATE
SET code = EXCLUDED.code, name = EXCLUDED.name;

INSERT INTO roles (id, code, name) VALUES
  ('stock', 'stock', 'Admin de Stock')
ON CONFLICT (id) DO UPDATE
SET code = EXCLUDED.code, name = EXCLUDED.name;

INSERT INTO role_permissions (role_id, permission_id) VALUES
  ('stock', 'tasks_stock')
ON CONFLICT DO NOTHING;

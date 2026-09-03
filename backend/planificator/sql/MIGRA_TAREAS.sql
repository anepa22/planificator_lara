-- Tablero Kanban de tareas.
-- Ejecutar una vez por ambiente. Es idempotente.

ALTER TABLE app_users
  ADD COLUMN IF NOT EXISTS person_id UUID NULL;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'app_users_person_fk'
  ) THEN
    ALTER TABLE app_users
      ADD CONSTRAINT app_users_person_fk
      FOREIGN KEY (person_id) REFERENCES people(id);
  END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_app_users_person
  ON app_users(person_id) WHERE person_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS tasks (
  id UUID PRIMARY KEY,
  title VARCHAR(160) NOT NULL,
  description TEXT,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  block_reason TEXT,
  assignee_person_id UUID NULL REFERENCES people(id),
  on_board BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT tasks_status_check CHECK (
    status IN ('PENDING', 'IN_PROGRESS', 'BLOCKED', 'DONE', 'VERIFIED')
  ),
  CONSTRAINT tasks_board_consistency CHECK (
    (NOT on_board AND assignee_person_id IS NULL)
    OR on_board
  )
);

ALTER TABLE tasks ADD COLUMN IF NOT EXISTS block_reason TEXT;

CREATE INDEX IF NOT EXISTS ix_tasks_board_status
  ON tasks(on_board, status, updated_at DESC);
CREATE INDEX IF NOT EXISTS ix_tasks_assignee
  ON tasks(assignee_person_id) WHERE assignee_person_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS task_history (
  id BIGSERIAL PRIMARY KEY,
  task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
  actor_user_id UUID NULL REFERENCES app_users(id) ON DELETE SET NULL,
  action VARCHAR(30) NOT NULL,
  from_status VARCHAR(20),
  to_status VARCHAR(20),
  from_assignee_person_id UUID NULL REFERENCES people(id),
  to_assignee_person_id UUID NULL REFERENCES people(id),
  block_reason TEXT,
  occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_task_history_task
  ON task_history(task_id, occurred_at DESC);

INSERT INTO permissions (id, code, name) VALUES
  ('tasks_write', 'tasks:write', 'Tomar y mover tareas propias'),
  ('tasks_manage', 'tasks:manage', 'Administrar todas las tareas')
ON CONFLICT (id) DO UPDATE
SET code = EXCLUDED.code, name = EXCLUDED.name;

INSERT INTO roles (id, code, name) VALUES
  ('personal', 'personal', 'Personal')
ON CONFLICT (id) DO UPDATE
SET code = EXCLUDED.code, name = EXCLUDED.name;

UPDATE roles SET name = 'Supervisor' WHERE id = 'editor';

INSERT INTO role_permissions (role_id, permission_id) VALUES
  ('personal', 'tasks_write'),
  ('editor', 'tasks_write'),
  ('editor', 'tasks_manage'),
  ('admin', 'tasks_write'),
  ('admin', 'tasks_manage')
ON CONFLICT DO NOTHING;

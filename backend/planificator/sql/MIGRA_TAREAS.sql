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
  assignee_user_id UUID NULL REFERENCES app_users(id),
  on_board BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT tasks_status_check CHECK (
    status IN ('PENDING', 'IN_PROGRESS', 'BLOCKED', 'DONE', 'VERIFIED')
  ),
  CONSTRAINT tasks_board_consistency CHECK (
    (NOT on_board AND assignee_user_id IS NULL)
    OR on_board
  )
);

ALTER TABLE tasks ADD COLUMN IF NOT EXISTS block_reason TEXT;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS location_id TEXT NULL;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'tasks_location_fk'
  ) THEN
    ALTER TABLE tasks
      ADD CONSTRAINT tasks_location_fk
      FOREIGN KEY (location_id) REFERENCES locations(id);
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_tasks_board_status
  ON tasks(on_board, status, updated_at DESC);

CREATE TABLE IF NOT EXISTS task_history (
  id BIGSERIAL PRIMARY KEY,
  task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
  actor_user_id UUID NULL REFERENCES app_users(id) ON DELETE SET NULL,
  action VARCHAR(30) NOT NULL,
  from_status VARCHAR(20),
  to_status VARCHAR(20),
  from_assignee_user_id UUID NULL REFERENCES app_users(id),
  to_assignee_user_id UUID NULL REFERENCES app_users(id),
  block_reason TEXT,
  occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_task_history_task
  ON task_history(task_id, occurred_at DESC);

  INSERT INTO permissions (id, code, name) VALUES
    ('tasks_write', 'tasks:write', 'Tomar y mover tareas propias'),
    ('tasks_manage', 'tasks:manage', 'Administrar todas las tareas'),
    ('tasks_history', 'tasks:history', 'Ver historial de movimientos de tareas')
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
  ('editor', 'tasks_history'),
  ('admin', 'tasks_write'),
  ('admin', 'tasks_manage'),
  ('admin', 'tasks_history')
ON CONFLICT DO NOTHING;

-- Asignación a usuarios con rol Personal (ya no al equipo).
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS assignee_user_id UUID NULL;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'tasks_assignee_user_fk'
  ) THEN
    ALTER TABLE tasks
      ADD CONSTRAINT tasks_assignee_user_fk
      FOREIGN KEY (assignee_user_id) REFERENCES app_users(id);
  END IF;
END $$;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'tasks'
      AND column_name = 'assignee_person_id'
  ) THEN
    UPDATE tasks t
    SET assignee_user_id = u.id
    FROM app_users u
    WHERE t.assignee_user_id IS NULL
      AND u.person_id IS NOT NULL
      AND u.person_id = t.assignee_person_id;
  END IF;
END $$;

ALTER TABLE tasks DROP CONSTRAINT IF EXISTS tasks_board_consistency;
ALTER TABLE tasks ADD CONSTRAINT tasks_board_consistency CHECK (
  (NOT on_board AND assignee_user_id IS NULL) OR on_board
);

ALTER TABLE tasks DROP CONSTRAINT IF EXISTS tasks_assignee_person_id_fkey;
ALTER TABLE tasks DROP COLUMN IF EXISTS assignee_person_id;

DROP INDEX IF EXISTS ix_tasks_assignee;
CREATE INDEX IF NOT EXISTS ix_tasks_assignee
  ON tasks(assignee_user_id) WHERE assignee_user_id IS NOT NULL;

ALTER TABLE task_history ADD COLUMN IF NOT EXISTS from_assignee_user_id UUID NULL;
ALTER TABLE task_history ADD COLUMN IF NOT EXISTS to_assignee_user_id UUID NULL;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'task_history_from_assignee_user_fk'
  ) THEN
    ALTER TABLE task_history
      ADD CONSTRAINT task_history_from_assignee_user_fk
      FOREIGN KEY (from_assignee_user_id) REFERENCES app_users(id);
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'task_history_to_assignee_user_fk'
  ) THEN
    ALTER TABLE task_history
      ADD CONSTRAINT task_history_to_assignee_user_fk
      FOREIGN KEY (to_assignee_user_id) REFERENCES app_users(id);
  END IF;
END $$;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'task_history'
      AND column_name = 'from_assignee_person_id'
  ) THEN
    UPDATE task_history h
    SET from_assignee_user_id = u.id
    FROM app_users u
    WHERE h.from_assignee_user_id IS NULL
      AND u.person_id IS NOT NULL
      AND u.person_id = h.from_assignee_person_id;
    UPDATE task_history h
    SET to_assignee_user_id = u.id
    FROM app_users u
    WHERE h.to_assignee_user_id IS NULL
      AND u.person_id IS NOT NULL
      AND u.person_id = h.to_assignee_person_id;
  END IF;
END $$;

ALTER TABLE task_history DROP CONSTRAINT IF EXISTS task_history_from_assignee_person_id_fkey;
ALTER TABLE task_history DROP CONSTRAINT IF EXISTS task_history_to_assignee_person_id_fkey;
ALTER TABLE task_history DROP COLUMN IF EXISTS from_assignee_person_id;
ALTER TABLE task_history DROP COLUMN IF EXISTS to_assignee_person_id;

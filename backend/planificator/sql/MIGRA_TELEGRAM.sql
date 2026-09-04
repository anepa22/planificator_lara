-- Chat privado al que se notifican los cambios de estado de tareas.
-- El valor se obtiene de Telegram después de que el usuario inicia el bot.

ALTER TABLE app_users
  ADD COLUMN IF NOT EXISTS telegram_chat_id VARCHAR(32);

-- Los avisos van a quien tenga este permiso, no al asignado de la tarea.
-- Idempotente: se puede correr más de una vez.

INSERT INTO permissions (id, code, name)
VALUES ('tasks_notify', 'tasks:notify', 'Recibir notificaciones de tareas')
ON CONFLICT (id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, 'tasks_notify'
FROM roles r
WHERE r.id IN ('admin', 'editor')
ON CONFLICT DO NOTHING;

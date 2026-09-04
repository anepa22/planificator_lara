-- Chat privado al que se notifican los cambios de estado de tareas.
-- El valor se obtiene de Telegram después de que el asistente inicia el bot.

ALTER TABLE app_users
  ADD COLUMN IF NOT EXISTS telegram_chat_id VARCHAR(32);

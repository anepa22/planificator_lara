-- Catálogo: el rol interno sigue siendo id/code 'personal'.
-- Solo cambia el nombre visible. Idempotente.
-- Ya aplicado en planificator_db_dev (2026-09-04).
-- Pendiente: planificator_db_prod al promover este cambio.

UPDATE roles
SET name = 'Asistente'
WHERE id = 'personal';

UPDATE permissions
SET name = 'Gestionar asistentes'
WHERE id = 'staff_write';

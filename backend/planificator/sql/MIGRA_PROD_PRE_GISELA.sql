-- Unir la persona de grilla "Gisela Montero" con el usuario gicemon
-- (misma clave, rol Supervisor). Correr DESPUÉS de MIGRA_TAREAS.sql
-- y ANTES de MIGRA_USUARIOS_UNICOS.sql.
-- No crea usuario nuevo ni toca password_hash.

UPDATE app_users u
SET person_id = p.id,
    display_name = p.name
FROM people p
WHERE lower(u.username) = 'gicemon'
  AND p.name = 'Gisela Montero'
  AND (u.person_id IS NULL OR u.person_id <> p.id);

DO $$
DECLARE
    n INT;
BEGIN
    SELECT COUNT(*) INTO n
    FROM app_users u
    JOIN people p ON p.id = u.person_id
    WHERE lower(u.username) = 'gicemon'
      AND p.name = 'Gisela Montero';
    IF n <> 1 THEN
        RAISE EXCEPTION 'No se pudo vincular gicemon con Gisela Montero (filas=%)', n;
    END IF;
END $$;

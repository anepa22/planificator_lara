-- =====================================================================
-- MIGRA_USUARIOS_UNICOS.sql
-- Unifica "personal" y "usuarios" en una sola entidad: app_users.
--
-- La tabla people desaparece. Todo lo que colgaba de people.id
-- (turnos, vacaciones, francos, horas) pasa a colgar de app_users.id.
--
-- Cada persona sin cuenta se convierte en un usuario con can_login = FALSE:
-- sigue apareciendo en el planificador pero no puede ingresar al sistema.
-- Un supervisor le habilita el login más adelante desde el ABM de usuarios.
--
-- Idempotente: se puede volver a ejecutar sin efecto.
-- =====================================================================

SET client_encoding TO 'UTF8';

BEGIN;

-- ---------------------------------------------------------------------
-- 1. app_users toma los atributos que antes vivían en people
-- ---------------------------------------------------------------------

ALTER TABLE app_users ADD COLUMN IF NOT EXISTS color TEXT NULL;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS can_login BOOLEAN NOT NULL DEFAULT TRUE;

-- ---------------------------------------------------------------------
-- 2. Cada persona pasa a ser un usuario
--    - si ya tenía cuenta vinculada, se le copia el color
--    - si no tenía cuenta, se le crea una sin login
-- ---------------------------------------------------------------------

DO $$
DECLARE
    r          RECORD;
    base_slug  TEXT;
    candidate  TEXT;
    suffix     INT;
    created    INT := 0;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'people'
    ) THEN
        RAISE NOTICE 'La tabla people ya no existe: nada que migrar';
        RETURN;
    END IF;

    -- Color de la persona vinculada
    EXECUTE $sql$
        UPDATE app_users u
        SET color = p.color
        FROM people p
        WHERE u.person_id = p.id
          AND u.color IS NULL
          AND p.color IS NOT NULL
    $sql$;

    -- Personas sin cuenta: se crea usuario sin login
    FOR r IN EXECUTE $sql$
        SELECT p.id, p.name, p.color, p.is_active
        FROM people p
        WHERE NOT EXISTS (SELECT 1 FROM app_users u WHERE u.person_id = p.id)
        ORDER BY p.name
    $sql$
    LOOP
        base_slug := lower(regexp_replace(
            translate(
                r.name,
                'áéíóúÁÉÍÓÚàèìòùÀÈÌÒÙäëïöüÄËÏÖÜñÑ',
                'aeiouAEIOUaeiouAEIOUaeiouAEIOUnN'),
            '[^a-zA-Z0-9]+', '.', 'g'));
        base_slug := btrim(base_slug, '.');
        IF base_slug IS NULL OR base_slug = '' THEN
            base_slug := 'personal';
        END IF;

        candidate := base_slug;
        suffix := 1;
        WHILE EXISTS (SELECT 1 FROM app_users WHERE lower(username) = candidate) LOOP
            suffix := suffix + 1;
            candidate := base_slug || suffix::TEXT;
        END LOOP;

        EXECUTE $sql$
            INSERT INTO app_users
                (username, password_hash, display_name, person_id, is_active, color, can_login)
            VALUES ($1, '!', $2, $3, $4, $5, FALSE)
        $sql$
        USING candidate, r.name, r.id, r.is_active, r.color;

        created := created + 1;
    END LOOP;

    RAISE NOTICE 'Usuarios creados a partir de personas: %', created;
END $$;

-- ---------------------------------------------------------------------
-- 3. shifts.person_id -> shifts.user_id
-- ---------------------------------------------------------------------

ALTER TABLE shifts ADD COLUMN IF NOT EXISTS user_id UUID NULL;

DO $$
DECLARE
    orphans INT;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'shifts' AND column_name = 'person_id'
    ) THEN
        RAISE NOTICE 'shifts.person_id ya no existe: turnos ya migrados';
        RETURN;
    END IF;

    EXECUTE $sql$
        UPDATE shifts s
        SET user_id = u.id
        FROM app_users u
        WHERE u.person_id = s.person_id
          AND s.user_id IS NULL
    $sql$;

    SELECT count(*) INTO orphans FROM shifts WHERE user_id IS NULL;
    IF orphans > 0 THEN
        RAISE EXCEPTION 'Quedaron % turnos sin usuario asociado; se aborta la migración', orphans;
    END IF;
END $$;

ALTER TABLE shifts ALTER COLUMN user_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'shifts_user_id_fkey'
    ) THEN
        ALTER TABLE shifts
            ADD CONSTRAINT shifts_user_id_fkey
            FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE;
    END IF;

    -- Un usuario no puede tener dos turnos solapados (reemplaza el de person_id)
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'shifts_no_user_time_overlap'
    ) THEN
        ALTER TABLE shifts
            ADD CONSTRAINT shifts_no_user_time_overlap
            EXCLUDE USING gist (user_id WITH =, time_range WITH &&);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_shifts_user_date ON shifts(user_id, work_date);

ALTER TABLE shifts DROP CONSTRAINT IF EXISTS shifts_no_person_time_overlap;
DROP INDEX IF EXISTS idx_shifts_person_date;

-- ---------------------------------------------------------------------
-- 4. Vistas: pasan de person_* a user_*
-- ---------------------------------------------------------------------

DROP VIEW IF EXISTS v_hours_by_person_week;
DROP VIEW IF EXISTS v_hours_by_user_week;
DROP VIEW IF EXISTS v_shifts_week;

CREATE VIEW v_shifts_week AS
SELECT s.id,
       s.user_id,
       u.display_name                                                     AS user_name,
       u.color                                                            AS user_color,
       s.location_id,
       l.name                                                             AS location_name,
       l.color                                                            AS location_color,
       l.color_soft                                                       AS location_color_soft,
       s.work_date,
       (date_trunc('week', s.work_date::timestamp))::date                 AS week_start,
       ((EXTRACT(isodow FROM s.work_date))::smallint - 1)                 AS day_index,
       s.start_time,
       s.end_time,
       round(EXTRACT(epoch FROM (s.end_time - s.start_time)) / 3600.0, 2) AS hours,
       s.notes
FROM shifts s
         JOIN app_users u ON u.id = s.user_id
         JOIN locations l ON l.id = s.location_id
WHERE u.is_active AND l.is_active;

CREATE VIEW v_hours_by_user_week AS
SELECT week_start,
       user_id,
       user_name,
       round(sum(hours), 2)  AS total_hours,
       (count(*))::integer   AS shift_count
FROM v_shifts_week
GROUP BY week_start, user_id, user_name;

-- ---------------------------------------------------------------------
-- 5. Adiós people
-- ---------------------------------------------------------------------

ALTER TABLE shifts DROP CONSTRAINT IF EXISTS shifts_person_id_fkey;
ALTER TABLE shifts DROP COLUMN IF EXISTS person_id;

ALTER TABLE app_users DROP CONSTRAINT IF EXISTS app_users_person_fk;
DROP INDEX IF EXISTS ux_app_users_person;
ALTER TABLE app_users DROP COLUMN IF EXISTS person_id;

DROP TABLE IF EXISTS people;

-- ---------------------------------------------------------------------
-- 6. Permisos
--    people:write pasa a llamarse staff:write (gestionar el personal
--    del planificador sin tocar credenciales ni roles).
-- ---------------------------------------------------------------------

INSERT INTO permissions (id, code, name)
VALUES ('staff_write', 'staff:write', 'Gestionar personal')
ON CONFLICT (id) DO UPDATE SET code = EXCLUDED.code, name = EXCLUDED.name;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, 'staff_write'
FROM role_permissions rp
WHERE rp.permission_id = 'people_write'
ON CONFLICT DO NOTHING;

DELETE FROM role_permissions WHERE permission_id = 'people_write';
DELETE FROM permissions WHERE id = 'people_write';

-- ---------------------------------------------------------------------
-- 7. Rol Personal = aparece en el planificador
--    Lo reciben los usuarios que vienen de people (sin login) y
--    cualquiera que ya tenga turnos cargados, para no perder filas
--    de la grilla. Las cuentas puramente administrativas quedan afuera.
-- ---------------------------------------------------------------------

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, 'personal'
FROM app_users u
WHERE (NOT u.can_login OR EXISTS (SELECT 1 FROM shifts s WHERE s.user_id = u.id))
ON CONFLICT DO NOTHING;

-- La bitácora histórica de personas queda como personal
UPDATE audit_log SET entity_type = 'staff' WHERE entity_type = 'person';

COMMIT;

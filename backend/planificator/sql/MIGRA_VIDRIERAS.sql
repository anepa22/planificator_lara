-- Vidriera por local y día.
-- Ejecutar contra la misma base que usa el API (dev/test/prod según el ambiente).

ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS supports_vidriera BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE locations
SET supports_vidriera = TRUE,
    updated_at = NOW()
WHERE is_active = TRUE
  AND id NOT IN ('vacaciones', 'franco')
  AND (
      lower(replace(id, ' ', '')) ~ '^lara[0-9]+$'
      OR lower(regexp_replace(name, '[^a-zA-Z0-9]', '', 'g')) ~ '^lara[0-9]+$'
  );

CREATE TABLE IF NOT EXISTS location_vidrieras (
    location_id TEXT NOT NULL REFERENCES locations (id),
    work_date   DATE NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (location_id, work_date)
);

CREATE INDEX IF NOT EXISTS location_vidrieras_work_date_idx
    ON location_vidrieras (work_date);

# Diseño: vidriera por local y día

## Modelo
No es un turno. Es un flag por par `(local, fecha)`.

Un día con Lara1 y Lara2 = dos filas. Un día sin vidriera = cero filas.

## Datos
Script `backend/planificator/sql/MIGRA_VIDRIERAS.sql` (el esquema no usa Flyway).

1. Columna `locations.supports_vidriera BOOLEAN NOT NULL DEFAULT FALSE`.
2. `UPDATE` a `TRUE` en locales Lara (id/nombre tipo Lara1, Lara2, …),
   nunca en `vacaciones` ni `franco`.
3. Tabla `location_vidrieras`:
   - `location_id` → `locations(id)`
   - `work_date`
   - PK `(location_id, work_date)`

El frontend no hardcodea “Lara”. Mira `supportsVidriera`.

## API

| Método | Ruta | Quién |
|---|---|---|
| `GET` | `/api/vidrieras?from=&to=` | Público, como turnos |
| `PUT` | `/api/vidrieras` `{ locationId, workDate }` | `shifts:write`, idempotente |
| `DELETE` | `/api/vidrieras?locationId=&workDate=` | `shifts:write` |

`GET /api/locations` incluye `supportsVidriera`.

Escritura rechaza local inexistente, inactivo, sin flag, o de ausencia.
Bitácora `entityType = vidriera` en CREATE/DELETE.

## UI
- Semana (`ScheduleGrid`): check en el encabezado de columna; encabezado amarillo si está activo.
- Mes (`MonthGantt`): comentario “Vidriera” en amarillo en la barra/tarjeta del turno, no en el header. El nombre del local no cambia de color.
- `App.jsx` carga el rango visible (semana o mes), igual que turnos.

## Archivos
Backend: Location + SQL + VidrieraController/Service/Repository + SecurityConfig + AuditService.
Frontend: `client.js`, `App.jsx`, `ScheduleGrid.jsx`, `MonthGantt.jsx`, CSS.

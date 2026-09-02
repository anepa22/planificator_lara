# Tasks: vidriera por local y día

## Datos
- [x] Script SQL: columna `locations.supports_vidriera` (default `false`).
- [x] `UPDATE` de esa columna en los locales Lara (`Lara1`, `Lara2`, …).
- [x] Tabla `location_vidrieras` con PK `(location_id, work_date)` y FK a `locations`.

## Backend
- [x] `Location` y `LocationRepository` exponen `supportsVidriera`.
- [x] Dominio + repositorio: listar por rango `from`/`to`, insertar, borrar.
- [x] `GET /api/vidrieras?from=&to=` público.
- [x] `PUT /api/vidrieras` y `DELETE` con `shifts:write`.
- [x] Rechazar local inexistente, sin flag, `vacaciones` o `franco`.
- [x] PUT idempotente.
- [x] Auditoría `vidriera` en CREATE/DELETE.
- [x] `GET /api/locations` incluye `supportsVidriera`.
- [x] `SecurityConfig`: GET de vidrieras `permitAll`.

## Frontend
- [x] `client.js`: `getVidrieras`, `putVidriera`, `deleteVidriera`.
- [x] `App.jsx`: cargar vidrieras con la semana y el mes visibles.
- [x] `ScheduleGrid`: badge “Vidriera” si `supportsVidriera`; clic prende/apaga el día de las pestañas; sin `shifts:write` se ve y no se cliquea.
- [x] `MonthGantt`: cabecera del día con nombre(s) en amarillo; no se marca desde acá.
- [x] Filtro de local en el mes: solo vidrieras de esos locales.
- [x] Recargar mantiene lo marcado.
- [x] CSS del nombre amarillo (solo mes).

## No hacer
- [x] No tocar menú, login, turnos, vacaciones, almuerzo, usuarios, ABM de locales.

## Pendiente operativo
- [ ] Ejecutar `backend/planificator/sql/MIGRA_VIDRIERAS.sql` en la base.
- [ ] Verificar criterios de aceptación en semana y mes.

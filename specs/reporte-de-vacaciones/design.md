# Diseño: reporte de vacaciones

## Datos
No hay tabla nueva. El reporte lee `shifts` con `location_id = vacaciones`.

El permiso se crea con `backend/planificator/sql/MIGRA_REPORTE_VACACIONES.sql`.
El esquema no usa Flyway. Ese script corre una sola vez y copia el permiso a
los roles que ya cargan vacaciones.

`AuthCleanup`, al arrancar, asegura `vacations:report` y `vacations:print`
en `admin` y `editor`. No vuelve a copiar el permiso de consulta a otros
roles: si se lo sacaron a un rol, el próximo arranque no lo devuelve.

El PDF lo arma `VacationReportPdf` con los mismos tramos que la pantalla
y, debajo de cada persona, el total de sus días.
El archivo se llama `vacaciones-YYYY-MM-DD-YYYY-MM-DD.pdf`.

## API

| Método | Ruta | Quién |
|---|---|---|
| `GET` | `/api/shifts/vacation-report?from=&to=` | Autenticado con `vacations:report` |
| `GET` | `/api/shifts/vacation-report.pdf?from=&to=` | Autenticado con `vacations:print` |

La lectura de la grilla (`GET /api/shifts`) sigue siendo pública.
Esta ruta no entra en `permitAll`: sin sesión responde 401 y sin el
permiso responde 403.

La respuesta agrupa por persona:

- `from`, `to`, `totalDays`
- `people[]`: `userId`, `userName`, `days`, `ranges[]`
- cada rango: `dateFrom`, `dateTo`, `days`

Fechas en ISO (`YYYY-MM-DD`).

## Agrupación
La consulta devuelve un día por persona y fecha. El servicio ordena por
persona y fecha, y une los días contiguos. No depende de que la base
devuelva a cada persona en un bloque.

## UI
Modal `VacationReportModal`, abierto desde Asistentes → Vacaciones → Reporte.

El selector de año marca el año solo cuando Desde y Hasta son el 1 de enero
y el 31 de diciembre de ese año. Si se cambia una fecha a mano, queda en
Personalizado.

Si se disparan dos búsquedas, solo se muestra la última respuesta.

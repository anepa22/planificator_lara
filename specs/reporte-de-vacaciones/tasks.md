# Tasks: reporte de vacaciones

## Especificación
- [x] Definir permiso, menú, tramos y período máximo.

## Base de datos
- [x] Script `MIGRA_REPORTE_VACACIONES.sql`.
- [x] El API crea el permiso al arrancar y lo deja en Supervisor y Administrador.
- [x] La copia al resto de los roles que cargan vacaciones queda solo en el script.
- [ ] Ejecutar el script en cada ambiente antes del deploy, o dejar que lo haga el arranque.

## Backend
- [x] Permiso `vacations:report`.
- [x] Consulta de días de vacaciones por rango.
- [x] Agrupación de días contiguos por persona.
- [x] `GET /api/shifts/vacation-report` autenticado y con permiso.
- [x] Rechazo de rango invertido o mayor a 800 días.
- [x] Prueba de tramos con días intercalados entre personas.

## Frontend
- [x] Ítem Reporte separado de Cargar.
- [x] Modal con año y fechas.
- [x] Listado por persona, tramo y totales.
- [x] El menú respeta `vacations:report` y `vacations:write` por separado.
- [x] Botón Imprimir PDF solo con `vacations:print`.
- [x] `GET /api/shifts/vacation-report.pdf` exige ese permiso.
- [x] El permiso de impresión queda en Supervisor y Administrador.

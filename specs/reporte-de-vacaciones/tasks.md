# Tasks: reporte de vacaciones

## Especificación
- [x] Definir permiso, menú, tramos y período máximo.

## Base de datos
- [x] Script `MIGRA_REPORTE_VACACIONES.sql`.
- [x] Script `MIGRA_REPORTE_VACACIONES_PDF.sql`.
- [x] El API crea los dos permisos al arrancar y los deja en Supervisor y Administrador.
- [x] La copia de la consulta al resto de los roles que cargan vacaciones queda solo en el script.
- [x] En producción no hace falta ejecutar los scripts: lo cubre el arranque.

## Backend
- [x] Permiso `vacations:report`.
- [x] Consulta de días de vacaciones por rango.
- [x] Agrupación de días contiguos por persona.
- [x] `GET /api/shifts/vacation-report` autenticado y con permiso.
- [x] Rechazo de rango invertido o mayor a 800 días.
- [x] Prueba de tramos con días intercalados entre personas.
- [x] Permiso `vacations:print`.
- [x] `GET /api/shifts/vacation-report.pdf`.
- [x] El PDF totaliza los días de cada persona.

## Frontend
- [x] Ítem Reporte separado de Cargar.
- [x] Modal con año y fechas.
- [x] Listado por persona, tramo y totales.
- [x] El menú respeta `vacations:report` y `vacations:write` por separado.
- [x] Botón Imprimir PDF solo con `vacations:print`.
- [x] El botón imprime el período en pantalla y espera a que las fechas coincidan.
- [x] El permiso de impresión queda en Supervisor y Administrador.

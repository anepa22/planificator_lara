# Diseño: reporte de vacaciones

## Datos
No hay tabla nueva. El reporte lee `shifts` con `location_id = vacaciones`.

No usa Flyway. Los permisos se crean al arrancar el API, así que en
producción no hace falta correr un script.

`MIGRA_REPORTE_VACACIONES.sql` queda por si se aplica a mano: copia
`vacations:report` a los roles que ya tienen `vacations:write`.
`MIGRA_REPORTE_VACACIONES_PDF.sql` asigna `vacations:print` solo a `admin`
y `editor`.

`AuthCleanup` asegura los dos permisos en `admin` y `editor`. No vuelve a
copiar la consulta a otros roles ni entrega la impresión a quien solo carga
vacaciones.

## PDF
`ShiftService` arma el reporte, valida el período y exige `vacations:print`.
No pide además `vacations:report`. `VacationReportPdf` solo lo dibuja.

Columnas: Persona, Desde, Hasta y Días. Las fechas van en `dd/MM/yyyy`.
Debajo de los tramos de cada persona hay una fila `Total` con la suma de
sus días. El encabezado indica el período, los días totales y la cantidad
de personas.

La respuesta es `application/pdf`, con
`Content-Disposition: attachment` y nombre
`vacaciones-YYYY-MM-DD-YYYY-MM-DD.pdf`.

## API

| Método | Ruta | Quién |
|---|---|---|
| `GET` | `/api/shifts/vacation-report?from=&to=` | Autenticado con `vacations:report` |
| `GET` | `/api/shifts/vacation-report.pdf?from=&to=` | Autenticado con `vacations:print` |

La lectura de la grilla (`GET /api/shifts`) sigue siendo pública.
Estas dos rutas no entran en `permitAll`: sin sesión responden 401 y sin
el permiso correspondiente responden 403.

La respuesta JSON agrupa por persona:

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

El botón Imprimir PDF baja el `from` y `to` del reporte ya cargado. Queda
deshabilitado mientras las fechas del formulario no coinciden con ese
reporte, o mientras hay una búsqueda o una descarga en curso. La descarga
no reemplaza el listado de la pantalla.

# Proposal

## Why

Las horas de la semana se ven en la grilla, pero no hay forma de consultar un rango de fechas elegido ni de acotar por persona o por local. Para saber cuántas horas se asignaron hay que mirar semana por semana.

## What Changes

- En el menú Asistentes, un ítem Horas abre un reporte de horas asignadas.
- El reporte pide un rango Desde / Hasta, inclusive, y permite filtrar por persona y por local.
- Cada persona del resultado muestra sus horas asignadas y el total del rango.
- Franco y vacaciones no suman. El almuerzo se descuenta igual que en la grilla: las horas de almuerzo que ya usa la grilla, una vez por cada día con turno de venta, sin bajar de 0.
- Quien no tiene el permiso no ve el ítem. Quien lo tiene puede bajar un PDF de la búsqueda que está en pantalla.

## Capabilities

### New Capabilities

- `reporte-de-horas`: consulta de horas asignadas en un rango de fechas, con filtros de persona y local, y un PDF de esa búsqueda, desde el menú Asistentes.

### Modified Capabilities

## Impact

- Menú Asistentes, junto a Vacaciones y Vidrieras.
- Un permiso nuevo para ver el reporte, distinto de cargar turnos o de ver vacaciones.
- API de consulta del rango y de su PDF. Usa el mismo almuerzo que la grilla; la grilla no cambia. El PDF no tiene un permiso distinto del de ver el reporte.
- Franco, vacaciones, el cupo y el catálogo de locales no cambian.

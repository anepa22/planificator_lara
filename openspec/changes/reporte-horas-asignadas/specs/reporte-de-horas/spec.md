# Spec Delta

## Purpose

Consultar las horas asignadas en un rango de fechas y acotar el resultado por persona y por local.

## ADDED Requirements

### Requirement: Quién consulta
Solo un usuario con `hours:report` SHALL poder abrir el reporte. Sin ese permiso el sistema SHALL rechazar la consulta y SHALL NOT mostrar el ítem. `shifts:write` SHALL NOT alcanzar.

#### Scenario: Sin permiso
- **WHEN** un usuario sin `hours:report` abre el menú o consulta el reporte
- **THEN** no ve el ítem y la consulta se rechaza

### Requirement: Acceso al menú
El menú Asistentes SHALL mostrar Horas a quien tenga `hours:report`, junto a Vacaciones y Vidrieras.

#### Scenario: Con permiso
- **WHEN** un usuario con `hours:report` abre Asistentes
- **THEN** ve el ítem Horas

### Requirement: Rango
Al abrir, el reporte SHALL mostrar el mes calendario en curso. Desde y Hasta SHALL ser inclusive. Hasta SHALL NOT ser anterior a Desde y el rango SHALL NOT superar 800 días.

#### Scenario: Apertura
- **WHEN** se abre el reporte el 27 de octubre de 2026
- **THEN** el rango mostrado es del 1 de octubre de 2026 al 31 de octubre de 2026

#### Scenario: Rango inválido
- **WHEN** Hasta es anterior a Desde o el rango supera 800 días
- **THEN** el sistema rechaza la consulta

### Requirement: Filtros
El reporte SHALL poder filtrar por una persona y por un local. Sin filtro SHALL incluir a todas las personas y a todos los locales de venta. Franco y vacaciones SHALL NOT ofrecerse como local.

#### Scenario: Una persona
- **WHEN** se elige una persona y un rango
- **THEN** el resultado es solo de esa persona

#### Scenario: Un local
- **WHEN** se elige un local de venta
- **THEN** solo cuentan los turnos de ese local

### Requirement: Horas asignadas
El reporte SHALL sumar la duración de cada turno de venta del rango y SHALL restar el almuerzo de la grilla una vez por día trabajado, sin bajar de 0. Franco y vacaciones SHALL NOT sumar ni contar como día. Cada persona SHALL aparecer en una fila con sus horas. Sin filtro de persona, quien quede en 0 SHALL NOT aparecer. Con esa persona elegida, la fila SHALL mostrar 0. El reporte SHALL mostrar el total de las filas.

#### Scenario: Turno de venta
- **WHEN** una persona tiene un turno de 09:30 a 20:00 y el almuerzo de la grilla es 0
- **THEN** ese turno suma 10,5 horas

#### Scenario: Almuerzo
- **WHEN** una persona tiene un turno de 09:30 a 20:00 y el almuerzo de la grilla es 1 hora
- **THEN** ese día suma 9,5 horas

#### Scenario: Dos turnos el mismo día
- **WHEN** una persona tiene dos turnos de venta el mismo día y el almuerzo es 1 hora
- **THEN** se resta 1 hora una sola vez

#### Scenario: Franco
- **WHEN** el único turno de una persona en el rango es un franco
- **THEN** sin filtro de persona no aparece, y elegida muestra 0

#### Scenario: Total
- **WHEN** el resultado tiene dos personas con 10 y 5 horas
- **THEN** el total es 15

### Requirement: PDF
Quien tenga `hours:report` SHALL poder descargar un PDF de la última búsqueda. Si el rango, un filtro o el almuerzo cambiaron y todavía no se buscó, el botón SHALL permanecer deshabilitado. El PDF SHALL repetir el rango, los filtros, el almuerzo, cada persona con sus horas y el total.

#### Scenario: Búsqueda en pantalla
- **WHEN** el usuario imprime después de buscar un rango
- **THEN** el PDF corresponde a esa búsqueda y no a fechas o filtros modificados sin buscar

#### Scenario: Mismas horas
- **WHEN** la búsqueda muestra a una persona con 9,5 horas y un total de 9,5
- **THEN** el PDF repite 9,5 y el total 9,5

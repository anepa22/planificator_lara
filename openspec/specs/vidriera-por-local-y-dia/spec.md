# Vidriera por local y día

## Purpose
Marcar si un local de ventas hace vidriera en una fecha y mostrarlo en la semana y en el mes.

## Requirements

### Requirement: Marca por local y fecha
El sistema SHALL guardar la vidriera como un sí o no para cada par de local y fecha, sin persona ni horario. Marcar un local SHALL NOT marcar otro.

#### Scenario: Dos locales el mismo día
- **WHEN** se marca vidriera para Lara 1 y Lara 2 en la misma fecha
- **THEN** quedan registradas dos vidrieras independientes

### Requirement: Locales habilitados
El sistema SHALL aceptar solo un local activo con vidriera habilitada. SHALL rechazar un local inexistente, inactivo, sin esa habilitación, o que sea vacaciones o franco.

#### Scenario: Local no habilitado
- **WHEN** alguien intenta marcar vidriera en un local que no la admite
- **THEN** el sistema rechaza la operación

### Requirement: Carga y modificación
Un usuario con `shifts:write` SHALL poder marcar y desmarcar vidrieras desde el mismo modal. Guardar un día SHALL agregar los locales marcados y quitar los desmarcados. Sin `shifts:write`, el sistema SHALL mostrar los indicadores y SHALL NOT ofrecer la edición.

#### Scenario: Quitar un local
- **WHEN** un usuario con `shifts:write` destilda un local ya marcado y guarda
- **THEN** ese local deja de tener vidriera en esa fecha

#### Scenario: Lectura sin permiso de escritura
- **WHEN** un usuario sin `shifts:write` consulta la semana o el mes
- **THEN** ve las vidrieras cargadas y no puede modificarlas

### Requirement: Vista semanal
Si un local tiene vidriera en el día seleccionado, la semana SHALL mostrar la palabra «Vidriera» y el encabezado de ese local resaltado. Con `shifts:write`, activar ese encabezado SHALL abrir la edición de ese día.

#### Scenario: Día marcado
- **WHEN** el día seleccionado tiene vidriera en un local
- **THEN** su encabezado muestra «Vidriera»

### Requirement: Vista mensual
El mes SHALL mostrar «Vidriera» en la tarjeta del turno de ese local y fecha. Debajo del mes SHALL listar los días con vidriera y sus locales. Un filtro de local SHALL limitar tanto las tarjetas como ese resumen.

#### Scenario: Mes filtrado
- **WHEN** el mes está filtrado por un local
- **THEN** el resumen muestra solo las vidrieras de ese local

### Requirement: Consulta y permanencia
La consulta por rango de fechas SHALL ser pública. Una vidriera guardada SHALL permanecer al recargar la página. El alta y la baja SHALL quedar en la bitácora.

#### Scenario: Recarga
- **WHEN** se recarga la página después de guardar una vidriera
- **THEN** la vidriera sigue visible

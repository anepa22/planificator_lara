# Reporte de vacaciones

## Purpose
Consultar los días de vacaciones ya cargados en un período y agrupar los días seguidos de cada persona.

## Requirements

### Requirement: Permisos independientes
Ver el reporte SHALL exigir `vacations:report`. Imprimir el PDF SHALL exigir `vacations:print`. Cargar o quitar vacaciones SHALL seguir exigiendo `vacations:write`. Ninguno de los tres permisos SHALL reemplazar a otro.

#### Scenario: Consulta sin permiso de impresión
- **WHEN** un usuario con `vacations:report` y sin `vacations:print` abre el reporte
- **THEN** ve los tramos y no puede imprimir el PDF

#### Scenario: Impresión sin permiso de consulta
- **WHEN** un usuario con `vacations:print` solicita el PDF
- **THEN** el sistema lo genera sin exigir también `vacations:report`

### Requirement: Acceso al menú
El menú Asistentes SHALL mostrar Vacaciones → Reporte a quien tenga `vacations:report` y Vacaciones → Cargar a quien tenga `vacations:write`. Sin sesión o sin el permiso correspondiente, la consulta SHALL responder 401 o 403.

#### Scenario: Usuario sin permiso
- **WHEN** un usuario sin `vacations:report` intenta abrir el reporte
- **THEN** no ve el ítem y la API rechaza la consulta

### Requirement: Período
Al abrir, el reporte SHALL mostrar el año en curso. El usuario SHALL poder elegir el año anterior, el actual, el siguiente o un rango Desde / Hasta. Hasta SHALL NOT ser anterior a Desde y el período SHALL NOT superar 800 días.

#### Scenario: Rango inválido
- **WHEN** Hasta es anterior a Desde o el período supera 800 días
- **THEN** el sistema rechaza la consulta

### Requirement: Tramos
El sistema SHALL leer las vacaciones cargadas en la grilla y SHALL unir los días calendario consecutivos de la misma persona en un tramo. Un día intermedio sin vacaciones SHALL abrir otro tramo. La cantidad de días SHALL incluir el primer y el último día. Dos personas con el mismo nombre SHALL permanecer separadas.

#### Scenario: Días consecutivos
- **WHEN** una persona tiene vacaciones el lunes, martes y jueves
- **THEN** el reporte muestra un tramo de dos días y otro de un día

#### Scenario: Período vacío
- **WHEN** el período no tiene vacaciones
- **THEN** el reporte muestra el período sin personas

### Requirement: Totales
Cada persona SHALL mostrar sus tramos y la suma de sus días. El encabezado SHALL mostrar la suma de días y la cantidad de personas del período.

#### Scenario: Total de una persona
- **WHEN** una persona tiene dos tramos
- **THEN** su total es la suma de los días de ambos tramos

### Requirement: PDF
Quien tenga `vacations:print` SHALL poder descargar un PDF del período que está en pantalla. Si Desde o Hasta cambiaron y todavía no se buscó, el botón SHALL permanecer deshabilitado. El PDF SHALL listar cada tramo y, debajo de cada persona, el total de sus días.

#### Scenario: Período en pantalla
- **WHEN** el usuario imprime después de buscar un rango
- **THEN** el PDF corresponde a ese rango y no a las fechas modificadas sin buscar

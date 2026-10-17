# Spec Delta

## Purpose

Calcular el cupo de vacaciones de cada período, sumar los días acordados y los acumulados, y rechazar una carga que supere ese total.

## ADDED Requirements

### Requirement: Fecha de ingreso
El sistema SHALL guardar una fecha de ingreso por persona y SHALL exigirla para calcular los días de ley. Quien administra usuarios SHALL poder cargarla y modificarla.

#### Scenario: Persona sin fecha de ingreso
- **WHEN** se intentan cargar vacaciones para una persona sin fecha de ingreso
- **THEN** el sistema rechaza la carga e indica que falta esa fecha

### Requirement: Período de vacaciones
El período SHALL ir del 1 de octubre de un año al 30 de septiembre del año siguiente y SHALL renovarse el 1 de octubre. Los días gozados SHALL ser las vacaciones cargadas con fecha dentro de ese período.

#### Scenario: Día de octubre
- **WHEN** se carga una vacación el 2 de octubre de 2026
- **THEN** esa vacación cuenta para el período que empieza el 1 de octubre de 2026

#### Scenario: Día de septiembre
- **WHEN** se carga una vacación el 20 de septiembre de 2027
- **THEN** esa vacación sigue contando para el período que empezó el 1 de octubre de 2026

### Requirement: Días de ley
El sistema SHALL calcular los días corridos de ley según los años cumplidos al 31 de diciembre del año en que empieza el período: 14 hasta 5 años, 21 con más de 5 y hasta 10, 28 con más de 10 y hasta 20, y 35 con más de 20.

#### Scenario: Seis años al cierre
- **WHEN** al 31 de diciembre de 2026 la persona cumple 6 años desde su ingreso
- **THEN** en el período que empieza el 1 de octubre de 2026 le corresponden 21 días de ley

#### Scenario: Cinco años exactos
- **WHEN** al 31 de diciembre la persona cumple exactamente 5 años y trabajó al menos la mitad de los lunes a sábado de ese año calendario
- **THEN** le corresponden 14 días de ley en el período que empieza ese año

### Requirement: Cupo proporcional
Si en el año calendario la persona trabajó menos de la mitad de los lunes a sábado, el sistema SHALL calcular los días de ley como 1 cada 20 días trabajados y SHALL descartar el resto. Un día trabajado SHALL ser una fecha de ese año con turno distinto de vacaciones y franco. Si llega a esa mitad, SHALL usar la escala por antigüedad.

#### Scenario: Ingreso el 1 de julio
- **WHEN** ingresa el 1 de julio, trabaja hasta el 31 de diciembre y esos días alcanzan la mitad de los lunes a sábado del año
- **THEN** los días de ley son los de la escala y no el proporcional

#### Scenario: Menos de veinte días
- **WHEN** la persona trabajó 19 días del año y no llegó a la mitad de los lunes a sábado
- **THEN** los días de ley de ese período son 0

#### Scenario: Cuarenta días trabajados
- **WHEN** la persona trabajó 40 días del año y no llegó a la mitad de los lunes a sábado
- **THEN** los días de ley de ese período son 2

### Requirement: Días acordados
Quien carga vacaciones SHALL poder guardar, antes de asignar las fechas, una cantidad de días acordados para una persona y un período. Esos días SHALL sumarse a los de ley. Si no hay cantidad acordada, el sistema SHALL usar cero.

#### Scenario: Acuerdo por fuera de la ley
- **WHEN** a una persona con 14 días de ley se le cargan 5 días acordados para ese período
- **THEN** esos 5 se suman a su cupo usable

### Requirement: Acumulación
El 1 de octubre el sistema SHALL sumar al período nuevo los días del período anterior que no se usaron. Esos días SHALL seguir acumulándose si tampoco se usan después.

#### Scenario: Quedan días
- **WHEN** en un período el cupo usable era 19 y se gozaron 10
- **THEN** el 1 de octubre siguiente se suman 9 días al período nuevo

### Requirement: Saldo visible
Al cargar vacaciones, el sistema SHALL mostrar, para cada persona y período afectado, los días de ley, los acordados, los acumulados, los gozados y los que quedan.

#### Scenario: Saldo con acuerdo y acumulación
- **WHEN** hay 14 días de ley, 5 acordados, 4 acumulados y 10 ya gozados
- **THEN** la pantalla muestra 23 de cupo usable, 10 gozados y 13 restantes

### Requirement: Rechazo al superar el cupo
El sistema SHALL rechazar una carga si, en algún período del rango, los días nuevos más los ya gozados superan la ley, más los acordados, más los acumulados. Quitar vacaciones SHALL devolver esos días al saldo.

#### Scenario: La carga no entra
- **WHEN** quedan 4 días en el período y se intentan cargar 5
- **THEN** el sistema no guarda esos días e indica cuántos quedan

#### Scenario: Rango en dos períodos
- **WHEN** el rango cruza el 1 de octubre
- **THEN** el sistema valida el cupo de cada período por separado

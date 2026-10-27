# Spec Delta

## Purpose

Calcular cuántos días de vacaciones le corresponden a cada persona según su antigüedad y cuántos le quedan en el período de goce.

## ADDED Requirements

### Requirement: Fecha de ingreso
El sistema SHALL guardar una fecha de ingreso por usuario. Sin esa fecha SHALL NOT calcular el cupo ni SHALL aceptar una carga nueva de vacaciones para esa persona.

#### Scenario: Usuario sin fecha
- **WHEN** se intenta cargar vacaciones para una persona sin fecha de ingreso
- **THEN** el sistema rechaza la carga

#### Scenario: Fecha cargada
- **WHEN** un administrador guarda la fecha de ingreso de un usuario
- **THEN** el sistema puede calcular el cupo de esa persona

### Requirement: Período de goce
El período de goce del año Y SHALL ir del 1 de octubre de Y al 30 de septiembre de Y+1. La antigüedad de ese período SHALL contarse desde la fecha de ingreso hasta el 31 de diciembre de Y.

#### Scenario: Día dentro del período
- **WHEN** se carga una vacación el 15 de octubre de 2026
- **THEN** ese día descuenta la antigüedad del período que abre el 1 de octubre de 2026

#### Scenario: Día del período siguiente
- **WHEN** se carga una vacación el 1 de octubre de 2027
- **THEN** esa vacación descuenta el período que termina el 30 de septiembre de 2028

### Requirement: Escala por antigüedad
Si al 31 de diciembre de Y la antigüedad es de al menos seis meses, el sistema SHALL otorgar 14 días corridos cuando no exceda cinco años, 21 cuando exceda cinco y no exceda diez, 28 cuando exceda diez y no exceda veinte, y 35 cuando exceda veinte.

#### Scenario: Cinco años justos
- **WHEN** al 31 de diciembre de Y la antigüedad es de cinco años
- **THEN** el cupo por antigüedad es 14 días

#### Scenario: Más de cinco años
- **WHEN** al 31 de diciembre de Y la antigüedad supera los cinco años y no supera los diez
- **THEN** el cupo por antigüedad es 21 días

#### Scenario: Más de veinte años
- **WHEN** al 31 de diciembre de Y la antigüedad supera los veinte años
- **THEN** el cupo por antigüedad es 35 días

### Requirement: Cupo proporcional
Si al 31 de diciembre de Y la antigüedad es menor a seis meses, el sistema SHALL otorgar un día por cada veinte días de lunes a viernes ya transcurridos desde el ingreso hasta hoy, con tope el 31 de diciembre de Y, y SHALL descartar la fracción. Un sábado o un domingo SHALL NOT entrar. El sistema SHALL NOT usar los turnos cargados para esta cuenta.

#### Scenario: Menos de seis meses
- **WHEN** la persona ingresó el 1 de octubre de 2026 y hoy es el 23 de octubre de 2026
- **THEN** hay 17 días de lunes a viernes y el cupo proporcional es 0

#### Scenario: Semana incompleta
- **WHEN** un miércoles no tiene turno cargado
- **THEN** ese miércoles entra en la cuenta y un domingo no entra

#### Scenario: Seis meses justos
- **WHEN** al 31 de diciembre de Y la antigüedad es de seis meses
- **THEN** el cupo usa la escala de 14 días y no la proporcional

### Requirement: Habilitación de la antigüedad
Desde el período que abre el 1 de octubre de 2026, la antigüedad de ese período SHALL entrar en Quedan. Lo habilitado antes de esa fecha SHALL NOT sumarse.

#### Scenario: Período abierto el 1 de octubre de 2026
- **WHEN** la antigüedad al 31 de diciembre de 2026 es 21 y hay 7 vacaciones cargadas en ese período
- **THEN** De este período es 21, Del período anterior es 0, Gozados es 7 y Quedan es 14

#### Scenario: Octubre de 2027
- **WHEN** de esos 21 sobran 14 y la antigüedad al 31 de diciembre de 2027 es 21
- **THEN** el 1 de octubre de 2027 Del período anterior es 14

### Requirement: Arrastre del saldo
El período que abre el 1 de octubre de 2026 SHALL arrancar con Del período anterior en cero. Desde el período siguiente, lo no gozado SHALL sumarse a la antigüedad que se habilita. Un exceso de días gozados SHALL NOT reducir el período siguiente.

#### Scenario: Años anteriores
- **WHEN** la persona ingresó el 1 de mayo de 2022 y tiene vacaciones anteriores al 1 de octubre de 2026
- **THEN** en el período que abre el 1 de octubre de 2026, Del período anterior es 0

#### Scenario: El sobrante sigue
- **WHEN** el 1 de octubre de 2027 Del período anterior es 14 y en ese período no se carga ningún día
- **THEN** el período que abre el 1 de octubre de 2028 suma esos 14 a su antigüedad

### Requirement: Ajuste manual
Quien tenga `vacations:write` SHALL poder sumar, para una persona y un período, una cantidad entera no negativa de días que el arrastre no conoce. Ese ajuste SHALL sumarse a Quedan y SHALL NOT reemplazar ni repetir el saldo arrastrado. El sistema SHALL rechazar un número negativo.

#### Scenario: Saldo inicial
- **WHEN** Del período anterior es 0, De este período es 14 y se agregan 3 días extra
- **THEN** Quedan es 17

#### Scenario: Día concedido
- **WHEN** De este período es 14 y se agrega 1 día extra
- **THEN** Quedan es 15

#### Scenario: No duplica el sobrante
- **WHEN** De este período es 14, Del período anterior es 0 y hay 3 días extra
- **THEN** Quedan es 17

#### Scenario: Número negativo
- **WHEN** se intenta guardar un ajuste negativo
- **THEN** el sistema rechaza el valor

### Requirement: Saldo del período
Desde el período que abre el 1 de octubre de 2026, el saldo SHALL ser De este período más Del período anterior más los días extra, menos las vacaciones ya cargadas en ese período. El sistema SHALL rechazar una carga nueva cuya cantidad de días, contando el primero y el último, deje el saldo de alguna persona seleccionada por debajo de cero.

#### Scenario: Carga dentro del saldo
- **WHEN** el saldo es 5 y se cargan 5 días corridos
- **THEN** el sistema guarda la carga y el saldo queda en cero

#### Scenario: Carga que se pasa
- **WHEN** el saldo es 2 y se intentan cargar 3 días
- **THEN** el sistema rechaza la carga y no guarda ningún día del rango

### Requirement: Cupo en cero
Si De este período, Del período anterior y Días extra son cero, el sistema SHALL rechazar la carga y SHALL NOT guardar ningún día del pedido.

#### Scenario: Menos de veinte días de calendario
- **WHEN** la persona ingresó el 20 de diciembre de 2026, sin días extra, y se intenta cargar un día en el período que abre el 1 de octubre de 2026
- **THEN** el sistema rechaza la carga y no guarda ningún día

#### Scenario: La antigüedad de este período habilita
- **WHEN** De este período es 14, Del período anterior es 0 y no hay días extra
- **THEN** se puede cargar un día de ese período

#### Scenario: Un día extra habilita
- **WHEN** De este período y Del período anterior son cero y hay 1 día extra
- **THEN** se puede cargar ese día

### Requirement: Botón en Cargar
Cargar SHALL incluir el botón Cupo. Ese botón SHALL abrir el cupo de la única persona ya elegida en Cargar, sin desplegable de personas. Muestra De este período, Del período anterior, Gozados y Quedan, y es el único lugar de los Días extra, también en cero. De este período y Del período anterior SHALL NOT ser editables. El Reporte SHALL NOT incluir ese botón. Quedan SHALL ser De este período más Del período anterior más los días extra menos los gozados.

#### Scenario: Persona ya elegida
- **WHEN** en Cargar no hay una sola persona marcada y se pulsa Cupo
- **THEN** Cupo no abre y pide elegir una

#### Scenario: Veintiuno de este período
- **WHEN** De este período es 21, Del período anterior es 0 y Gozados es 7
- **THEN** el botón muestra esos números y Quedan 14

#### Scenario: Días extra solo ahí
- **WHEN** se abre el Reporte
- **THEN** no hay botón ni campo para cargar días extra

#### Scenario: Un día extra
- **WHEN** desde el botón se guarda 1 día extra para una persona de ese período
- **THEN** Quedan de esa persona sube un día

### Requirement: Sección Cargar
Cargar SHALL seguir eligiendo personas y un rango de fechas, y SHALL NOT editar el cupo ni los días extra. Guardar SHALL aplicar las reglas de saldo y de cupo en cero.

#### Scenario: Cargar fechas
- **WHEN** una persona tiene saldo y en Cargar se elige un rango dentro de ese saldo
- **THEN** el sistema guarda las vacaciones y el botón Cupo refleja los nuevos gozados

### Requirement: El franco no descuenta
El sistema SHALL NOT contar los días de franco como vacaciones gozadas ni como días del cupo.

#### Scenario: Franco en el período
- **WHEN** una persona tiene francos cargados dentro del período de goce
- **THEN** esos días no reducen el saldo de vacaciones

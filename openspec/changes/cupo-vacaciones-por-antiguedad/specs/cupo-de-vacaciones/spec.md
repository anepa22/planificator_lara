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
- **THEN** ese día descuenta lo que sobra de 2025-2026 y no descuenta los días de este período

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
Si al 31 de diciembre de Y la antigüedad es menor a seis meses, el sistema SHALL otorgar un día por cada veinte días de calendario desde la fecha de ingreso hasta el 31 de diciembre de Y, inclusive, y SHALL descartar la fracción. Esos días SHALL incluir domingos y días sin turno. El sistema SHALL NOT usar los turnos cargados para esta cuenta.

#### Scenario: Menos de seis meses
- **WHEN** la persona ingresó el 1 de octubre de Y
- **THEN** los días de calendario hasta el 31 de diciembre, domingos incluidos, se dividen por veinte y el cupo por antigüedad es la parte entera

#### Scenario: Semana incompleta
- **WHEN** la persona no tiene turno todos los días de la semana
- **THEN** los días sin turno igual entran en la cuenta proporcional

#### Scenario: Seis meses justos
- **WHEN** al 31 de diciembre de Y la antigüedad es de seis meses
- **THEN** el cupo usa la escala de 14 días y no la proporcional

### Requirement: Habilitación de la antigüedad
Los días de antigüedad de un período SHALL mostrarse en De este período y SHALL habilitarse el 1 de octubre siguiente. Hasta esa fecha SHALL NOT entrar en Quedan.

#### Scenario: Quedan tres
- **WHEN** la antigüedad al 31 de diciembre de 2025 es 14, hay 11 vacaciones cargadas desde el 1 de octubre de 2025 y se mira el período que abre el 1 de octubre de 2026
- **THEN** De este período es 14, Del período anterior es 3, Gozados es 0 y Quedan es 3

#### Scenario: Habilitación en octubre de 2027
- **WHEN** al 30 de septiembre de 2027 sobran 2 de 2025-2026 y la antigüedad al 31 de diciembre de 2026 es 14
- **THEN** el 1 de octubre de 2027 el saldo habilitado es 16

### Requirement: Arrastre del saldo
Hasta el 30 de septiembre de 2027, Quedan SHALL ser el sobrante de la antigüedad al 31 de diciembre de 2025 más los días extra, menos las vacaciones cargadas desde el 1 de octubre de 2025. Períodos anteriores a 2025-2026 SHALL NOT sumarse. Desde el 1 de octubre de 2027, lo no gozado SHALL sumarse al período siguiente junto con la antigüedad que se habilita. Un exceso de días gozados SHALL NOT reducir el período siguiente.

#### Scenario: Sin años anteriores
- **WHEN** la persona ingresó el 1 de mayo de 2022 y no tiene vacaciones cargadas desde el 1 de octubre de 2025
- **THEN** en el período que abre el 1 de octubre de 2026, Del período anterior es 14 y Quedan es 14

#### Scenario: El sobrante sigue
- **WHEN** el saldo habilitado el 1 de octubre de 2027 es 16 y en ese período no se carga ningún día
- **THEN** el período que abre el 1 de octubre de 2028 suma esos 16 a la antigüedad que se habilita ese día

### Requirement: Ajuste manual
Quien tenga `vacations:write` SHALL poder sumar, para una persona y un período, una cantidad entera no negativa de días que el arrastre no conoce. Ese ajuste SHALL sumarse a Quedan aunque la antigüedad de ese período todavía no esté habilitada, y SHALL NOT reemplazar ni repetir el saldo arrastrado. El sistema SHALL rechazar un número negativo.

#### Scenario: Saldo inicial
- **WHEN** Del período anterior es 0, De este período es 14 todavía no habilitado y se agregan 3 días extra
- **THEN** Quedan es 3 y esos 3 se pueden cargar

#### Scenario: Día concedido
- **WHEN** Del período anterior es 3 y se agrega 1 día extra
- **THEN** Quedan es 4 y los 14 de este período siguen fuera de Quedan

#### Scenario: No duplica el sobrante
- **WHEN** Del período anterior es 3 y se agregan 3 días extra
- **THEN** Quedan es 6

#### Scenario: Número negativo
- **WHEN** se intenta guardar un ajuste negativo
- **THEN** el sistema rechaza el valor

### Requirement: Saldo del período
El saldo SHALL ser Del período anterior más los días extra menos las vacaciones ya cargadas contra ese saldo. De este período SHALL NOT integrar el saldo hasta el 1 de octubre en que se habilita. El sistema SHALL rechazar una carga nueva cuya cantidad de días, contando el primero y el último, deje el saldo de alguna persona seleccionada por debajo de cero.

#### Scenario: Carga dentro del saldo
- **WHEN** el saldo es 5 y se cargan 5 días corridos
- **THEN** el sistema guarda la carga y el saldo queda en cero

#### Scenario: Carga que se pasa
- **WHEN** el saldo es 2 y se intentan cargar 3 días
- **THEN** el sistema rechaza la carga y no guarda ningún día del rango

### Requirement: Cupo en cero
Si Del período anterior y Días extra son cero, el sistema SHALL rechazar la carga aunque De este período sea mayor que cero, y SHALL NOT guardar ningún día del pedido.

#### Scenario: Menos de veinte días de calendario
- **WHEN** la persona ingresó el 20 de diciembre de 2026, no tiene saldo de 2025-2026 ni días extra, y se intenta cargar un día en el período que abre el 1 de octubre de 2026
- **THEN** el sistema rechaza la carga y no guarda ningún día

#### Scenario: Antigüedad todavía no habilitada
- **WHEN** De este período es 14, Del período anterior es 0, no hay días extra y se intenta cargar un día antes del 1 de octubre de 2027
- **THEN** el sistema rechaza la carga y no guarda ningún día

#### Scenario: Un día extra habilita
- **WHEN** Del período anterior es cero y hay 1 día extra
- **THEN** se puede cargar ese día antes del 1 de octubre de 2027

### Requirement: Botón en Cargar
Cargar SHALL incluir el botón Cupo. Ese botón SHALL abrir, para una persona y un período de goce, De este período, Del período anterior, Gozados y Quedan, y SHALL ser el único lugar donde se cargan los Días extra, también en cero. De este período y Del período anterior SHALL NOT ser editables. El Reporte SHALL NOT incluir ese botón. Quedan SHALL ser Del período anterior más los días extra menos los gozados.

#### Scenario: Tres días del período anterior
- **WHEN** De este período es 14, Del período anterior es 3 y Gozados es 0
- **THEN** el botón muestra esos números y Quedan 3

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

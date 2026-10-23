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
- **THEN** esa vacación descuenta el cupo del período que termina el 30 de septiembre de 2027

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

### Requirement: Arrastre del saldo
El saldo no gozado de un período SHALL sumarse al cupo del período siguiente. Ese saldo SHALL ser el cupo del período menos las vacaciones cargadas en él. El primer período de goce posterior a la fecha de ingreso SHALL NOT tener saldo anterior.

#### Scenario: No tomó vacaciones
- **WHEN** en un período el cupo es 14 y no se carga ningún día
- **THEN** el período siguiente suma esos 14 a los días de la antigüedad

#### Scenario: Sobraron tres
- **WHEN** en un período el cupo es 14 y se cargan 11 días
- **THEN** el período siguiente suma 3 a los días de la antigüedad

#### Scenario: Primer período
- **WHEN** la persona ingresó el 1 de mayo de 2022 y el período es el que abre el 1 de octubre de 2022
- **THEN** el cupo de ese período no suma un saldo anterior

### Requirement: Ajuste manual
Quien tenga `vacations:write` SHALL poder sumar, para una persona y un período, una cantidad entera no negativa de días que el arrastre no conoce. Ese ajuste SHALL sumarse al cupo de ese período y SHALL NOT reemplazar ni repetir el saldo arrastrado. El sistema SHALL rechazar un número negativo.

#### Scenario: Saldo inicial
- **WHEN** al empezar no hay saldo anterior calculado y se agregan 3 días a una persona con 14 por antigüedad
- **THEN** el cupo de ese período es 17

#### Scenario: Día concedido
- **WHEN** se agrega 1 día aparte de la antigüedad y del arrastre
- **THEN** el cupo de ese período sube un día

#### Scenario: Número negativo
- **WHEN** se intenta guardar un ajuste negativo
- **THEN** el sistema rechaza el valor

### Requirement: Saldo del período
El saldo SHALL ser el cupo del período menos los días de vacaciones ya cargados dentro de ese período. El sistema SHALL rechazar una carga nueva cuya cantidad de días, contando el primero y el último, deje el saldo de alguna persona seleccionada por debajo de cero.

#### Scenario: Carga dentro del saldo
- **WHEN** el saldo es 5 y se cargan 5 días corridos
- **THEN** el sistema guarda la carga y el saldo queda en cero

#### Scenario: Carga que se pasa
- **WHEN** el saldo es 2 y se intentan cargar 3 días
- **THEN** el sistema rechaza la carga y no guarda ningún día del rango

### Requirement: Cupo en cero
Si en un período De este período, Del período anterior y Días extra son cero, el sistema SHALL rechazar la carga de vacaciones de esa persona en ese período y SHALL NOT guardar ningún día del pedido.

#### Scenario: Menos de veinte días de calendario
- **WHEN** la persona ingresó el 20 de diciembre de 2026, no tiene arrastre ni días extra, y se intenta cargar un día en el período que abre el 1 de octubre de 2026
- **THEN** el sistema rechaza la carga y no guarda ningún día

#### Scenario: Un día extra habilita
- **WHEN** De este período y Del período anterior son cero y hay 1 día extra
- **THEN** se puede cargar ese día

### Requirement: Botón en Cargar
Cargar SHALL incluir el botón Cupo. Ese botón SHALL abrir, para una persona y un período de goce, De este período, Del período anterior, Gozados y Quedan, y SHALL ser el único lugar donde se cargan los Días extra, también en cero. De este período y Del período anterior SHALL NOT ser editables. El Reporte SHALL NOT incluir ese botón. Quedan SHALL ser el cupo menos los gozados.

#### Scenario: Tres días arrastrados
- **WHEN** la antigüedad del período es 14, el arrastre es 3 y todavía no hay vacaciones cargadas
- **THEN** el botón muestra De este período 14, Del período anterior 3, Gozados 0 y Quedan 17

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

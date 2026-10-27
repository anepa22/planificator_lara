# Spec Delta

## Purpose

Crear locales de venta, editarlos y darlos de alta o de baja sin perder los turnos ya cargados.

## ADDED Requirements

### Requirement: Quién administra
Solo un usuario con `locations:manage` (Gestionar locales) SHALL poder crear, editar, dar de alta y dar de baja un local. Sin ese permiso el sistema SHALL rechazar la operación y SHALL NOT mostrar el ítem Locales. Tener `users:manage` sin `locations:manage` SHALL NOT alcanzar. La consulta de los locales activos SHALL seguir siendo pública.

#### Scenario: Sin el permiso de locales
- **WHEN** un usuario sin `locations:manage` abre el menú o intenta dar de baja un local
- **THEN** no ve el ítem Locales y la baja se rechaza

#### Scenario: Administrar usuarios no alcanza
- **WHEN** un usuario tiene `users:manage` y no tiene `locations:manage`
- **THEN** no ve el ítem Locales y no puede dar de baja un local

#### Scenario: Consulta pública
- **WHEN** una persona sin sesión consulta los locales
- **THEN** recibe los locales activos

### Requirement: Alta
El alta SHALL pedir nombre, color y si el local hace vidriera. El nombre SHALL ser obligatorio y único, sin distinguir mayúsculas. El local SHALL quedar activo y SHALL aparecer para turnos y tareas nuevas.

#### Scenario: Local nuevo
- **WHEN** un administrador crea «Lara 3» con vidriera habilitada
- **THEN** Lara 3 queda activo y se puede elegir en turnos, tareas y vidriera

#### Scenario: Nombre vacío
- **WHEN** se intenta crear un local sin nombre
- **THEN** el sistema rechaza el alta

#### Scenario: Nombre repetido
- **WHEN** ya existe un local llamado «Lara 1» y se intenta crear «lara 1»
- **THEN** el sistema rechaza el alta

### Requirement: Tope de locales
Los locales de venta SHALL ser como máximo 8, estén activos o dados de baja. Franco y vacaciones SHALL NOT contar. Crear un noveno SHALL rechazarse. Dar de baja SHALL NOT liberar un lugar para uno nuevo.

#### Scenario: Noveno local
- **WHEN** ya hay 8 locales de venta, aunque alguno esté de baja, y se intenta crear otro
- **THEN** el sistema rechaza el alta y siguen siendo 8

#### Scenario: Franco no cuenta
- **WHEN** hay 7 locales de venta, y franco y vacaciones también existen
- **THEN** se puede crear un local de venta más

#### Scenario: Reactivar no suma
- **WHEN** ya hay 8 locales de venta y uno está de baja
- **THEN** ese local se puede volver a dar de alta y no se puede crear uno nuevo

### Requirement: Edición
De un local de venta SHALL poder cambiarse el nombre, el color, el orden y si hace vidriera. El identificador SHALL NOT cambiar. Un nombre ya usado por otro local SHALL rechazarse. El nombre nuevo SHALL verse en los turnos ya guardados.

#### Scenario: Cambio de nombre
- **WHEN** se renombra un local que ya tiene turnos
- **THEN** esos turnos muestran el nombre nuevo y siguen en el mismo local

#### Scenario: Sin vidriera
- **WHEN** se desmarca que el local hace vidriera
- **THEN** deja de ofrecerse para una vidriera nueva

### Requirement: Baja
La baja SHALL dejar el local inactivo y SHALL NOT borrarlo ni borrar sus turnos, tareas o vidrieras. Un local inactivo SHALL NOT ofrecerse para un turno, una tarea o una vidriera nuevos.

#### Scenario: Carga nueva
- **WHEN** un local está dado de baja y se intenta cargar un turno ahí
- **THEN** el sistema rechaza el turno y el local no aparece en el selector

#### Scenario: Lo ya guardado
- **WHEN** un local con turnos se da de baja
- **THEN** esos turnos siguen guardados con el nombre del local

### Requirement: Historial en la grilla
Una semana o un mes que ya tiene turnos de un local inactivo SHALL seguir mostrándolos con su nombre. Un período sin turnos de ese local SHALL NOT mostrar su columna.

#### Scenario: Semana con turnos
- **WHEN** se consulta una semana que tiene turnos de un local dado de baja
- **THEN** esos turnos se ven con el nombre del local

#### Scenario: Semana sin turnos
- **WHEN** se consulta una semana sin turnos de ese local
- **THEN** el local no aparece como columna

### Requirement: Volver a dar de alta
Un local inactivo SHALL poder darse de alta de nuevo y SHALL volver a ofrecerse para cargas nuevas.

#### Scenario: Reactivación
- **WHEN** un administrador da de alta un local que estaba de baja
- **THEN** el local vuelve a aparecer para turnos y tareas nuevos

### Requirement: Franco y vacaciones
Franco y vacaciones SHALL permanecer como locales del sistema. El catálogo SHALL NOT crearlos, editarlos ni darlos de baja.

#### Scenario: Baja de franco
- **WHEN** se intenta dar de baja el local franco
- **THEN** el sistema rechaza la operación y franco sigue disponible

#### Scenario: Alta con el mismo nombre
- **WHEN** se intenta crear un local llamado «Vacaciones»
- **THEN** el sistema rechaza el alta

### Requirement: Bitácora
El alta, la edición y la baja de un local SHALL quedar en la bitácora.

#### Scenario: Baja registrada
- **WHEN** un administrador da de baja un local
- **THEN** la bitácora registra esa baja

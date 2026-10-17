# Tablero de tareas

## Purpose
Publicar tareas, asignarlas y seguirlas en un tablero Kanban independiente de los turnos.

## Requirements

### Requirement: Columnas fijas
El tablero SHALL mostrar, de izquierda a derecha, Pendientes, En proceso, Bloqueada, Terminada y Verificada.

#### Scenario: Tarea publicada
- **WHEN** un gestor publica una tarea que está fuera del tablero
- **THEN** la tarea aparece en Pendientes y sin asignar

### Requirement: Lectura pública
El sistema SHALL permitir ver el tablero sin iniciar sesión y SHALL rechazar crear, asignar, mover, modificar o retirar tareas sin el permiso correspondiente.

#### Scenario: Visitante
- **WHEN** una persona sin sesión abre el tablero
- **THEN** ve las tareas publicadas y no puede operarlas

### Requirement: Administración de tareas
Un usuario con `tasks:manage` SHALL poder crear, modificar y eliminar la ficha de una tarea. La ficha SHALL tener título y MAY tener descripción y un local de trabajo. El sistema SHALL rechazar vacaciones y franco como local.

#### Scenario: Alta fuera del tablero
- **WHEN** un gestor crea una tarea
- **THEN** la tarea queda guardada y no aparece en el tablero hasta que se publique

### Requirement: Publicación y retiro
Un usuario con `tasks:manage` SHALL poder publicar una tarea fuera del tablero y SHALL poder retirarla del tablero sin eliminarla.

#### Scenario: Retiro
- **WHEN** un gestor retira una tarea publicada
- **THEN** la tarea deja de verse en el tablero y permanece en la administración

### Requirement: Asignación
Un usuario con `tasks:write` SHALL poder asignarse una tarea Pendiente sin asignar. Un usuario con `tasks:manage` SHALL poder asignarla o reasignarla a cualquier usuario activo que tenga `tasks:appear`. El sistema SHALL rechazar la asignación si ese usuario está de vacaciones en la fecha actual y SHALL ignorar el franco.

#### Scenario: Autoasignación
- **WHEN** un usuario con `tasks:write` se asigna una tarea Pendiente sin asignar y no está de vacaciones hoy
- **THEN** la tarea queda asignada a ese usuario

#### Scenario: Destino sin permiso para aparecer
- **WHEN** un gestor intenta asignar la tarea a un usuario sin `tasks:appear`
- **THEN** el sistema rechaza la asignación

### Requirement: Movimiento
El sistema SHALL exigir que la tarea esté publicada y asignada antes de moverla. Un usuario con `tasks:write` SHALL poder mover solo sus tareas a En proceso, Bloqueada o Terminada. Un usuario con `tasks:manage` SHALL poder mover cualquier tarea, devolverla a Pendientes y pasarla a Verificada.

#### Scenario: Movimiento propio
- **WHEN** el asignado mueve su tarea a En proceso
- **THEN** la tarea cambia a esa columna

#### Scenario: Destino reservado al gestor
- **WHEN** el asignado intenta mover su tarea a Verificada o Pendientes
- **THEN** el sistema rechaza el movimiento

### Requirement: Motivo de bloqueo
El sistema SHALL exigir un motivo no vacío para pasar una tarea a Bloqueada y SHALL conservarlo en el historial al salir de esa columna.

#### Scenario: Bloqueo sin motivo
- **WHEN** alguien mueve una tarea a Bloqueada sin motivo
- **THEN** el sistema rechaza el movimiento

### Requirement: Historial visible
El sistema SHALL mostrar los hitos de Bloqueada, Terminada y Verificada solo mientras el estado actual todavía los implica. El detalle de movimientos SHALL exigir `tasks:history`.

#### Scenario: La tarea vuelve atrás
- **WHEN** una tarea Terminada vuelve a En proceso
- **THEN** el historial deja de mostrar el hito Terminada

### Requirement: Retención de verificadas
Un usuario con `tasks:retention` SHALL poder definir cuántos días permanece visible una tarea Verificada. Cumplido ese plazo, el sistema SHALL retirarla del tablero sin eliminarla.

#### Scenario: Plazo cumplido
- **WHEN** una tarea Verificada supera los días configurados
- **THEN** el sistema la retira del tablero

### Requirement: Aviso por Telegram
El sistema SHALL avisar un cambio de estado o de asignación a los usuarios con `tasks:notify` y un identificador de Telegram, después de confirmar el cambio.

#### Scenario: Cambio de estado
- **WHEN** una tarea asignada pasa a En proceso
- **THEN** reciben el aviso los usuarios configurados para notificación

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

### Requirement: Rol Admin de Stock
El sistema SHALL ofrecer el rol Admin de Stock con el permiso `tasks:stock` y sin permisos de horarios, vacaciones, usuarios, retención ni avisos. La autorización SHALL usar ese permiso y no el nombre del rol.

#### Scenario: Rol asignado
- **WHEN** un administrador asigna el rol Admin de Stock a un usuario
- **THEN** ese usuario obtiene `tasks:stock` y no obtiene `tasks:manage`

### Requirement: Marca de stock
Una tarea SHALL poder marcarse como de stock. Las tareas ya existentes SHALL quedar sin esa marca. El tablero SHALL mostrar la etiqueta Stock en cada tarea marcada y SHALL permitir filtrar por ella.

#### Scenario: Tarea anterior
- **WHEN** se abre el tablero después de incorporar la marca
- **THEN** las tareas que ya existían no muestran la etiqueta Stock

#### Scenario: Filtro
- **WHEN** alguien filtra el tablero por Stock
- **THEN** solo ve las tareas marcadas como stock

### Requirement: Alta de una tarea de stock
Un usuario con `tasks:stock` SHALL poder crear solo tareas de stock. Un usuario con `tasks:manage` SHALL poder marcar o desmarcar stock en cualquier tarea. Un usuario con `tasks:stock` SHALL NOT poder quitar la marca ni modificar una tarea que no sea de stock.

#### Scenario: Alta del admin de stock
- **WHEN** un usuario con `tasks:stock` y sin `tasks:manage` crea una tarea
- **THEN** la tarea queda guardada, marcada como stock y fuera del tablero hasta que se publique

#### Scenario: Intento de desmarcar
- **WHEN** un usuario con `tasks:stock` y sin `tasks:manage` intenta quitar la marca de stock
- **THEN** el sistema rechaza el cambio

### Requirement: Administración de tareas de stock
Un usuario con `tasks:stock` SHALL poder editar, eliminar, publicar y retirar cualquier tarea de stock. El sistema SHALL rechazar esas operaciones sobre una tarea que no sea de stock.

#### Scenario: Publicar una tarea de stock
- **WHEN** un usuario con `tasks:stock` publica una tarea de stock que está fuera del tablero
- **THEN** la tarea aparece en Pendientes, sin asignar y con la etiqueta Stock

#### Scenario: Tarea que no es de stock
- **WHEN** un usuario con `tasks:stock` y sin `tasks:manage` intenta editar, eliminar, publicar o retirar una tarea sin la marca
- **THEN** el sistema rechaza la operación

### Requirement: Asignación de tareas de stock
Un usuario con `tasks:stock` SHALL poder asignar o reasignar cualquier tarea de stock publicada a un usuario activo con `tasks:appear`. El sistema SHALL rechazar el destino si está de vacaciones hoy y SHALL rechazar la operación si la tarea no es de stock.

#### Scenario: Asignación válida
- **WHEN** un usuario con `tasks:stock` asigna una tarea de stock publicada a un usuario activo con `tasks:appear` que no está de vacaciones hoy
- **THEN** la tarea queda asignada a ese usuario

#### Scenario: Destino de vacaciones
- **WHEN** un usuario con `tasks:stock` asigna una tarea de stock a un usuario que está de vacaciones hoy
- **THEN** el sistema rechaza la asignación

### Requirement: Movimiento de tareas de stock
Un usuario con `tasks:stock` SHALL poder mover cualquier tarea de stock publicada y asignada a cualquier columna, incluidas Pendientes y Verificada. El sistema SHALL rechazar el movimiento si la tarea no es de stock. Un usuario con `tasks:write` SHALL seguir pudiendo mover sus propias tareas de stock solo a En proceso, Bloqueada o Terminada.

#### Scenario: Verificar una tarea de stock
- **WHEN** un usuario con `tasks:stock` mueve una tarea de stock asignada a Verificada
- **THEN** la tarea cambia a esa columna

#### Scenario: El asignado mueve la suya
- **WHEN** el asignado, con `tasks:write` y sin `tasks:stock`, mueve su tarea de stock a En proceso
- **THEN** la tarea cambia a esa columna

#### Scenario: Mover una tarea ajena al stock
- **WHEN** un usuario con `tasks:stock` y sin `tasks:manage` intenta mover una tarea sin la marca
- **THEN** el sistema rechaza el movimiento

### Requirement: Historial de una tarea de stock
Un usuario con `tasks:stock` SHALL poder ver el detalle de movimientos de una tarea de stock. El sistema SHALL rechazar ese detalle si la tarea no es de stock y el usuario no tiene `tasks:history`.

#### Scenario: Detalle de una tarea de stock
- **WHEN** un usuario con `tasks:stock` abre el detalle de movimientos de una tarea de stock
- **THEN** el sistema muestra ese detalle

#### Scenario: Detalle de otra tarea
- **WHEN** un usuario con `tasks:stock` y sin `tasks:history` abre el detalle de una tarea sin la marca
- **THEN** el sistema rechaza la consulta

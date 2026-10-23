# Proposal

## Why

El stock necesita dar de alta, asignar y mover sus tareas sin el resto de permisos de un supervisor. Hoy `tasks:manage` administra todas las tareas del tablero y no hay forma de marcar una tarea como de stock.

## What Changes

- Nuevo rol **Admin de Stock**, con el permiso `tasks:stock`. Quien lo tenga crea, edita, publica, retira, asigna y mueve cualquier tarea de stock, incluidas las columnas Pendientes y Verificada.
- Ese permiso no alcanza para las tareas que no son de stock, ni para horarios, vacaciones, usuarios, retención ni avisos.
- Una tarea puede marcarse como stock. Las existentes quedan sin esa marca. El tablero actual las muestra con la etiqueta **Stock** y se pueden filtrar.
- Quien tiene `tasks:manage` sigue administrando todas las tareas y puede marcar o desmarcar stock. Quien solo tiene `tasks:write` sigue tomando y moviendo sus propias tareas, sean de stock o no.

## Capabilities

### New Capabilities

- Ninguna.

### Modified Capabilities

- `tablero-de-tareas`: la administración, la asignación y el movimiento distinguen las tareas de stock, y el tablero las muestra marcadas.

## Impact

- Permiso y rol nuevos, sembrados al arrancar la API y en un SQL de migración. Se asigna el rol desde Usuarios.
- Columna en `tasks`, DTOs de alta y edición, y las operaciones de `TaskService` que hoy exigen `tasks:manage`.
- Menú **Administrar tareas**, el formulario de la ficha y el tablero (`TaskBoard`), que agregan la marca y el filtro.
- `tasks:manage` no cambia de alcance: sigue pudiendo operar cualquier tarea.

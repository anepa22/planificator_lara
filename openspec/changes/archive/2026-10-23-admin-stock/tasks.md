# Tasks

## 1. Permiso y marca

- [x] 1.1 Agregar `tasks:stock` y el rol `stock` (Admin de Stock) en `AuthCleanup` y en `sql/MIGRA_ADMIN_STOCK.sql`, con la columna `tasks.stock boolean NOT NULL DEFAULT false`. Verificar que el SQL y el arranque son idempotentes y que el rol no incluye `tasks:manage`.
- [x] 1.2 Llevar el booleano en `Task`, los DTOs de alta y edición y el mapeo del repositorio. Verificar que una fila sin valor explícito se lee como no stock.

## 2. Operaciones

- [x] 2.1 Permitir crear, editar, borrar, publicar, retirar, asignar, desasignar y mover cuando hay `tasks:manage`, o cuando hay `tasks:stock` y la tarea está marcada. El alta de quien solo tiene `tasks:stock` queda marcada; quitar la marca o tocar una tarea sin marca se rechaza. Verificar con pruebas de `TaskService`: alta forzada, rechazo al desmarcar, publicación y movimiento a Verificada de una tarea de stock, y rechazo al mover una tarea sin marca.
- [x] 2.2 Abrir el detalle de movimientos con `tasks:history`, o con `tasks:stock` si la tarea está marcada. Verificar que un usuario solo con `tasks:stock` ve el detalle de una tarea de stock y no el de una tarea sin marca.
- [x] 2.3 Conservar las reglas actuales de `tasks:write`, `tasks:appear` y vacaciones. Verificar que un asistente mueve su tarea de stock solo a En proceso, Bloqueada o Terminada, y que asignar a alguien de vacaciones sigue rechazado.

## 3. Pantalla

- [x] 3.1 Mostrar Administrar tareas con `tasks:manage` o `tasks:stock`. En el formulario, el gestor puede marcar y desmarcar; el perfil de stock solo lista tareas marcadas y no apaga la marca. Verificar en la pantalla que una tarea creada por ese perfil queda etiquetada Stock y que no aparece una tarea sin marca.
- [x] 3.2 Mostrar la etiqueta Stock en la tarjeta y agregar el filtro Stock junto a los filtros del tablero, arrancando en Todas. Verificar que el filtro deja solo las marcadas y que sin filtro se ven todas.

## Workflow follow-up

- Archivar el cambio cuando se revise la implementación.

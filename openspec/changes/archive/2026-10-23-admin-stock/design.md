# Design

## Context

Ver `proposal.md`. Hoy `TaskService` exige `tasks:manage` para crear, editar, borrar, publicar, retirar y desasignar, y trata como gestor a quien tiene ese permiso al asignar o mover. La ficha no tiene categoría. El rol se siembra en `AuthCleanup` y también hay un SQL manual. La pantalla de usuarios ya asigna roles y permisos sueltos.

## Goals / Non-Goals

**Goals:**

- Un permiso nuevo alcanza para operar cualquier tarea marcada como stock y ninguna otra.
- La marca viaja en la ficha y se ve en el tablero que ya existe.

**Non-Goals:**

- Un tablero distinto, un local nuevo o un flujo de estados distinto.
- Darle a este rol horarios, vacaciones, usuarios, retención o Telegram.

## Decisions

1. **Permiso `tasks:stock`, rol `stock` llamado Admin de Stock.** No se reutiliza `tasks:manage`, porque ese permiso sigue significando todas las tareas. El rol solo lleva `tasks:stock`. Quien ya tiene `tasks:manage` no necesita el permiso nuevo. Alternativa descartada: un filtro por nombre de rol. El resto del sistema autoriza por permiso.

2. **Columna `tasks.stock boolean NOT NULL DEFAULT false`.** Las filas actuales quedan en falso. El alta y la edición reciben el booleano. Si el actor solo tiene `tasks:stock`, el alta fuerza `true` y la edición rechaza pasarlo a `false` o tocar una fila en `false`. Si tiene `tasks:manage`, el valor del pedido se respeta. Alternativa descartada: un texto libre de categoría. Con un booleano el alcance del permiso queda cerrado.

3. **La operación se permite si hay `tasks:manage`, o si hay `tasks:stock` y la tarea está marcada.** Crear, editar, borrar, publicar, retirar, asignar, desasignar y mover usan esa regla. Asignar conserva las reglas actuales de usuario activo, `tasks:appear` y vacaciones de hoy. Mover conserva el motivo obligatorio de Bloqueada. Quien solo tiene `tasks:write` no entra en esta regla y sigue limitado a sus tareas y a En proceso, Bloqueada o Terminada, aunque la tarea sea de stock.

4. **El detalle de movimientos se abre con `tasks:history`, o con `tasks:stock` si la tarea está marcada.** No se le da `tasks:history` al rol, para no abrir el historial del resto del tablero.

5. **La UI abre Administrar tareas con `tasks:manage` o `tasks:stock`.** En la lista, el perfil de stock solo ve tareas marcadas y no puede apagar la marca. El gestor ve el control y puede cambiarlo. En el tablero, cada tarjeta marcada muestra la etiqueta Stock y el filtro existente suma la opción Stock. Sin filtro se ven todas, como ahora.

## Risks / Trade-offs

- [Un gestor desmarca una tarea y el admin de stock deja de verla] → Es el efecto pedido: la marca define el alcance. El gestor es quien puede devolverla.
- [La columna nueva llega a dev al arrancar la API] → El `ALTER` es idempotente y el default deja las tareas actuales fuera de stock. Revertir el código no borra la columna; se puede dropear a mano sin borrar tareas.

## Migration Plan

1. Al arrancar, crear el permiso, el rol, la relación rol-permiso y la columna si no existen. El mismo cambio queda en un SQL manual.
2. Desplegar API y web juntos: el tablero viejo ignora un campo de más, pero la marca solo se opera con la web nueva.
3. Asignar el rol desde Usuarios. No se asigna solo.

## Open Questions

Ninguna. El filtro arranca en Todas y la etiqueta es el texto Stock.

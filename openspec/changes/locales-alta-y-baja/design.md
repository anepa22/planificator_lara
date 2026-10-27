# Design

## Context

La tabla `locations` ya tiene nombre, color, `color_soft`, orden, `is_active` y `supports_vidriera`. `GET /api/locations` es público y solo devuelve activos. No hay alta ni edición. La semana lee `v_shifts_week`, que trae el nombre del local. Franco (`franco`) y vacaciones (`vacaciones`) están fijos en el código y la vidriera ya rechaza un local inactivo. Ver proposal.md para el motivo.

## Goals / Non-Goals

**Goals:**

- Reusar `is_active` para la baja y el identificador estable para no cortar turnos, tareas ni vidrieras.
- Una sola pantalla para quien tiene `locations:manage`.
- La grilla muestra un local inactivo solo cuando el período consultado tiene turnos suyos.

**Non-Goals:**

- Borrar locales o editar franco y vacaciones.
- Cambiar el cupo, el reporte de vacaciones o la regla de vidriera ya vigente.

## Decisions

1. La baja pone `is_active` en falso. No hay `DELETE`. Un local con turnos, tareas o vidrieras no se puede borrar sin dejar referencias rotas. La alternativa de borrado lógico en otra tabla duplica la columna que ya existe.

2. El identificador se genera una vez, en minúsculas y sin espacios, y no se edita. El nombre sí. Así un cambio de nombre se ve en los turnos ya guardados, que leen el nombre por el id. Si el identificador choca con uno existente, el alta se rechaza.

3. El color lo elige quien crea el local. El servidor guarda ese color y deriva `color_soft`. La pantalla no pide los dos.

4. El permiso es `locations:manage`, con el nombre Gestionar locales. `users:manage` no alcanza. Al crearlo, se lo damos una sola vez a quien ya administraba usuarios y al rol admin, para que no pierdan la pantalla. Después se puede sacar de un rol sin volver a otorgarlo en el próximo arranque.

5. `GET /api/locations` sigue devolviendo solo activos y sigue siendo público. La pantalla de administración usa otra lectura, con `locations:manage`, que incluye los inactivos y excluye franco y vacaciones. Crear, editar y cambiar el alta o la baja exigen el mismo permiso y quedan en la bitácora.

6. La grilla arma sus columnas con los locales activos más cualquier local que tenga turnos en el período que se está mirando. Un inactivo sin turnos en esa semana no aparece. El selector de un turno, una tarea o una vidriera nuevos usa solo activos. El servicio rechaza un id inactivo aunque el cliente lo envíe.

7. El nombre se compara sin mayúsculas y sin espacios de más. También se rechaza un nombre que coincida con Franco o Vacaciones. El orden de uno nuevo es el último. Después se puede cambiar.

8. Destildar la vidriera no borra las marcas ya guardadas. Solo impide marcar una fecha nueva, que es lo que la spec de vidriera ya exige para un local sin esa habilitación.

## Risks / Trade-offs

- [La vista `v_shifts_week` no está en el repositorio y podría filtrar `is_active`] → Al implementar, confirmar que un local inactivo sigue trayendo nombre. Si filtra, quitar ese filtro.
- [Un índice único de nombre puede fallar si hoy hay dos locales que solo difieren en mayúsculas] → Revisar los nombres antes de crearlo. Si hay choque, no se crea el índice y el rechazo queda en el servicio.
- [Una baja mal hecha esconde el local de las cargas nuevas enseguida] → La pantalla pide confirmación. El historial del período con turnos sigue visible y se puede volver a dar de alta.

## Migration Plan

No hay tabla nueva ni datos para migrar. El despliegue de la API alcanza para crear y dar de baja. La pantalla llega con la web. Si se vuelve a la versión anterior, los locales que quedaron activos siguen listándose y los dados de baja siguen ocultos, porque esa lectura ya existía. No hace falta un script manual: el índice, si se agrega, se crea al arrancar la API de forma idempotente.

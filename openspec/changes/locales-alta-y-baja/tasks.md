# Tasks

## 1. Alta, edición y baja

- [x] 1.1 Agregar lectura de todos los locales de venta, alta y actualización en el repositorio, y verificar con un test que un inactivo se lista para administración y que el identificador no cambia al renombrar.
- [x] 1.2 Implementar las reglas de nombre obligatorio y único, color, vidriera, orden, identificador estable, rechazo de franco y vacaciones, y bitácora. Verificar con tests de alta, nombre vacío, nombre repetido, baja y vuelta a alta.
- [x] 1.3 Exponer la lectura de administración y las escrituras solo con `locations:manage`, dejar `GET /api/locations` público y solo con activos, y verificar que sin permiso, incluido `users:manage` solo, responde 403.
- [x] 1.4 Crear al arrancar, de forma idempotente, el índice único de nombre en minúsculas si los nombres actuales no chocan. Verificar que un segundo arranque no falla.
- [x] 1.5 Crear el permiso Gestionar locales al arrancar, una sola vez, y dárselo a quien ya tenía `users:manage` y al rol admin. Un arranque posterior no vuelve a otorgarlo.
- [x] 1.6 Rechazar el alta y la reactivación cuando ya hay 8 locales de venta activos. Franco y vacaciones no cuentan. Verificar el rechazo y que la pantalla no ofrece crear ni reactivar en ese caso.

## 2. Cargas nuevas

- [x] 2.1 Rechazar un turno nuevo en un local inactivo y verificar el test de ese rechazo, sin cambiar el cupo ni el franco.
- [x] 2.2 Rechazar una tarea nueva en un local inactivo y verificar el test.
- [x] 2.3 Verificar con un test que una vidriera nueva se rechaza si el local está inactivo o ya no tiene vidriera habilitada, y que una marca ya guardada sigue consultable.

## 3. Grilla

- [x] 3.1 Confirmar que `v_shifts_week` sigue trayendo el nombre de un local inactivo. Si filtra por activo, quitar ese filtro y verificar que el nombre sigue llegando.
- [x] 3.2 Armar las columnas de la semana y del mes con los locales activos más los que tengan turnos en el período, y verificar que una semana sin esos turnos no muestra la columna y una semana con turnos sí los muestra.

## 4. Pantalla

- [x] 4.1 Agregar Locales al menú, visible solo con `locations:manage`, con alta, edición, baja con confirmación y vuelta a alta. Verificar que franco y vacaciones no aparecen y que sin el permiso el ítem no está.

## Workflow follow-up

- Archivar el cambio cuando el comportamiento esté revisado.

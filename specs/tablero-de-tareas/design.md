# Diseño: tablero de tareas

## Datos

### Vínculo usuario-persona
Agregar `app_users.person_id UUID NULL UNIQUE REFERENCES people(id)`.

### Tareas
Tabla `tasks`:

- `id UUID` PK
- `title VARCHAR` obligatorio
- `description TEXT`
- `status VARCHAR` con valores `PENDING`, `IN_PROGRESS`, `BLOCKED`, `DONE`,
  `VERIFIED`
- `block_reason TEXT` con el motivo vigente cuando está bloqueada
- `assignee_person_id UUID NULL REFERENCES people(id)`
- `on_board BOOLEAN NOT NULL DEFAULT FALSE`
- timestamps

Una tarea fuera del tablero tiene `on_board = false`. Publicarla la deja
`on_board = true`, `status = PENDING` y sin asignar.

### Historial
Tabla `task_history`:

- tarea, usuario actor, acción, estado anterior/nuevo
- persona asignada anterior/nueva
- motivo de bloqueo
- fecha

La bitácora general también registra altas, bajas y cambios relevantes.

## API

- `GET /api/tasks/board`: público; tareas activas en tablero.
- `GET /api/tasks`: Supervisor/Admin; listado completo del ABM.
- `POST /api/tasks`: Supervisor/Admin.
- `PUT /api/tasks/{id}`: modificación de ficha por Supervisor/Admin.
- `DELETE /api/tasks/{id}`: Supervisor/Admin.
- `POST /api/tasks/{id}/publish`: publicar en Pendientes.
- `POST /api/tasks/{id}/assign`: asignar. Personal solo puede usar su propia
  persona; Supervisor/Admin cualquiera.
- `POST /api/tasks/{id}/unassign`: Supervisor/Admin; vuelve a Pendientes.
- `POST /api/tasks/{id}/move`: movimiento de estado; motivo obligatorio si el
  destino es `BLOCKED`.
- `POST /api/tasks/{id}/retire`: Supervisor/Admin; fuera del tablero.

El backend obtiene usuario/persona desde JWT y aplica autorización por recurso.

## Permisos
- `tasks:write`: operar tareas propias y autoasignarse.
- `tasks:manage`: ABM y operación total.
- Rol nuevo `personal`: `tasks:write`.
- Rol interno `editor` se muestra como Supervisor y recibe `tasks:manage`.
- `admin` recibe ambos permisos.

## Vacaciones
Antes de asignar se consulta `shifts` por `person_id`, `work_date = CURRENT_DATE`
y `location_id = 'vacaciones'`. El rechazo ocurre dentro del servicio antes de
actualizar la tarea. `franco` no se consulta.

## Frontend
- Nueva vista `tasks` junto a Semanal/Mensual.
- `TaskBoard`: cinco columnas, tarjetas y drag/drop.
- Para evitar una dependencia adicional, el MVP usa la API HTML5 de drag/drop;
  las acciones también quedan disponibles por botones/select para accesibilidad.
- `TaskBlockReasonModal`: solicita motivo antes de confirmar Bloqueada.
- `TaskAdminModal`: ABM visible solo con `tasks:manage`.
- `UsersModal`: etiqueta Editor → Supervisor y selector de persona vinculada.
- Las mutaciones refrescan el tablero; ante error se conserva el estado real
  del servidor y se muestra el mensaje.

## Concurrencia
Las operaciones comprueban el estado/asignado actual en backend. El cliente no
aplica cambios optimistas permanentes: refresca luego de cada mutación.

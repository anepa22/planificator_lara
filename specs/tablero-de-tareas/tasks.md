# Tasks: tablero de tareas

## Especificación
- [x] Definir alcance, roles, estados y transiciones.
- [x] Definir modelo, API, permisos y validación de vacaciones.

## Base de datos
- [x] Crear migración de vínculo `app_users.person_id`.
- [x] Crear roles/permisos Personal, Supervisor y Admin.
- [x] Crear tablas `tasks` y `task_history`.
- [ ] Ejecutar `MIGRA_TAREAS.sql` en cada ambiente.

## Backend
- [x] Dominio, DTO y repositorio de tareas.
- [x] Servicio con reglas de asignación, movimiento y autorización.
- [x] Validar vacaciones al asignar.
- [x] API pública de tablero y API protegida de ABM/acciones.
- [x] Vincular persona en ABM de usuarios.
- [x] Auditoría de tareas.
- [x] Pruebas de reglas principales del servicio.

## Frontend
- [x] Nueva vista Tablero en la navegación.
- [x] Cinco columnas en el orden acordado.
- [x] Tarjetas visibles para visitante.
- [x] Autoasignación y movimiento de tareas propias para Personal.
- [x] Drag/drop y controles alternativos.
- [x] Motivo obligatorio al bloquear.
- [x] ABM y operación total para Supervisor/Admin.
- [x] Selector de persona vinculada en Usuarios.
- [x] Cambiar etiqueta Editor por Supervisor.

## Verificación
- [x] Compilar y probar backend.
- [x] Ejecutar lint y build frontend.
- [ ] Verificar criterios de aceptación manuales.

## Fuera de alcance
- [x] Reportes, métricas y dashboards.

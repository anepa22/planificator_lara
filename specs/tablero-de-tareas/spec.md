# Tablero de tareas

## Objetivo
Agregar al planificador un módulo Kanban independiente de los turnos. Permite
publicar tareas, asignarlas al equipo y seguirlas hasta su verificación.

## Columnas
El orden fijo de izquierda a derecha es:

1. Pendientes
2. En proceso
3. Bloqueada
4. Terminada
5. Verificada

Una tarea creada en el ABM no aparece en el tablero hasta que Supervisor o
Administrador la publique en Pendientes.

## Roles

### Visitante (sin login)
- Ve el calendario y todo el tablero.
- No puede crear, asignar, mover, modificar ni retirar tareas.

### Personal
- Es un usuario vinculado a una persona del equipo.
- Ve todas las tareas del tablero.
- Puede asignarse una tarea sin asignar que esté en Pendientes.
- Puede mover únicamente sus tareas asignadas entre En proceso, Bloqueada y
  Terminada.
- No puede modificar la ficha de ninguna tarea.
- No puede asignar tareas a terceros, desasignar, volver a Pendientes, pasar a
  Verificada ni retirar tareas del tablero.

### Supervisor
- Reemplaza el nombre visible del rol Editor existente; conserva su id interno.
- Puede ver el ABM de tareas y realizar altas, modificaciones y bajas.
- Puede publicar tareas en Pendientes.
- Puede asignar o reasignar cualquier tarea a cualquier persona.
- Puede desasignar y devolver una tarea a Pendientes.
- Puede modificar y mover cualquier tarea.
- Puede pasar tareas a Verificada y retirarlas del tablero.

### Administrador
- Tiene todas las facultades del Supervisor y la administración general.

## Personas y usuarios
- Las personas del equipo y las cuentas de usuario siguen siendo conceptos
  separados, vinculados de forma opcional uno a uno.
- Una persona necesita usuario vinculado para operar el tablero.
- Una cuenta administrativa puede existir sin ser una persona del equipo.
- El alta/edición de usuarios permite seleccionar la persona vinculada.

## ABM de tareas
- Solo Supervisor y Administrador ven el ABM.
- Campos mínimos: título y descripción.
- Alta: crea una tarea fuera del tablero.
- Modificación: cambia su ficha.
- Baja: elimina la tarea.
- Publicar: coloca una tarea fuera del tablero en Pendientes, sin asignar.
- Retirar: saca una tarea del tablero sin eliminarla del ABM.

## Asignación
- Personal puede asignarse a sí mismo una tarea Pendiente sin asignar.
- Supervisor y Administrador pueden asignar/reasignar a cualquier persona.
- Al asignar se valida si la persona está de vacaciones en la fecha actual.
- Si está de vacaciones, la asignación se rechaza con un mensaje claro.
- El franco no impide asignar.
- Una vez asignada, solo Supervisor o Administrador pueden desasignarla y
  devolverla a Pendientes.

## Movimiento
- El tablero permite arrastrar tarjetas entre columnas.
- Personal solo puede arrastrar tareas que tenga asignadas.
- Personal no puede arrastrar a Pendientes ni Verificada.
- Supervisor y Administrador pueden mover cualquier tarjeta.
- Entrar a Bloqueada exige un motivo no vacío.
- Al salir de Bloqueada, el motivo queda en el historial de movimientos.
- Verificada solo puede ser destino de Supervisor o Administrador.

## Seguridad
- Las reglas se validan en backend, no solo ocultando controles.
- Lectura del tablero: pública.
- Operación propia: permiso de tareas del rol Personal.
- Operación total y ABM: Supervisor y Administrador.
- Todos los cambios quedan registrados en bitácora.

## Fuera de alcance
- Reportes, métricas y dashboards.
- Cambios en el funcionamiento de turnos, vacaciones, francos o vidrieras.
- Fechas límite, prioridades, adjuntos, comentarios y subtareas.

## Criterios de aceptación
- [ ] Visitante ve todo el tablero y no puede operarlo.
- [ ] Personal puede asignarse una Pendiente si no está de vacaciones hoy.
- [ ] Personal mueve solo sus tareas y no llega a Verificada.
- [ ] Personal no puede desasignar ni volver una tarea a Pendientes.
- [ ] Supervisor/Admin realizan el ABM y publican tareas en Pendientes.
- [ ] Supervisor/Admin asignan, reasignan, desasignan y mueven cualquier tarea.
- [ ] Solo Supervisor/Admin pasan a Verificada y retiran del tablero.
- [ ] Bloquear exige motivo.
- [ ] El rol Editor se muestra como Supervisor.
- [ ] Usuario y persona pueden vincularse uno a uno.

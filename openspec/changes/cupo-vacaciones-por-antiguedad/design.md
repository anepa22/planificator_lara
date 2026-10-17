# Design

## Context

Las vacaciones son turnos de día completo con local `vacaciones`, colgados de `app_users`. No hay fecha de ingreso, días acordados ni acumulación. La carga pasa por `VacationModal` y el alta de turnos. Ver `proposal.md` para el motivo.

## Goals / Non-Goals

**Goals:**

- Calcular en el servidor el cupo de cada período: ley + acordados + acumulados − gozados.
- Guardar los días acordados antes de asignar fechas.
- Mostrar el saldo en el modal y rechazar en el alta cualquier período que se pase.

**Non-Goals:**

- Cambiar el reporte ni su PDF.
- Un cupo distinto para francos.
- Hacer vencer los días acumulados.

## Decisions

- El período `Y` va del 1 de octubre de `Y` al 30 de septiembre de `Y+1`. La antigüedad de ese período es la cumplida al 31 de diciembre de `Y`.
- Los días hábiles son los lunes a sábado del año calendario `Y`. Un día trabajado es una fecha de ese año con al menos un turno que no sea vacaciones ni franco. Si no llegan a la mitad, los días de ley son la división entera de los trabajados por 20. Si llegan, vale la escala por antigüedad al 31 de diciembre de `Y`. Quien ingresa el 1 de julio y trabaja el resto del año queda del lado de la escala.
- La fecha de ingreso vive en `app_users`. Los días acordados viven en una fila por usuario y período, no en el turno. Alternativa descartada: reemplazar la ley con el acuerdo. El acuerdo se suma.
- El saldo no se guarda. Se recorre desde el primer período posterior al ingreso: lo no usado de uno entra al siguiente. Quitar una vacación recalcula solo.
- Los gozados de un período son los turnos `vacaciones` con `work_date` dentro de ese período.
- La consulta del saldo y la carga del acuerdo exigen `vacations:write`. El alta de turnos de vacaciones repite la validación.
- Un rango que cruza el 1 de octubre se parte en dos períodos y cada uno se valida contra su propio saldo.
- Sin fecha de ingreso, el alta de vacaciones responde error. El resto de los turnos no exige esa fecha.

## Risks / Trade-offs

- [Personas ya activas sin fecha de ingreso] → No se les pueden cargar vacaciones nuevas hasta completarla. La grilla actual no se borra.
- [Vacaciones ya cargadas entre enero y septiembre] → Quedan en el período que empezó el 1 de octubre anterior, no en el año calendario.
- [El navegador podría saltear la validación visual] → El rechazo queda en el alta del turno.
- [Dos cargas simultáneas] → La segunda revalida contra lo ya guardado.

## Migration Plan

- Agregar `app_users.hire_date` nula y la tabla de días acordados por usuario y período. No hay backfill.
- Desplegar API y web juntos. Si se vuelve atrás, las columnas pueden quedar sin usarse.

## Open Questions

Ninguna. El período, el proporcional, la acumulación sin vencimiento y la suma de los días acordados quedaron fijados.

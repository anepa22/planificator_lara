# Design

## Context

Las vacaciones son turnos del local `vacaciones`. La carga elige personas y un rango de fechas. `app_users` no tiene fecha de ingreso. El reporte abre en el año calendario y lista tramos, sin cupo. Ver `proposal.md` para el motivo.

## Goals / Non-Goals

**Goals:**

- Calcular el cupo del período de goce con la escala y la regla proporcional de la spec.
- Guardar la fecha de ingreso y el ajuste manual de un período.
- Arrastrar el saldo no gozado al período siguiente.
- Mostrar De este período, Del período anterior, Gozados y Quedan desde un botón dentro de Cargar y, solo para consultar, en el reporte y el PDF. Cargar los días extra solo desde ese botón.
- Rechazar el rango que se pasa.

**Non-Goals:**
- Volver a cargar a mano el saldo que ya arrastra el sistema.
- Editar días extra desde el reporte.
- Contar francos o días efectivamente trabajados fuera del calendario de la relación laboral.

## Decisions

1. **Período identificado por el año Y de su 1 de octubre.** Una fecha pertenece al período Y si cae entre el 1 de octubre de Y y el 30 de septiembre de Y+1. La antigüedad se mide al 31 de diciembre de Y.
2. **Fecha de ingreso en `app_users.hire_date`**, nullable. Quien administra usuarios la edita. Sin valor, la carga de vacaciones de esa persona se rechaza.
3. **El arrastre no se guarda.** Para el período Y, el cupo es la escala o la proporcional, más el saldo no gozado de Y−1, más el ajuste manual de Y. El saldo no gozado es cupo menos vacaciones cargadas. La cadena empieza en el primer período cuyo 1 de octubre es posterior al ingreso: ese no tiene saldo anterior. Así, si las vacaciones viejas ya están en la grilla, los 3 días que sobran aparecen solos.
4. **Ajuste manual en `vacation_adjustments (user_id, period_year, days)`**, entero no negativo. La ausencia de fila vale cero. Solo `vacations:write` lo escribe. Sirve para el saldo inicial que la grilla no puede reconstruir y para un día concedido aparte. No es una copia del arrastre.
5. **Proporcional sobre días de calendario.** Desde la fecha de ingreso hasta el 31 de diciembre de Y, inclusive, se divide por 20 y se descarta la fracción. Entran domingos y días sin turno. No se cuentan los turnos cargados: esa cuenta daría menos días a quien no trabaja toda la semana.
6. **Días gozados = turnos con local `vacaciones` cuya fecha cae en el período.** Cada turno cuenta un día. El franco no se cuenta. El saldo es el cupo menos esos días.
7. **La carga valida a cada persona del rango antes de guardar.** Si alguna quedaría con saldo negativo, no se persiste ningún día del pedido. Si De este período, Del período anterior y Días extra son cero, esa persona no puede cargar en ese período. El reemplazo de un turno que ya era vacaciones de esa persona en ese día no descuenta dos veces.
8. **El menú no gana un ítem.** Cargar sigue eligiendo personas y fechas, y tiene el botón Cupo. Ese botón abre De este período, Del período anterior, Gozados y Quedan, y ahí se escriben los Días extra, también en cero. De este período y Del período anterior no se editan. El reporte y el PDF muestran los mismos números, no tienen el botón y no editan. Si el ajuste es cero, no muestran la línea Días extra. Quedan es el cupo menos los gozados. El reporte abre en el período de goce en curso. Un rango libre mantiene los tramos y no arma un saldo único.

## Risks / Trade-offs

- [La proporcional cuenta días sin turno] → Es la cuenta que más días le da a quien no trabaja toda la semana. Quedó elegido así.
- [Usuarios ya cargados sin fecha de ingreso] → La columna nace nula. Hasta completarla, esa persona no admite vacaciones nuevas. Las vacaciones ya cargadas siguen visibles.
- [Un rango que cruza el 1 de octubre] → Parte de los días caen en un período y el resto en el siguiente. La validación reparte el rango por período y exige saldo en cada uno.

## Migration Plan

- `AuthCleanup` aplica, de forma idempotente, `app_users.hire_date` y la tabla `vacation_adjustments`.
- El mismo SQL queda en `backend/planificator/sql/`, para poder leerlo. Al arrancar la API se aplica solo; no hace falta ejecutarlo a mano.
- Rollback: quitar la validación del cupo y dejar de leer las columnas. Borrarlas es un paso aparte, como con el cupo anterior.

## Open Questions

Ninguna. El arrastre automático y el ajuste manual quedaron definidos en la spec.

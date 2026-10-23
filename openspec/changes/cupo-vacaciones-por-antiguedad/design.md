# Design

## Context

Las vacaciones son turnos del local `vacaciones`. La carga elige personas y un rango de fechas. `app_users` no tiene fecha de ingreso. El reporte abre en el año calendario y lista tramos, sin cupo. Ver `proposal.md` para el motivo.

## Goals / Non-Goals

**Goals:**

- Calcular el cupo del período de goce con la escala y la regla proporcional de la spec.
- Guardar la fecha de ingreso y el ajuste manual de un período.
- Habilitar el 1 de octubre de 2027 los días de este período, más lo que sobre de 2025-2026, y de ahí arrastrar lo no gozado.
- Mostrar De este período, Del período anterior, Gozados y Quedan desde un botón dentro de Cargar y, solo para consultar, en el reporte y el PDF. Cargar los días extra solo desde ese botón.
- Rechazar el rango que se pasa.

**Non-Goals:**
- Volver a cargar a mano el saldo que ya arrastra el sistema.
- Editar días extra desde el reporte.
- Contar francos o días efectivamente trabajados fuera del calendario de la relación laboral.

## Decisions

1. **Período identificado por el año Y de su 1 de octubre.** Una fecha pertenece al período Y si cae entre el 1 de octubre de Y y el 30 de septiembre de Y+1. La antigüedad se mide al 31 de diciembre de Y.
2. **Fecha de ingreso en `app_users.hire_date`**, nullable. Quien administra usuarios la edita. Sin valor, la carga de vacaciones de esa persona se rechaza.
3. **El arrastre no se guarda y la antigüedad espera un octubre.** De este período es la escala o la proporcional al 31 de diciembre de Y. Se muestra y entra en Quedan recién el 1 de octubre de Y+1. Hasta el 30 de septiembre de 2027, Quedan es lo que sobra de la antigüedad al 31 de diciembre de 2025, más los días extra de este período, menos las vacaciones cargadas desde el 1 de octubre de 2025. No se suman períodos anteriores a 2025-2026: si en 2023 o 2024 no hubo vacaciones, esos días no aparecen. El 1 de octubre de 2027, Quedan pasa a ser la antigüedad al 31 de diciembre de 2026 más lo no gozado de 2025-2026. Desde ese día, lo no gozado de un saldo habilitado se suma al período siguiente, junto con la antigüedad que se habilita. Un saldo negativo de más días gozados que cupo no reduce el período que sigue.
4. **Ajuste manual en `vacation_adjustments (user_id, period_year, days)`**, entero no negativo. La ausencia de fila vale cero. Solo `vacations:write` lo escribe. Sirve para un saldo anterior a 2025-2026 que la grilla no reconstruye y para un día concedido aparte. Suma a Quedan ya, en el período donde se guarda. No es una copia del sobrante de 2025-2026.
5. **Proporcional sobre días de calendario.** Desde la fecha de ingreso hasta el 31 de diciembre de Y, inclusive, se divide por 20 y se descarta la fracción. Entran domingos y días sin turno. No se cuentan los turnos cargados: esa cuenta daría menos días a quien no trabaja toda la semana.
6. **Días gozados = turnos con local `vacaciones`.** Cada turno cuenta un día. El franco no se cuenta. Los cargados desde el 1 de octubre de 2025 hasta el 30 de septiembre de 2027 descuentan el saldo de 2025-2026. Los anteriores a esa fecha no. Desde el 1 de octubre de 2027, cada período descuenta los turnos cuya fecha cae en él. Quedan es Del período anterior más los días extra menos los gozados. De este período no entra en esa cuenta hasta el octubre en que se habilita.
7. **La carga valida a cada persona del rango antes de guardar.** Si alguna quedaría con saldo negativo, no se persiste ningún día del pedido. Si Del período anterior y Días extra son cero, no se puede cargar, aunque De este período muestre días sin habilitar. El reemplazo de un turno que ya era vacaciones de esa persona en ese día no descuenta dos veces. Un rango que cruza el 1 de octubre de 2027 se parte: los días del pedido que caen antes se tratan como ya gozados al medir el saldo que se habilita ese día.
8. **El menú no gana un ítem.** Cargar sigue eligiendo personas y fechas, y tiene el botón Cupo. Ese botón abre De este período, Del período anterior, Gozados y Quedan, y ahí se escriben los Días extra, también en cero. De este período y Del período anterior no se editan. El reporte y el PDF muestran los mismos números, no tienen el botón y no editan. Si el ajuste es cero, no muestran la línea Días extra. El reporte abre en el período de goce en curso. Un rango libre mantiene los tramos y no arma un saldo único.

## Risks / Trade-offs

- [La proporcional cuenta días sin turno] → Es la cuenta que más días le da a quien no trabaja toda la semana. Quedó elegido así.
- [Usuarios ya cargados sin fecha de ingreso] → La columna nace nula. Hasta completarla, esa persona no admite vacaciones nuevas. Las vacaciones ya cargadas siguen visibles.
- [Un rango que cruza el 1 de octubre] → Parte de los días caen en un período y el resto en el siguiente. La validación reparte el rango por período y exige saldo en cada uno. El 1 de octubre de 2027 además habilita la antigüedad que estaba a la vista.
- [Quedan no incluye De este período] → Quien ya gozó todo 2025-2026 espera al 1 de octubre de 2027, salvo un día extra. Los días de este período se ven para que se sepa qué se habilita.

## Migration Plan

- `AuthCleanup` aplica, de forma idempotente, `app_users.hire_date` y la tabla `vacation_adjustments`.
- El mismo SQL queda en `backend/planificator/sql/`, para poder leerlo. Al arrancar la API se aplica solo; no hace falta ejecutarlo a mano.
- Rollback: quitar la validación del cupo y dejar de leer las columnas. Borrarlas es un paso aparte, como con el cupo anterior.

## Open Questions

Ninguna. El arrastre automático y el ajuste manual quedaron definidos en la spec.

# Tasks

## 1. Datos

- [x] 1.1 Agregar `app_users.hire_date` y la tabla `vacation_adjustments (user_id, period_year, days)` en `AuthCleanup` y en un SQL idempotente. Verificar que arrancar dos veces no falla y que una fila ausente equivale a cero días de ajuste.
- [x] 1.2 Exponer la fecha de ingreso en el alta y la edición de usuarios, y el ajuste manual para quien tiene `vacations:write`. Verificar que un número negativo se rechaza y que quien no administra usuarios no cambia la fecha.

## 2. Cálculo

- [x] 2.1 Calcular el período Y de una fecha, la antigüedad al 31 de diciembre de Y y el cupo: 14, 21, 28 o 35, o la proporcional de días de calendario desde el ingreso hasta el 31 de diciembre, domingos y días sin turno incluidos, dividida por 20. Verificar con pruebas los bordes de cinco, diez y veinte años, seis meses justos, el ingreso del 1 de octubre y que un día sin turno igual suma.
- [x] 2.2 Armar el cupo como antigüedad más saldo no gozado del período anterior más ajuste manual, y el saldo como ese cupo menos turnos del local `vacaciones`. Verificar que quien no toma los 14 los suma al período siguiente, que el primer período posterior al ingreso no tiene arrastre, que un ajuste de 3 no se duplica con ese arrastre, que el franco no descuenta y que un rango que cruza el 1 de octubre se parte en dos períodos.

## 3. Carga

- [x] 3.1 Rechazar la carga si alguna persona no tiene fecha de ingreso, si el rango dejaría su saldo por debajo de cero, o si De este período, Del período anterior y Días extra son cero, sin guardar días parciales. Verificar el ingreso del 20 de diciembre de 2026 sin arrastre ni extra, que un día extra sí habilita ese día, y que un turno de vacaciones que se reemplaza en el mismo día no se descuenta dos veces.
- [x] 3.2 En Cargar, agregar el botón Cupo. Al abrirlo, mostrar De este período, Del período anterior, Gozados y Quedan, y editar solo Días extra. El Reporte no tiene ese botón. Verificar en la pantalla que 14 más 3 arrastrados y 0 gozados muestran Quedan 17, que guardar 1 día extra sube Quedan, que un saldo de 2 rechaza 3 días al guardar fechas y que 5 días sobre un saldo de 5 quedan en cero.
- [x] 3.3 Abrir el reporte en el período de goce en curso y mostrar, por persona y solo para consultar, los mismos números que el botón. Si el ajuste es cero, no mostrar Días extra. En un rango que no es un período de goce, dejar los tramos sin un saldo único. El PDF de un período de goce repite los números y no ofrece edición. Verificar con el caso 14, arrastre 3 y 11 gozados: Quedan 6, y que el PDF de ese período los incluye.

## Workflow follow-up

- Archivar el cambio cuando se revise la implementación.

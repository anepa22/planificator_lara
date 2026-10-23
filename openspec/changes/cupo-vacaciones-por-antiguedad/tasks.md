# Tasks

## 1. Datos

- [x] 1.1 Agregar `app_users.hire_date` y la tabla `vacation_adjustments (user_id, period_year, days)` en `AuthCleanup` y en un SQL idempotente. Verificar que arrancar dos veces no falla y que una fila ausente equivale a cero días de ajuste.
- [x] 1.2 Exponer la fecha de ingreso en el alta y la edición de usuarios, y el ajuste manual para quien tiene `vacations:write`. Verificar que un número negativo se rechaza y que quien no administra usuarios no cambia la fecha.

## 2. Cálculo

- [x] 2.1 Calcular el período Y de una fecha, la antigüedad al 31 de diciembre de Y y el cupo: 14, 21, 28 o 35, o la proporcional de días de calendario desde el ingreso hasta el 31 de diciembre, domingos y días sin turno incluidos, dividida por 20. Verificar con pruebas los bordes de cinco, diez y veinte años, seis meses justos, el ingreso del 1 de octubre y que un día sin turno igual suma.
- [x] 2.2 Armar Quedan, hasta el 30 de septiembre de 2027, como lo que sobra de la antigüedad al 31 de diciembre de 2025 más el ajuste de este período, menos los turnos del local `vacaciones` desde el 1 de octubre de 2025. De este período se muestra y no entra en Quedan. El 1 de octubre de 2027 se habilita la antigüedad al 31 de diciembre de 2026 más ese sobrante, y desde ahí lo no gozado pasa al período siguiente. Verificar 14 al 31 de diciembre de 2025 con 11 ya cargados: Quedan 3 y los 14 de este período fuera; un ingreso del 1 de mayo de 2022 sin vacaciones desde el 1 de octubre de 2025 deja Del período anterior en 14 y no suma años anteriores; si de esos 3 sobran 2, el 1 de octubre de 2027 el saldo es 16; un ajuste de 3 no se duplica; el franco no descuenta; un rango que cruza el 1 de octubre de 2027 se parte y no usa dos veces el sobrante.

## 3. Carga

- [x] 3.1 Rechazar la carga si alguna persona no tiene fecha de ingreso, si el rango dejaría su saldo por debajo de cero, o si Del período anterior y Días extra son cero, aunque De este período muestre días sin habilitar, sin guardar días parciales. Verificar el ingreso del 20 de diciembre de 2026 sin saldo de 2025-2026 ni extra, que 14 de este período todavía no habilitados no alcanzan para cargar, que un día extra sí habilita ese día, y que un turno de vacaciones que se reemplaza en el mismo día no se descuenta dos veces.
- [x] 3.2 En Cargar, el botón Cupo muestra De este período, Del período anterior, Gozados y Quedan, y se edita solo Días extra. El Reporte no tiene ese botón. Verificar en la pantalla que De este período 14, Del período anterior 3 y Gozados 0 muestran Quedan 3, que guardar 1 día extra sube Quedan, que un saldo de 2 rechaza 3 días al guardar fechas y que 5 días sobre un saldo de 5 quedan en cero.
- [x] 3.3 Abrir el reporte en el período de goce en curso y mostrar, por persona y solo para consultar, los mismos números que el botón. Si el ajuste es cero, no mostrar Días extra. En un rango que no es un período de goce, dejar los tramos sin un saldo único. El PDF de un período de goce repite los números y no ofrece edición. Verificar De este período 14, Del período anterior 3 y Gozados 0 con Quedan 3, y que el PDF de ese período los incluye.

## Workflow follow-up

- Archivar el cambio cuando se revise la implementación.

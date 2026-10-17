# Tasks

## 1. Fecha de ingreso

- [x] 1.1 Agregar `hire_date` nula en `app_users` y exponerla en el alta y la edición de usuarios. Verificar que un usuario se guarda y se vuelve a leer con esa fecha.
- [x] 1.2 Mostrar la fecha de ingreso en la administración de usuarios. Verificar en la pantalla que se puede cargar y corregir.

## 2. Días acordados

- [x] 2.1 Guardar una cantidad de días acordados por usuario y período, editable por quien tiene `vacations:write`. Verificar que se puede cargar, cambiar y dejar en cero.
- [x] 2.2 Mostrar la carga de esos días antes de asignar las fechas de vacaciones. Verificar que un acuerdo de 5 días queda asociado al período elegido.

## 3. Cálculo del cupo

- [x] 3.1 Calcular los días de ley al 31 de diciembre del año calendario, según 14, 21, 28 y 35, cuando los días trabajados de ese año llegan a la mitad de sus lunes a sábado. Verificar con pruebas los límites de 5, 6, 10, 11, 20 y 21 años, y que un ingreso el 1 de julio que trabaja hasta el 31 de diciembre mantiene la escala.
- [x] 3.2 Si en el año calendario no llegan a esa mitad, calcular 1 día de ley cada 20 días con turno distinto de vacaciones y franco. Verificar que 19 días dan 0 y 40 dan 2.
- [x] 3.3 Contar como gozados los turnos de vacaciones entre el 1 de octubre y el 30 de septiembre siguiente. Verificar que el 2 de octubre y el 20 de septiembre caen en el período que corresponde.
- [x] 3.4 Sumar ley, días acordados y lo no usado de los períodos anteriores. Verificar que 14 de ley, 5 acordados y 10 gozados dejan 9 para el período siguiente.
- [x] 3.5 Rechazar el alta si falta la fecha de ingreso o si algún período del rango supera el cupo. Verificar que no se crean turnos y que un rango que cruza el 1 de octubre se valida por período.

## 4. Carga en pantalla

- [x] 4.1 Mostrar en el modal ley, acordados, acumulados, gozados y restantes para las personas y las fechas elegidas. Verificar que el saldo cambia al cambiar personas, fechas o días acordados.
- [x] 4.2 Impedir guardar desde el modal cuando el servidor rechaza el cupo y mostrar el motivo. Verificar que una carga dentro del total sigue guardándose.

## Workflow follow-up

- Archivar el cambio cuando la implementación esté revisada.

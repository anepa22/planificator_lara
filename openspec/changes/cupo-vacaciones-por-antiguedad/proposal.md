# Proposal

## Why

Hoy se pueden cargar vacaciones sin saber cuántos días le corresponden a cada persona ni cuántos le quedan. El cupo depende de la antigüedad, de lo acordado por fuera de la ley y de lo que no se usó en el período anterior.

## What Changes

- Guardar la fecha de ingreso de cada persona. Hoy no existe.
- El período de vacaciones va del 1 de octubre al 30 de septiembre siguiente. Se renueva cada 1 de octubre.
- Calcular los días de ley con la escala de la Ley de Contrato de Trabajo: 14 días hasta 5 años, 21 hasta 10, 28 hasta 20 y 35 con más de 20. La antigüedad se toma al 31 de diciembre del año en que empieza el período.
- Si en el año calendario no trabajó al menos la mitad de los días hábiles, los días de ley son 1 cada 20 días trabajados. Diecinueve días dan 0 y cuarenta dan 2. Quien entra el 1 de julio y trabaja hasta el 31 de diciembre llega a esa mitad y mantiene la escala completa.
- Permitir cargar, de antemano y por persona y período, días acordados por fuera de la ley. Se suman a los de la ley.
- Lo no usado se acumula y se suma el 1 de octubre siguiente. No vence.
- El cupo usable es días de ley + días acordados + días acumulados.
- Mostrar, al cargar vacaciones, ese total, los ya gozados y los que quedan.
- Rechazar la carga solo si supera ese total.
- Los días gozados son los ya cargados en la grilla como vacaciones dentro del período. Quitarlos devuelve cupo.

Supuestos:

- Un día trabajado es un día con turno que no sea vacaciones ni franco. Los días hábiles son los lunes a sábado del año calendario al que corresponde el período.
- Sin fecha de ingreso no se puede calcular la parte legal ni cargar vacaciones.
- Si no hay días acordados, el cupo es la ley más lo acumulado.

## Capabilities

### New Capabilities

- `cupo-de-vacaciones`: cupo por período, días acordados, acumulación y rechazo al superar el total.

### Modified Capabilities

- Ninguna. El reporte de vacaciones sigue mostrando tramos y no calcula el cupo.

## Impact

- Alta y edición de usuarios: fecha de ingreso.
- Carga de días acordados por persona y período.
- Modal de carga de vacaciones y alta de turnos de vacaciones.
- No cambia vidrieras, francos, tareas ni el PDF del reporte.

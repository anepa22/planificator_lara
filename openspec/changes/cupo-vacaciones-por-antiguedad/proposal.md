# Proposal

## Why

Hoy se cargan vacaciones sin saber cuántos días le corresponden a cada persona. El cupo depende de la antigüedad y lo que no se goza tiene que pasar al período siguiente.

## What Changes

- El cupo de cada período de goce se calcula con la escala de la Ley de Contrato de Trabajo a partir de la fecha de ingreso.
- El período va del 1 de octubre del año Y al 30 de septiembre de Y+1. La antigüedad se cuenta hasta el 31 de diciembre de Y.
- Lo que no se goza en un período se suma solo al cupo del siguiente. La cadena empieza en el primer período posterior al ingreso.
- Quien carga vacaciones puede sumar, a mano y sin número negativo, días que el arrastre no conoce: el saldo inicial al empezar, o un día concedido aparte de la antigüedad. No se usa para volver a cargar el saldo que ya pasó solo.
- Los días ya cargados en el período descuentan el saldo. Guardar un rango que deje el saldo negativo se rechaza. Si de este período, del período anterior y los días extra son cero, no se puede cargar.
- Sin fecha de ingreso no se calcula el cupo ni se cargan vacaciones para esa persona. El franco no entra en la cuenta.

## Capabilities

### New Capabilities

- `cupo-de-vacaciones`: cálculo del cupo por antigüedad, arrastre del saldo no gozado y ajuste manual desde un botón dentro de Cargar.

### Modified Capabilities

- `reporte-de-vacaciones`: el reporte abre en el período de goce y muestra, solo para consultar, de este período, del período anterior, gozados y quedan. No edita días extra. El PDF repite esos números.

## Impact

- Fecha de ingreso en la ficha de usuario, sembrada al arrancar la API y en un SQL idempotente.
- Ajuste manual por usuario y período de goce. El arrastre se calcula; no se guarda como copia del saldo.
- El menú sigue siendo Cargar y Reporte. Dentro de Cargar, el botón Cupo muestra De este período, Del período anterior, Gozados y Quedan, y ahí se cargan los días extra. El reporte muestra los mismos números y no los edita.
- El reporte abre en el período de goce en curso. El PDF repite esos cuatro números cuando el período de la pantalla es un período de goce.

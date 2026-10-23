# Proposal

## Why

Hoy se cargan vacaciones sin saber cuántos días le corresponden a cada persona. El cupo depende de la antigüedad y lo que no se goza tiene que pasar al período siguiente.

## What Changes

- El cupo de cada período de goce se calcula con la escala de la Ley de Contrato de Trabajo a partir de la fecha de ingreso.
- El período va del 1 de octubre del año Y al 30 de septiembre de Y+1. La antigüedad se cuenta hasta el 31 de diciembre de Y.
- Desde el período que abre el 1 de octubre de 2026 se goza la antigüedad de ese período. Lo anterior a esa fecha no se cuenta. Lo que no se goza pasa al período siguiente.
- Quien carga vacaciones puede sumar, a mano y sin número negativo, días que el arrastre no conoce: un saldo anterior al 1 de octubre de 2026 que la grilla no reconstruye, o un día concedido. No se usa para volver a cargar el sobrante que ya aparece solo.
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

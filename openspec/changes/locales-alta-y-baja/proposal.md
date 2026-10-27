# Proposal

## Why

Los locales de venta están cargados en la base y no se pueden crear ni retirar desde la aplicación. Para abrir o cerrar un local hay que tocar los datos a mano.

## What Changes

- Quien tiene el permiso Gestionar locales puede crear un local de venta, editarlo, darlo de alta y darlo de baja. Administrar usuarios no alcanza. No puede haber más de 8 locales de venta activos.
- El alta lo deja disponible para turnos, tareas y vidriera. La baja lo oculta para cargas nuevas y no borra el local ni lo ya guardado.
- Una semana o un mes que ya tiene turnos de ese local sigue mostrándolos con su nombre.
- Franco y vacaciones no entran en esta pantalla: siguen siendo locales del sistema y no se pueden crear, editar ni dar de baja.
- No se elimina un local. La baja es la forma de retirarlo.

## Capabilities

### New Capabilities

- `catalogo-de-locales`: alta, edición y baja de los locales de venta, y el efecto de un local inactivo sobre las cargas nuevas y el historial.

### Modified Capabilities

## Impact

- API de locales, hoy solo de lectura pública de los activos.
- Pantalla de administración, visible con `locations:manage`, junto a Usuarios.
- Grilla semanal y mensual, selector de turnos, tareas y vidriera, que dejan de ofrecer un local dado de baja.
- Bitácora del alta, la edición y la baja.
- Franco y vacaciones, el cupo y el reporte de vacaciones no cambian.

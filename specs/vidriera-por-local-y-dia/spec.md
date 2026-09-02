# Indicador de vidriera por local y día

## Contexto
Gisela necesita marcar, en un día puntual y en un local de ventas,
si ese día se hace la vidriera. Hoy no hay forma de indicarlo ni de verlo.

## Decisiones
- Permiso marcar/desmarcar: `shifts:write`. Sin ese permiso, solo lectura.
- Vidriera = sí/no por (local, fecha). Sin persona ni horario.
- En un día puede haber vidriera en todos los Lara, en algunos, en uno, o en ninguno.
- Cada local se marca por separado (Lara1 no prende Lara2).
- Locales habilitados: los de ventas Lara (Lara1, Lara2, …), vía flag
  en el local (`supports_vidriera`). No aplica a vacaciones ni franco.
- Alta/baja de locales: fuera de esta entrega. El flag queda listo para
  cuando exista esa pantalla.
- Semana: badge/botón “Vidriera” en el encabezado de la columna del local
  (el día es el de las pestañas). Se marca y desmarca solo acá.
- Mes: solo lectura. En la cabecera del día, el nombre del local en amarillo.
  Si hay varios locales ese día, todos esos nombres en amarillo.
- El amarillo es el color del nombre del local en la vista mensual,
  no un color de producto para toda la app.

## Fuera de alcance
- Login, usuarios, roles nuevos.
- Vacaciones, francos, almuerzo, feriados, personas.
- Convertir vidriera en un turno de una persona.
- Pantalla para agregar/sacar locales.
- Permiso nuevo (se reutiliza `shifts:write`).
- Marcar o desmarcar desde la vista mensual.

## Comportamiento

### Semana
- El badge aparece solo si el local tiene `supports_vidriera`.
- Con `shifts:write`, clic prende/apaga vidriera de ese local en el día
  seleccionado.
- Badge activo e inactivo se distinguen. No hace falta que sea amarillo.
- Sin `shifts:write`, se ve el estado y no es clicable.

### Mes
- Cabecera del día: nombre(s) del/los local(es) con vidriera, en amarillo.
- Sin vidriera ese día: la cabecera se ve como hoy.
- Si el mes está filtrado por local, mostrar solo las vidrieras de esos locales.

### Persistencia
- Recargar la página mantiene lo marcado.

## Criterios de aceptación
- [ ] En un Lara habilitado y un día concreto puedo marcar vidriera.
- [ ] Puedo desmarcar esa misma vidriera.
- [ ] En la semana, el badge de ese local refleja el día seleccionado.
- [ ] En el mes, ese día muestra el nombre del local en amarillo.
- [ ] Mismo día, dos Lara: dos nombres en amarillo.
- [ ] Recargar mantiene el dato.
- [ ] Un local sin flag (p. ej. vacaciones/franco) no muestra el badge.
- [ ] Usuario sin `shifts:write` ve el indicador y no puede cambiarlo.

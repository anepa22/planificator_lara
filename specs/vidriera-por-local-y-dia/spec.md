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
- Carga y modificación: mismo modal **Cargar o modificar vidriera** en Personal
  (junto a Equipo, Vacaciones, Almuerzo). Destildar un local y guardar lo quita.
- Semana: sin check. Si ese local tiene vidriera ese día, se ve la palabra
  “Vidriera” y el encabezado pintado de amarillo. Con permiso, el clic en ese
  encabezado abre el modal de ese día para modificar.
- Mes: en la tarjeta del turno, comentario “Vidriera” en amarillo.
  El nombre del local queda con el color habitual. Con permiso, el clic en
  “Vidriera” abre el modal de ese día (sin abrir el turno).

## Fuera de alcance
- Login, usuarios, roles nuevos.
- Vacaciones, francos, almuerzo, feriados, personas.
- Convertir vidriera en un turno de una persona.
- Pantalla para agregar/sacar locales.
- Permiso nuevo (se reutiliza `shifts:write`).
- Check en la grilla semanal. El indicador amarillo solo abre el modal.

## Comportamiento

### Carga y modificación (menú Personal → Cargar o modificar vidriera)
- Con `shifts:write`, el ítem abre un modal: día + locales habilitados.
- El modal lista las vidrieras ya cargadas del período visible; al tocar una se
  carga ese día para editarlo.
- Guardar sincroniza ese día (alta de los tildados, baja de los destildados).
- Sin `shifts:write` el ítem no aparece.

### Semana
- Si hay vidriera en ese local y día: encabezado amarillo y la palabra “Vidriera”.
- Si no hay: el encabezado se ve como siempre, sin la palabra ni check.
- Con `shifts:write`, clic en el encabezado amarillo abre el modal de ese día.

### Mes
- En la tarjeta del turno aparece “Vidriera” en amarillo si hay vidriera
  en ese local ese día. El nombre del local no cambia de color.
- Si el mes está filtrado por local, aplica a las tarjetas visibles.
- Con `shifts:write`, clic en el comentario “Vidriera” abre el modal de ese día.

### Persistencia
- Recargar la página mantiene lo marcado.

## Criterios de aceptación
- [ ] Desde Personal → Cargar o modificar vidriera puedo marcar un Lara en un día concreto.
- [ ] Puedo desmarcar o cambiar locales de una vidriera ya cargada desde el mismo modal.
- [ ] En la semana, clic en el encabezado amarillo abre el modal de ese día.
- [ ] En el mes, clic en el comentario “Vidriera” abre el modal de ese día.
- [ ] En la semana, sin check: si está cargada, palabra “Vidriera” y encabezado amarillo.
- [ ] En el mes, la tarjeta de quien trabaja en ese local ese día muestra “Vidriera” en amarillo.
- [ ] Recargar mantiene el dato.
- [ ] Usuario sin `shifts:write` ve el indicador y no tiene el ítem de menú.

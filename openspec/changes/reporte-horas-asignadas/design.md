# Design

## Context

La grilla muestra las horas de la semana en curso y les resta el almuerzo guardado en el navegador. `GET /api/shifts/summary` es público y solo cubre una semana. Franco y vacaciones están fijos. Ver proposal.md para el motivo.

## Goals / Non-Goals

**Goals:**

- Una consulta de un rango arbitrario, con permiso propio, que no reutilice el resumen semanal público.
- Las horas salen de la duración del turno, menos el almuerzo de la grilla una vez por día trabajado, redondeadas a dos decimales.
- Un PDF de la misma búsqueda que está en pantalla.

**Non-Goals:**

- Cambiar el descuento de almuerzo de la grilla.
- Contar franco, vacaciones o el cupo.

## Decisions

1. El permiso es `hours:report`, con el nombre Ver reporte de horas. No alcanza `shifts:write`. Al crearlo, se lo damos una sola vez a Administrador y a Supervisor. Un arranque posterior no vuelve a otorgarlo. La alternativa de reusar `vacations:report` mezcla dos consultas que no se reemplazan.

2. La consulta es un endpoint nuevo del rango, con `hours:report`. No se extiende `GET /api/shifts/summary`: ese sigue público y semanal. Desde, Hasta, persona y local son parámetros. Sin persona y sin local, entran todos los turnos de venta del rango.

3. La hora de un turno es el fin menos el inicio. Si el fin no es posterior al inicio, suma 0. Se suman los turnos de venta del rango y se resta el almuerzo de la grilla una vez por cada día distinto con turno de venta. El almuerzo es el valor que la grilla ya guarda en el navegador: si no hay valor, 1 hora; nunca menos de 0 ni más de 4. El resultado de la persona no baja de 0 y se redondea a dos decimales. La pantalla manda ese mismo valor en la consulta. Franco y vacaciones no suman ni cuentan como día.

4. Sin persona elegida, no se lista quien quedó en 0. Con una persona elegida, esa fila se muestra aunque sea 0. El total es la suma de las filas visibles.

5. El filtro de local ofrece los locales de venta activos y cualquier local de venta que tenga turnos en el rango, aunque esté de baja. Franco y vacaciones no están en la lista. Pedirlos como local se rechaza.

6. El rango se valida en el servidor: Hasta no anterior a Desde, y no más de 800 días, la misma cota del reporte de vacaciones. Al abrir, Desde y Hasta son el primer y el último día del mes calendario en curso, en America/Argentina/Buenos_Aires.

7. El ítem Horas cuelga de Asistentes, al mismo nivel que Vacaciones y Vidrieras. Sin el permiso no se renderiza. La pantalla es una ventana con el rango, los dos filtros, las filas, el total y el PDF.

8. El PDF usa el mismo resultado que la consulta, armado con OpenPDF como el de vacaciones. Lo autoriza `hours:report`: no se crea un permiso de impresión aparte. El botón queda deshabilitado si el rango, un filtro o el almuerzo no coinciden con la última búsqueda. El archivo repite el rango, los filtros, el almuerzo, cada persona y el total.

## Risks / Trade-offs

- [El almuerzo vive en cada navegador, como en la grilla] → El reporte manda el mismo valor que la grilla de esa pantalla, así los dos números coinciden. Otro navegador con otro almuerzo ve otro total, igual que hoy en la grilla.
- [Un rango largo lee muchos turnos] → El tope de 800 días es el mismo que ya usa el reporte de vacaciones.

## Migration Plan

No hay tabla nueva. El permiso se crea al arrancar la API, una sola vez, y se otorga a Administrador y Supervisor. La pantalla llega con la web. Si se vuelve a la versión anterior, el ítem desaparece y el permiso queda sin uso.

# Tasks

## 1. Permiso

- [x] 1.1 Crear `hours:report` (Ver reporte de horas) al arrancar, una sola vez, y dárselo a Administrador y a Supervisor. Verificar que un segundo arranque no vuelve a otorgarlo si se lo sacaron.

## 2. Consulta

- [x] 2.1 Calcular las horas del rango: fin menos inicio, restar el almuerzo una vez por día trabajado, piso 0, dos decimales, sin franco y sin vacaciones. El almuerzo va de 0 a 4 y, si no viene, es 1. Sin persona, omitir el cero; con persona elegida, mostrar 0. Rechazar Hasta anterior a Desde, más de 800 días, y un local franco o vacaciones. Verificar con tests de 09:30–20:00 y almuerzo 1 = 9,5, de dos turnos el mismo día que restan una sola vez, de un franco en 0, y del rango inválido.
- [x] 2.2 Exponer la consulta solo con `hours:report` y verificar que sin ese permiso responde 403.
- [x] 2.3 Generar el PDF de esa misma consulta, con rango, filtros, almuerzo, filas y total. Verificar que el texto contiene 9,5 y el total, y que sin `hours:report` responde 403.

## 3. Pantalla

- [x] 3.1 Agregar Horas al menú Asistentes, visible solo con `hours:report`. Verificar que sin el permiso el ítem no está.
- [x] 3.2 Abrir el reporte en el mes en curso, con Desde, Hasta, persona y local, las filas y el total, descontando el mismo almuerzo que la grilla. Verificar que un rango inválido se avisa en esa ventana, que franco y vacaciones no están en el filtro de local, y que cambiar el almuerzo de la grilla cambia el total del reporte.
- [x] 3.3 Agregar el botón de PDF, deshabilitado si el rango, un filtro o el almuerzo no coinciden con la última búsqueda. Verificar que al buscar se habilita y que el archivo corresponde a esa búsqueda.

## Workflow follow-up

- Archivar el cambio cuando el comportamiento esté revisado.

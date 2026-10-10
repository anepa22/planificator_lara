# Reporte de vacaciones

## Objetivo
Consultar, en un período, los días de vacaciones ya cargados en la grilla.
Los días seguidos de una misma persona se muestran como un tramo.

## Permisos
- Ver el reporte: `vacations:report` («Ver reporte de vacaciones»).
- Imprimir el PDF: `vacations:print` («Imprimir reporte de vacaciones»).
- Cargar o quitar vacaciones sigue siendo `vacations:write`.
- Los tres son independientes. Ver el reporte no alcanza para imprimirlo.
- `MIGRA_REPORTE_VACACIONES.sql` copia la consulta a Supervisor,
  Administrador y a todo rol que ya tenga `vacations:write`.
- `MIGRA_REPORTE_VACACIONES_PDF.sql` deja la impresión solo en Supervisor
  y Administrador.
- En producción no hace falta correrlos. Al arrancar, el API crea los dos
  permisos y se los asigna a Supervisor y Administrador.
- Ese arranque no vuelve a dar la consulta a un rol al que se la hayan
  quitado, ni la impresión a quien solo carga vacaciones.
- También se puede dar o quitar cada permiso por usuario, aparte del rol.

## Menú
Dentro de Asistentes:

1. Vacaciones
   - Cargar, si tiene `vacations:write`
   - Reporte, si tiene `vacations:report`
2. Vidrieras

El grupo Vacaciones aparece si el usuario puede cargar o ver el reporte.

## Reporte
- Abre el año en curso.
- Se puede elegir el año anterior, el actual o el siguiente, o un rango
  Desde / Hasta.
- Cada persona muestra sus tramos y la cantidad de días.
- El encabezado suma los días y las personas del período.
- Si no hay vacaciones, el período se ve vacío.
- Quien tiene `vacations:print` ve el botón Imprimir PDF.
- El PDF es el período que está en pantalla. Si se cambian Desde o Hasta
  y todavía no se buscó, el botón queda deshabilitado.
- El archivo lista cada tramo y, debajo de la persona, el total de sus días.

## Reglas
- Un tramo son días calendario consecutivos de la misma persona.
- Un hueco de un día abre otro tramo.
- La cantidad de días incluye el primer y el último día.
- El período no puede superar 800 días.
- Hasta no puede ser anterior a Desde.
- Dos personas con el mismo nombre no se mezclan.

## Fuera de alcance
- Francos, turnos de trabajo y vidrieras.
- Exportar a Excel u otro formato distinto del PDF.
- Editar vacaciones desde el reporte.
- Cupo anual de días de vacaciones.

## Criterios de aceptación
- [x] Quien tiene `vacations:report` ve Asistentes → Vacaciones → Reporte.
- [x] Quien no lo tiene no ve el ítem y la API responde sin permiso.
- [x] Cargar vacaciones sigue exigiendo `vacations:write`.
- [x] Imprimir PDF exige `vacations:print` y no alcanza con ver el reporte.
- [x] El PDF es del período en pantalla y totaliza los días de cada persona.
- [x] El año en curso aparece al abrir.
- [x] Los días seguidos forman un tramo y un hueco los separa.
- [x] El total de días coincide con la suma de los tramos.

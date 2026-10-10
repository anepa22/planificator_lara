# Reporte de vacaciones

## Objetivo
Consultar, en un período, los días de vacaciones ya cargados en la grilla.
Los días seguidos de una misma persona se muestran como un tramo.

## Permiso
- Ver el reporte: `vacations:report` («Ver reporte de vacaciones»).
- Imprimir el PDF: `vacations:print` («Imprimir reporte de vacaciones»).
- Cargar o quitar vacaciones sigue siendo `vacations:write`.
- Un usuario puede tener solo uno de los dos.
- El script inicial se lo asigna a Supervisor, Administrador y a todo rol
  que ya tenga `vacations:write`.
- El arranque del API mantiene el permiso de ver el reporte en Supervisor
  y Administrador. No se lo vuelve a poner a un rol al que se lo hayan quitado.
- El permiso de imprimir queda solo en Supervisor y Administrador. No se
  copia a quien únicamente carga vacaciones.
- También se puede dar o quitar por usuario, aparte del rol.

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
- Quien tiene `vacations:print` ve el botón Imprimir PDF y baja el archivo
  del período consultado. El PDF cierra cada persona con el total de sus días.

## Reglas
- Un tramo son días calendario consecutivos de la misma persona.
- Un hueco de un día abre otro tramo.
- La cantidad de días incluye el primer y el último día.
- El período no puede superar 800 días.
- Hasta no puede ser anterior a Desde.
- Dos personas con el mismo nombre no se mezclan.

## Fuera de alcance
- Francos, turnos de trabajo y vidrieras.
- Exportar a archivo.
- Editar vacaciones desde el reporte.
- Cupo anual de días de vacaciones.

## Criterios de aceptación
- [x] Quien tiene `vacations:report` ve Asistentes → Vacaciones → Reporte.
- [x] Quien no lo tiene no ve el ítem y la API responde sin permiso.
- [x] Cargar vacaciones sigue exigiendo `vacations:write`.
- [x] Imprimir PDF exige `vacations:print` y no alcanza con ver el reporte.
- [x] El año en curso aparece al abrir.
- [x] Los días seguidos forman un tramo y un hueco los separa.
- [x] El total de días coincide con la suma de los tramos.

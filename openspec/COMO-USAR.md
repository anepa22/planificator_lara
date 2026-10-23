# Cómo usar el modelo

Cada cambio de comportamiento pasa por cuatro pasos. El código entra recién en el tercero. Los textos del cambio se escriben en español.

Hay dos lugares:

- `openspec/specs/` es cómo funciona el producto hoy. Es la referencia del próximo cambio.
- `openspec/changes/<nombre>/` es un cambio en curso. No reemplaza la spec vigente hasta que se archiva.

## El ciclo

1. **Proponer.** Decí `/opsx-propose` y qué querés. Se arman cuatro archivos y no se toca el código:
   - `proposal.md`: para qué es el cambio.
   - `specs/.../spec.md`: qué tiene que cumplir el sistema. Es un delta, no la spec vigente.
   - `design.md`: cómo se hace.
   - `tasks.md`: la lista de tareas.
2. **Aplicar.** Cuando el plan cierra, `/opsx-apply` o un "dale". Recién ahí se escribe el código, tarea por tarea.
3. **Revisar.** Probalo. Si al usarlo cambia una regla, volvé al plan con `/opsx-update` y después aplicalo de nuevo. `/opsx-update` ajusta los archivos del cambio y no toca el código.
4. **Archivar.** `/opsx-archive` copia lo nuevo a `openspec/specs/` y guarda el cambio en `openspec/changes/archive/`. A partir de ahí, esa spec es la referencia.

Si hace falta pensarlo antes de proponer, `/opsx-explore` sirve para conversar la idea. Ahí no se implementa.

## Qué pedirle al chat

| Querés | Decí |
| --- | --- |
| Pensar una idea, sin código | `/opsx-explore` |
| Abrir un cambio y ver el plan | `/opsx-propose` y describí el cambio |
| Corregir el plan | `/opsx-update` |
| Programar las tareas | `/opsx-apply` |
| Cerrar el cambio y actualizar la spec vigente | `/opsx-archive` |

Un "dale" después de ver el plan significa aplicar. Un "dale" cuando el cambio ya está programado y revisado significa archivar.

## Cuándo no hace falta

Un retoque que no cambia lo que el sistema hace no necesita este ciclo: un texto, un color, un refactor.

Si cambia lo que alguien puede hacer, lo que ve, o lo que el sistema acepta o rechaza, sí hace falta. Aunque el cambio sea chico.

## Si el código se escribió antes

`/opsx-propose` va solo al principio, antes de programar. No se abre un cambio para algo que ya está en el código.

Si eso pasó y la spec vigente no lo cuenta, el cierre es escribir ese comportamiento directo en `openspec/specs/`. No hay carpeta en `openspec/changes/` para archivar, porque el cambio nunca se abrió.

El próximo cambio arranca de nuevo con `/opsx-propose`.

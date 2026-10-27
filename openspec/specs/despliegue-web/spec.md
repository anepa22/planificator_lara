# Despliegue web

## Purpose
Que una página abierta o un F5 normal muestre la versión recién desplegada, sin Ctrl+F5.

## Requirements

### Requirement: Página sin caché
La página de la aplicación SHALL responder con `Cache-Control: no-cache`. El navegador SHALL consultar esa página en cada carga. Una recarga normal SHALL alcanzar para ver la versión del último deploy.

#### Scenario: Recarga después de un deploy
- **WHEN** la web se desplegó y el usuario abre la página o presiona F5
- **THEN** ve la versión nueva sin Ctrl+F5

### Requirement: Archivos con hash
Los JavaScript y CSS cuyo nombre cambia en cada build SHALL poder cachearse por mucho tiempo. Un deploy SHALL NOT exigir volver a bajar un archivo cuyo nombre no cambió.

#### Scenario: Archivo ya descargado
- **WHEN** el navegador ya tiene un archivo de `/assets/` y el deploy no cambió su nombre
- **THEN** puede reutilizarlo sin descargarlo de nuevo

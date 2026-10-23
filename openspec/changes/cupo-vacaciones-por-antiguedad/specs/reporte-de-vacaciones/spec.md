# Spec Delta

## MODIFIED Requirements

### Requirement: Período
Al abrir, el reporte SHALL mostrar el período de goce en curso, del 1 de octubre al 30 de septiembre. El usuario SHALL poder elegir el período de goce anterior, el actual, el siguiente o un rango Desde / Hasta. Hasta SHALL NOT ser anterior a Desde y el período SHALL NOT superar 800 días.

#### Scenario: Apertura
- **WHEN** se abre el reporte el 23 de octubre de 2026
- **THEN** el período mostrado es del 1 de octubre de 2026 al 30 de septiembre de 2027

#### Scenario: Rango inválido
- **WHEN** Hasta es anterior a Desde o el período supera 800 días
- **THEN** el sistema rechaza la consulta

### Requirement: PDF
Quien tenga `vacations:print` SHALL poder descargar un PDF del período que está en pantalla. Si Desde o Hasta cambiaron y todavía no se buscó, el botón SHALL permanecer deshabilitado. El PDF SHALL listar cada tramo y, debajo de cada persona, el total de sus días. Cuando el período es un período de goce, el PDF SHALL repetir De este período, Del período anterior, Gozados y Quedan.

#### Scenario: Período en pantalla
- **WHEN** el usuario imprime después de buscar un rango
- **THEN** el PDF corresponde a ese rango y no a las fechas modificadas sin buscar

#### Scenario: PDF de un período de goce
- **WHEN** se imprime el período del 1 de octubre de 2026 al 30 de septiembre de 2027
- **THEN** cada persona incluye los cuatro números del cupo, además de sus tramos

## ADDED Requirements

### Requirement: Resumen del cupo
En un período de goce, cada persona del reporte SHALL mostrar, solo para consultar, De este período, Del período anterior, Gozados y Quedan. El reporte SHALL NOT editar días extra ni el cupo. Si hay días extra, SHALL mostrarlos como número. Si el ajuste es cero, SHALL NOT mostrar esa línea. En un rango que no es un período de goce, el reporte SHALL mostrar los tramos y SHALL NOT ofrecer un saldo único.

#### Scenario: Lectura de una persona
- **WHEN** en el período de goce una persona tiene 14 de este período, 3 arrastrados y 11 gozados
- **THEN** el reporte muestra De este período 14, Del período anterior 3, Gozados 11 y Quedan 6

#### Scenario: Ajuste en cero
- **WHEN** el ajuste de la persona es cero
- **THEN** el reporte no muestra la línea Días extra

#### Scenario: Rango libre
- **WHEN** el rango no coincide con un período de goce
- **THEN** se ven los tramos de vacaciones y no un saldo único

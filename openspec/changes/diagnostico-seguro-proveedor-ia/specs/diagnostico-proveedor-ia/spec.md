# Spec Delta

## Purpose

Permite diagnosticar fallos del proveedor de IA sin convertirlos en resultados de reportes engañosos ni exponer información interna o sensible a quienes consumen la API.

## ADDED Requirements

### Requirement: Los fallos del proveedor de IA deben detener la ejecución del reporte
El backend SHALL tratar como fallida la solicitud de reporte cuando el modelo no esté disponible o el proveedor no genere una respuesta utilizable; no SHALL ejecutar `QUERY_BASE` como sustituto silencioso de la respuesta solicitada.

#### Scenario: El proveedor rechaza o no completa una solicitud
- **WHEN** la invocación al proveedor de IA falla o no produce una consulta utilizable
- **THEN** el backend devuelve una respuesta de error apropiada y no ejecuta SQL contra Oracle para esa solicitud

#### Scenario: El modelo de IA no está disponible
- **WHEN** el backend no tiene un modelo de IA configurado para generar la consulta
- **THEN** el backend informa un error de disponibilidad y no ejecuta la consulta base como si la generación hubiera sido exitosa

### Requirement: Los fallos de IA deben poder correlacionarse con diagnósticos seguros
El backend SHALL registrar la excepción y su causa técnica en los logs del servidor, asociándolas a un identificador de correlación y al proveedor/modelo cuando estén disponibles. La respuesta HTTP SHALL incluir un código de error estable, un mensaje apto para el consumidor, el estado HTTP y el identificador de correlación.

#### Scenario: Diagnóstico del fallo disponible para soporte
- **WHEN** ocurre un fallo del proveedor durante la generación
- **THEN** el log del servidor contiene la traza completa de la excepción y su causa, vinculada al mismo identificador de correlación devuelto por la API

#### Scenario: Información sensible protegida
- **WHEN** se registra o devuelve información asociada a un fallo del proveedor
- **THEN** el backend omite o sanea credenciales, prompts completos y cuerpos crudos del proveedor, y nunca incluye stack traces en la respuesta HTTP

#### Scenario: Fallos diferenciados por clase
- **WHEN** el proveedor no responde a tiempo o devuelve otro tipo de fallo
- **THEN** el backend expone un código estable y un estado HTTP coherentes con la categoría del fallo, sin revelar detalles internos
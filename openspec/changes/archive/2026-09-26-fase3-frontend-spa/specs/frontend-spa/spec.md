# Spec Delta: frontend-spa

## Purpose

Brinda la experiencia de usuario necesaria para explorar el catálogo de reportes, formular prompts en lenguaje natural y visualizar resultados de ejecución con trazabilidad, seguridad y exportación sin requerir conocimientos de SQL.

## ADDED Requirements

### Requirement: El usuario puede consultar el catálogo de reportes desde la SPA
La SPA SHALL mostrar el catálogo de reportes activos recuperado desde el backend, ordenado por categoría y con información suficiente para identificar el propósito de cada reporte antes de ejecutarlo.

#### Scenario: Visualización del catálogo
- **WHEN** un usuario abre la aplicación en la pantalla principal
- **THEN** la interfaz carga y muestra los reportes activos con nombre, categoría, descripción y estado de disponibilidad

### Requirement: El usuario puede ejecutar un reporte desde la interfaz
La SPA SHALL permitir seleccionar un reporte y enviar un prompt en lenguaje natural para generar y ejecutar la consulta correspondiente, devolviendo el resultado con mensajes claros de éxito o error.

#### Scenario: Ejecución exitosa desde la interfaz
- **WHEN** un usuario selecciona un reporte y envía un prompt válido
- **THEN** la aplicación solicita la ejecución al backend y presenta un estado de carga mientras procesa la consulta

#### Scenario: Error de ejecución con respuesta del backend
- **WHEN** el backend rechaza la ejecución por validación, seguridad o ausencia del reporte
- **THEN** la SPA muestra un mensaje explicativo y no deja el usuario en un estado ambiguo

### Requirement: La SPA presenta resultados y SQL ejecutado de forma legible
La interfaz SHALL renderizar dinámicamente la estructura de columnas y filas del resultado, mostrar el SQL ejecutado aprobado por seguridad y mantener una vista clara del contexto de la consulta.

#### Scenario: Resultado tabular con metadatos
- **WHEN** la ejecución retorna filas y columnas
- **THEN** la grilla presenta cada columna con su nombre y valor, junto con la consulta ejecutada en un panel de auditoría

### Requirement: La SPA permite exportar y diagnosticar la ejecución
La aplicación SHALL ofrecer una acción para exportar el resultado actual en CSV o Excel y un mecanismo para revisar el SQL aprobado, errores y estado de la ejecución para análisis posterior.

#### Scenario: Exportación del resultado actual
- **WHEN** el usuario solicita exportar la grilla actual
- **THEN** la aplicación genera un archivo descargable compatible con CSV o Excel y conserva el conjunto de resultados visibles

#### Scenario: Diagnóstico de la ejecución
- **WHEN** la consulta falló o fue rechazada por validación
- **THEN** la aplicación expone el motivo del error y el SQL asociado cuando esté disponible para facilitar la resolución

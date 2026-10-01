# Spec Delta

## MODIFIED Requirements

### Requirement: La SPA permite exportar y diagnosticar la ejecución
La aplicación SHALL ofrecer una acción para exportar el resultado actual en CSV o Excel y un mecanismo para revisar el SQL aprobado, errores y estado de la ejecución para análisis posterior. Cuando una ejecución falla, SHALL mostrar el mensaje seguro y el código de error del backend y, si está disponible, su identificador de correlación. La interfaz SHALL interpretar el contrato de error publicado por la API y no SHALL presentar stack traces ni respuestas crudas del proveedor.

#### Scenario: Exportación del resultado actual
- **WHEN** el usuario solicita exportar la grilla actual
- **THEN** la aplicación genera un archivo descargable compatible con CSV o Excel y conserva el conjunto de resultados visibles

#### Scenario: Diagnóstico de la ejecución
- **WHEN** la consulta falló o fue rechazada por validación
- **THEN** la aplicación expone el motivo del error y el SQL asociado cuando esté disponible para facilitar la resolución

#### Scenario: Fallo del proveedor de IA
- **WHEN** la API responde con un error de generación que contiene código, mensaje seguro e identificador de correlación
- **THEN** la SPA muestra el mensaje y el identificador para soporte, sin mostrar stack traces ni contenido crudo del proveedor
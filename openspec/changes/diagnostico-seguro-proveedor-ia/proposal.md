# Proposal

## Why

Cuando falla el proveedor de IA, el backend actualmente devuelve y ejecuta la consulta base como si la solicitud se hubiera atendido; además, registra solo el mensaje corto de la excepción. Esto produce resultados engañosos y no permite relacionar el error visible en la SPA con el detalle técnico necesario para diagnosticarlo.

## What Changes

- El backend interrumpirá la ejecución si el modelo no está disponible o la llamada al proveedor falla; no usará `QUERY_BASE` como resultado exitoso de una petición que dependía de IA.
- El backend registrará la excepción completa y datos de diagnóstico correlacionables en logs de servidor, evitando guardar prompts completos, credenciales o cuerpos sensibles sin saneamiento.
- La respuesta de error de la API incluirá un código estable, un mensaje seguro y un identificador de correlación, además del estado HTTP. No expondrá stack traces ni respuestas crudas del proveedor.
- La SPA interpretará el contrato de error real del backend, mostrará el mensaje y el identificador de correlación, y ofrecerá detalles técnicos limitados a datos saneados.
- Las consultas generadas correctamente seguirán pasando por los guardrails SQL existentes antes de ejecutarse en Oracle.

## Capabilities

### New Capabilities
- `diagnostico-proveedor-ia`: Define el manejo seguro y observable de fallos del proveedor de IA, la correlación entre respuesta HTTP y logs del backend, y la prohibición de ejecutar la consulta base como fallback silencioso.

### Modified Capabilities
- `frontend-spa`: Actualiza la experiencia de error para mostrar el mensaje del backend y el identificador de correlación, sin exponer stack traces o contenido crudo del proveedor.

## Impact

- Backend Java/Spring AI: generación SQL, excepciones tipadas, logs del proveedor y `@RestControllerAdvice`.
- Contrato REST de errores y DTO de respuesta, con campos aditivos para el identificador de correlación y metadatos técnicos permitidos.
- Frontend React/Vite: lectura de `mensaje`, código de error y correlación; presentación de estado de fallo sin confundirlo con resultados exitosos.
- Pruebas de servicio y controller para fallos, sanitización, estados HTTP y ausencia de ejecución JDBC cuando falle la IA; pruebas de interfaz para representar el error.
- El `oracle-mcp-server` permanece como herramienta de desarrollo para inspección y validación del esquema y SQL; no participa en el manejo de fallos en runtime.
- No se modifican el esquema Oracle ni los guardrails que restringen la ejecución a consultas `SELECT`/`WITH`.

## No Goals

- Mostrar stack traces, prompts completos, credenciales o respuestas crudas del proveedor en la interfaz de usuario.
- Permitir que la IA modifique datos o ejecute DDL/DML.
- Cambiar el proveedor/modelo de IA, rediseñar la integración con Oracle o introducir una plataforma externa de observabilidad.
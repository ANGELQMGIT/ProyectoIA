# Proposal

## Why

El backend declara Spring AI `1.0.0-M2` con el starter de Vertex AI, mientras la configuración también contiene propiedades de Google GenAI API key que ese starter no consume. La migración a una línea estable compatible permitirá usar Gemini Developer API de forma coherente, sin cambiar el contrato de reportes ni la seguridad SQL.

## What Changes

- Alinear las versiones estables de la plataforma en Spring Boot `3.5.15` y Spring AI `1.1.8`, cuyo baseline fue verificado en el parent oficial de Spring AI.
- Reemplazar `spring-ai-vertex-ai-gemini-spring-boot-starter` por `spring-ai-starter-model-google-genai` y conservar la integración Java basada en `ChatModel`.
- Configurar Google GenAI bajo `spring.ai.google.genai`, con modelo bajo `spring.ai.google.genai.chat.options.model` y API key obtenida obligatoriamente desde `GEMINI_API_KEY`, sin valor secreto por defecto.
- Retirar las propiedades de conexión Vertex (`project-id`, `location` y credenciales) para evitar ambigüedad entre mecanismos de autenticación.
- Documentar la configuración segura de entorno y verificar generación, errores correlacionados y ausencia de ejecución JDBC ante fallos de IA.
- Mantener sin cambios los endpoints REST, el formato de respuestas, los guardrails `SELECT`/`WITH` y el límite `FETCH FIRST 500 ROWS ONLY`.

## Capabilities

### New Capabilities

None. This is an infrastructure-only provider and dependency migration.

### Modified Capabilities

None. Existing API, SQL and user-visible error behavior remain unchanged; `.openspec.yaml` declares `skip_specs: true`.

## Impact

- Backend Maven: versión de `spring-boot-starter-parent`, propiedad `${spring-ai.version}` y starter de modelo Google GenAI.
- Configuración de runtime: `application.yml`, variables de entorno `GEMINI_API_KEY` y `GEMINI_MODEL`, y guía local/despliegue.
- Pruebas backend: auto-configuración de `ChatModel`, generación con el proveedor configurado, fallos de autenticación/upstream y preservación del contrato de error.
- Arquitectura: SPA -> backend Spring AI -> Oracle JDBC; el servicio de generación conserva la interfaz `ChatModel`.
- Seguridad SQL: JSqlParser continúa permitiendo exclusivamente `SELECT`/`WITH`, bloqueando DDL/DML y aplicando el límite de 500 filas.
- MCP: `oracle-mcp-server` sigue siendo una herramienta de desarrollo para inspección del esquema y validación de consultas; no participa en runtime.

## No Goals

- Cambiar rutas REST, DTOs, reglas de prompts, el catálogo Oracle o el esquema de base de datos.
- Modificar el contrato visual de errores, exponer stack traces o respuestas crudas del proveedor.
- Permitir DDL/DML, ampliar privilegios JDBC o retirar los guardrails SQL.
- Mantener simultáneamente auto-configuraciones Vertex y Google GenAI para el mismo `ChatModel`.
- Registrar secretos reales en YAML, código, pruebas o documentación.
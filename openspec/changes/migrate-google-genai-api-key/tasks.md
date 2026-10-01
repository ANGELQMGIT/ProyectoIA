# Tasks

## 1. Versiones y proveedor Spring AI

- [x] 1.1 Actualizar el parent de Spring Boot a `3.5.15`, el BOM Spring AI a `1.1.8` y reemplazar el starter Vertex por `spring-ai-starter-model-google-genai`; verificar con `mvnw.cmd dependency:tree` que resuelve Google GenAI y no conserva el starter Vertex.
- [x] 1.2 Compilar el backend con Java 21 tras el cambio de versiones; verificar con `mvnw.cmd -DskipTests compile` y resolver incompatibilidades de APIs antes de continuar.

## 2. Configuración segura y documentación

- [x] 2.1 Reconfigurar `application.yml` a `spring.ai.google.genai.api-key` y `spring.ai.google.genai.chat.options.model`, eliminar la configuración Vertex y no incluir defaults secretos; verificar que la aplicación detecta la ausencia de `GEMINI_API_KEY` sin ejecutar una consulta base.
- [x] 2.2 Documentar cómo definir `GEMINI_API_KEY` y `GEMINI_MODEL` en entorno local/despliegue y cómo rotar credenciales; verificar que ejemplos y configuración versionada no contienen claves ni contraseñas.

## 3. Integración del modelo y manejo de errores

- [x] 3.1 Verificar con pruebas de contexto Spring que el starter Google GenAI configura un único `ChatModel` con las propiedades esperadas sin realizar llamadas externas ni requerir secretos reales.
- [x] 3.2 Cubrir generación, respuesta vacía, error upstream y timeout con pruebas unitarias, conservando el contrato de errores existente; verificar que cualquier fallo no alcanza `JdbcTemplate` y que los logs no incluyen mensajes crudos del proveedor.

## 4. Verificación integrada

- [x] 4.1 Ejecutar `mvnw.cmd test` y verificar guardrails SQL y contrato REST; realizar como verificación manual optativa una llamada real a Google GenAI con `GEMINI_API_KEY` y `GEMINI_MODEL` suministradas por entorno, sin imprimirlas.
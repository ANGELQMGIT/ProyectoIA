# Design

## Context

Véase `proposal.md` para la motivación y el alcance. El backend usa Spring Boot `3.3.4`, Spring AI `1.0.0-M2` y el starter Vertex Gemini; `AiSqlGeneratorService` consume la abstracción `ChatModel`. El YAML contiene simultáneamente propiedades Vertex y Google GenAI, además de credenciales con valores por defecto.

La versión estable Spring AI `1.1.8` declara Spring Boot `3.5.15` en su parent oficial y publica el starter `spring-ai-starter-model-google-genai`. Google GenAI Developer API usa API key; no requiere project ID ni región Vertex. El flujo posterior al `ChatModel` permanece: guardrail JSqlParser y JDBC Oracle solo tras una respuesta generada.

## Goals / Non-Goals

**Goals:**
- Alinear Boot y Spring AI con versiones compatibles, dejando una sola auto-configuración de `ChatModel`.
- Usar la API key únicamente desde `GEMINI_API_KEY` y configurar el modelo con la propiedad Google GenAI documentada.
- Preservar interfaz `ChatModel`, manejo fail-fast, trazabilidad, contrato REST, JSqlParser y ejecución JDBC existente.

**Non-Goals:**
- Migrar el proyecto a Spring AI 2.x / Spring Boot 4.x.
- Cambiar prompts, consultas base, modelo de respuesta HTTP, esquema Oracle, frontend o el uso de MCP.
- Guardar secretos de prueba, imprimirlos en logs o configurar la integración mediante valores por defecto inseguros.

## Decisions

### 1. Versiones estables alineadas

Usar Spring Boot `3.5.15` y Spring AI `1.1.8`; la versión del parent de Spring AI define el baseline de Boot. El BOM `spring-ai-bom` y los módulos Spring AI quedan ambos en `1.1.8`. No mezclar el nuevo starter con Spring AI `1.0.0-M2`.

**Alternativa considerada:** usar Spring AI `1.1.0-M1` con Boot `3.5.0`, el primer release que ofrecía Google GenAI. Se prefiere `1.1.8` por ser el patch estable posterior de la misma línea compatible.

### 2. Un único starter Google GenAI

Reemplazar `spring-ai-vertex-ai-gemini-spring-boot-starter` por `spring-ai-starter-model-google-genai`. No mantener ambas dependencias: el servicio inyecta `ObjectProvider<ChatModel>` y dos proveedores en classpath pueden producir ambigüedad o activar una configuración no deseada.

**Alternativa considerada:** conservar Vertex junto con Google GenAI y elegir por configuración. Se descarta para esta migración porque el objetivo es exclusivamente Gemini Developer API con API key y ambas auto-configuraciones exponen el mismo contrato `ChatModel`.

### 3. Configuración sin secretos ni claves Vertex

Configurar `spring.ai.google.genai.api-key` desde `${GEMINI_API_KEY}` sin fallback literal y `spring.ai.google.genai.chat.options.model` desde `${GEMINI_MODEL}`. En Spring AI 1.1.8 las opciones mantienen el segmento `.options`; retirar las propiedades `spring.ai.vertex.ai.gemini.*`. La documentación local/despliegue explica cómo suministrar variables y que el modelo debe estar soportado por la API.

Las credenciales actualmente escritas como defaults se consideran expuestas y deben rotarse fuera de este cambio de código; no se registran en archivos, ejemplos ni logs. La ausencia de `GEMINI_API_KEY` debe fallar claramente en configuración o reportar la disponibilidad del modelo, nunca disparar un fallback SQL.

**Alternativa considerada:** conservar la API key como valor por defecto o incluirla en pruebas. Se descarta por riesgo de filtración y rotación difícil.

### 4. Mantener `ChatModel` y aislar diferencias del proveedor

`AiSqlGeneratorService` conserva la llamada común `ChatModel.call(Prompt)`. Se adaptan solo configuración y clasificación de excepciones si el cliente Google GenAI representa timeouts o errores upstream con tipos diferentes. El handler mantiene `503` para configuración ausente, `504` para timeout y `502` para fallos/respuestas inválidas; no registra mensajes crudos del SDK.

**Alternativa considerada:** inyectar `GoogleGenAiChatModel` directamente. Se descarta porque acopla el servicio a un proveedor sin beneficio para esta funcionalidad.

### 5. Validación automatizada y humo optativo

Las pruebas de unidad/controller usan `ChatModel` simulado para validar generación, fallos y que ningún error llegue a JDBC. Un test de contexto verifica que el starter produce un `ChatModel` con propiedades no secretas. La llamada real al proveedor es una verificación optativa, manual y basada en `GEMINI_API_KEY`/`GEMINI_MODEL` de entorno; no forma parte de la suite automática.

La arquitectura sigue siendo SPA -> Backend Java/Spring AI -> Oracle DB. JSqlParser conserva `SELECT`/`WITH` y `FETCH FIRST 500 ROWS ONLY`; JDBC continúa con privilegio de solo lectura. `oracle-mcp-server` permanece como herramienta de desarrollo para inspección y validación, no como runtime.

## Risks / Trade-offs

- [Riesgo: cambios de compatibilidad al pasar de Boot 3.3 a 3.5 y Spring AI 1.0-M2 a 1.1] → Mitigación: ejecutar la suite completa, compilar con Java 21 y revisar avisos deprecados; mantener el cambio limitado al backend.
- [Riesgo: el modelo configurado no está disponible en Gemini Developer API o para esa cuenta] → Mitigación: externalizar `GEMINI_MODEL` y validar con un smoke test optativo, sin codificar la API key.
- [Riesgo: la nueva SDK envuelve timeouts de forma distinta] → Mitigación: pruebas por categoría y clasificación conservadora de cualquier otro error como fallo upstream `502`.
- [Riesgo: credenciales históricas en YAML ya fueron expuestas] → Mitigación: rotarlas; eliminarlas del archivo y requerir `GEMINI_API_KEY` en el entorno.

## Migration Plan

1. Actualizar Spring Boot parent a `3.5.15`, Spring AI BOM a `1.1.8` y reemplazar el starter Vertex por Google GenAI.
2. Cambiar la configuración a `spring.ai.google.genai`, retirar las propiedades Vertex, externalizar `GEMINI_API_KEY` y `GEMINI_MODEL`, y actualizar la documentación de ejecución.
3. Ejecutar pruebas backend y verificar que solo existe un `ChatModel`, que la generación normal llega al guardrail, y que fallos de IA no ejecutan JDBC.
4. Opcionalmente ejecutar un smoke test real con credenciales rotadas suministradas fuera del repositorio.

Para rollback, restaurar conjuntamente el parent Boot `3.3.4`, BOM Spring AI `1.0.0-M2`, starter Vertex y configuración Vertex anterior. No restaurar credenciales literales; la API key debe seguir fuera del repositorio.
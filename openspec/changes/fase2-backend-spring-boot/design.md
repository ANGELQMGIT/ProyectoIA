# Design: Fase 2 - Core Backend Java 21 + Spring AI + Guardrails SQL

## Context

Véase `proposal.md` y `specs/backend-spring-reportes/spec.md`. En la Fase 1 se configuró la base de datos Oracle DB con el esquema de menor privilegio `USR_DBTEST_APP`, la vista unificada `V_VENTAS_DETALLADAS` y la tabla `CATALOGO_REPORTES`. La Fase 2 implementa el core backend en Java 21 con Spring Boot 3.3+ que procesa las consultas en lenguaje natural, invoca a Gemini AI mediante Spring AI, valida la sintaxis mediante JSqlParser AST Guardrails y ejecuta la consulta en Oracle DB.

## Goals / Non-Goals

**Goals:**
- Estructurar la arquitectura de backend Spring Boot 3.3+ en Java 21 usando Apache Maven (`pom.xml`, `mvnw`) con Virtual Threads habilitados (`spring.threads.virtual.enabled: true`).
- Implementar DTOs inmutables utilizando Java 21 Records (`ReporteSummaryDto`, `EjecutarReporteRequest`, `EjecutarReporteResponse`, `ColumnaMetadataDto`).
- Diseñar la pipeline en `DynamicReportService`: Recuperación Metadatos -> Construcción Prompt -> Generación Gemini AI -> Sanitización -> Guardrail JSqlParser AST -> Ejecución JDBC -> Formateo JSON.
- Implementar el módulo independiente `SqlSecurityGuardrailService` respaldado por **JSqlParser**.
- Configurar el cliente JDBC con el usuario `USR_DBTEST_APP`.
- Exponer los endpoints REST `GET /api/v1/reportes` y `POST /api/v1/reportes/ejecutar` con `@RestControllerAdvice` global.

**Non-Goals:**
- Desarrollo de la interfaz gráfica SPA (Fase 3).
- Carga completa de los 40 reportes en el catálogo (Fase 4).
- Creación de vistas materializadas en Oracle DB (Fase 5).

## Decisions

### 1. Virtual Threads en Java 21 & Spring Boot 3.3+
- **Decisión:** Activar `spring.threads.virtual.enabled: true` en `application.yml`.
- **Justificación:** Optimiza el rendimiento al procesar llamadas I/O intensivas (invocación HTTP a Gemini AI y consultas JDBC a Oracle DB) sin bloquear hilos del sistema operativo.
- **Alternativa Considerada:** Thread Pool tradicional de Tomcat. Rechazado por mayor consumo de memoria bajo carga masiva.

### 2. DTOs Inmutables mediante Java 21 Records
- **Decisión:** Definir todos los objetos de transferencia de datos como `record`.
```java
public record ReporteSummaryDto(Long id, String nombre, String categoria, String descripcion) {}
public record EjecutarReporteRequest(Long idReporte, String promptUsuario) {}
public record ColumnaMetadataDto(String nombre, String tipoDato) {}
public record EjecutarReporteResponse(
    Long idReporte,
    String nombreReporte,
    String sqlEjecutado,
    List<ColumnaMetadataDto> columnas,
    List<Map<String, Object>> filas,
    long tiempoEjecucionMs
) {}
```
- **Justificación:** Inmutabilidad garantizada, sintaxis limpia sin boilerplate de getters/setters/constructores.

### 3. Pipeline de Procesamiento en `DynamicReportService`
- **Decisión:** Implementar un flujo secuencial estricto de 5 pasos:
  1. **Fetch Metadata:** `ReporteRepository` obtiene `PROMPT_SISTEMA` y `QUERY_BASE` de `CATALOGO_REPORTES` via JDBC (`USR_DBTEST_APP`).
  2. **Prompt Construction & Gemini Call:** Invocación a Gemini via Spring AI `ChatModel` inyectando reglas de negocio + prompt del usuario.
  3. **SQL Clean Up:** Eliminación de delimitadores Markdown (```sql ... ```) y espacios sobrantes.
  4. **AST Guardrail Validation (JSqlParser):** `SqlSecurityGuardrailService.validateAndSanitize(sql)` verifica que la AST contenga únicamente `SELECT` o `WITH`, prohíbe DDL/DML/comentarios e inyecta `FETCH FIRST 500 ROWS ONLY`.
  5. **JDBC Execution & Dynamic Formatting:** `JdbcTemplate.query()` ejecuta la consulta y extrae `ResultSetMetaData` para mapear tipos y datos dinámicamente.

### 4. Estrategia de Guardrails AST con JSqlParser
- **Decisión:** Usar `net.sf.jsqlparser.parser.CCJSqlParserUtil` para parsear el SQL en un árbol sintáctico abstracto (AST).
- **Validaciones:**
  - `Statement` debe ser instancia de `Select`.
  - Recorrido del AST comprobando que no existan nodos de tipo `Insert`, `Update`, `Delete`, `Drop`, `Create`, `Alter`, `Truncate`, `Execute`, `Grant`.
  - Verificación de ausencia de comentarios SQL en la cadena original (`--`, `/*`).
  - Inyección defensiva: Si la sentencia `Select` no contiene cláusula `Fetch` ni `Limit`, se agrega `Fetch` con count 500 (`FETCH FIRST 500 ROWS ONLY`).

### 5. Manejo Global de Excepciones (`GlobalExceptionHandler`)
- **Decisión:** `@RestControllerAdvice` capturando:
  - `SqlSecurityViolationException` -> HTTP 400 Bad Request (Detalles del rechazo por Guardrail).
  - `ReporteNotFoundException` -> HTTP 404 Not Found.
  - `DataAccessException` / `SQLException` -> HTTP 500 Internal Server Error (Mensajes sanitizados).
  - `TransientAiException` -> HTTP 504 Gateway Timeout.

## Risks / Trade-offs

- **[Riesgo: Gemini AI genera SQL con dialecto no compatible con Oracle DB]** → **Mitigación:** En el `PROMPT_SISTEMA` se especifica explícitamente el dialecto Oracle 19c (`FETCH FIRST 500 ROWS ONLY`, `TO_CHAR`, `JOIN` ANSI).
- **[Riesgo: Consultas lentas por agregaciones pesadas en Oracle]** → **Mitigación:** Inyección obligatoria de `FETCH FIRST 500 ROWS ONLY` y uso exclusivo de la vista precalculada `V_VENTAS_DETALLADAS`.
- **[Riesgo: Inyección de SQL mediante Prompts de Usuario (Prompt Injection)]** → **Mitigación:** El usuario no concatena SQL directo; el filtro AST de JSqlParser evalúa la sintaxis final antes de tocar la base de datos.

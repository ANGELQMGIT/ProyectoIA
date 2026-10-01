# Proposal: Fase 2 - Core Backend Java 21 + Spring AI + Guardrails SQL

## Why

Una vez establecida la base de datos Oracle DB y el catálogo piloto en la Fase 1, se requiere construir la capa de servicios backend en Java 21 y Spring Boot 3.3+. Esta capa se encargará de orquestar la integración con Google Gemini AI a través de Spring AI, aplicar guardrails estrictos de seguridad AST (JSqlParser) sobre las sentencias SQL generadas, y exponer los endpoints REST necesarios para la ejecución dinámica de reportes.

## What Changes

- **Inicialización de Proyecto Spring Boot 3.3+ con Java 21**: Configuración del proyecto mediante Apache Maven (`pom.xml`, Maven Wrapper `mvnw`), Virtual Threads habilitados (`spring.threads.virtual.enabled: true`), Java Records y Pattern Matching.
- **Integración de Spring AI con Google Gemini**: Conexión al modelo `gemini-1.5-pro` (o `flash`) mediante `spring-ai-google-genai` / `spring-ai-vertex-ai-gemini-spring-boot-starter`.
- **Módulo de Guardrails AST SQL con JSqlParser**:
  - Validación de sintaxis SQL estricta permitiendo únicamente declaraciones `SELECT` o `WITH`.
  - Rechazo inmediato de sentencias destructivas DDL/DML (`INSERT`, `UPDATE`, `DELETE`, `DROP`, `ALTER`, `TRUNCATE`, `GRANT`, `CREATE`, `EXEC`) y comentarios SQL (`--`, `/*`).
  - Inyección defensiva obligatoria de la cláusula `FETCH FIRST 500 ROWS ONLY` si no está presente.
- **Servicios de Negocio y Datos**:
  - `ReporteService`: Recuperación de metadatos de reportes desde la tabla `CATALOGO_REPORTES` mediante conexión JDBC (`USR_DBTEST_APP`).
  - `AiSqlGeneratorService`: Construcción del prompt enriquecido combinando el prompt del usuario + query base del catálogo e invocación a Gemini AI.
  - `SqlExecutionService`: Ejecución segura de la query SQL validada y mapeo dinámico de filas/columnas.
- **Endpoints API REST**:
  - `GET /api/v1/reportes`: Listado ordenado y categorizado del catálogo de reportes activos.
  - `POST /api/v1/reportes/ejecutar`: Recepción del prompt en lenguaje natural, generación AI, filtrado JSqlParser y retorno de la grilla de datos y SQL ejecutado.

## Capabilities

### New Capabilities

- `backend-spring-reportes`: Cubre la configuración del backend Java 21 con Virtual Threads, los endpoints REST `/api/v1/reportes`, el orquestador de Spring AI con Gemini, la capa de persistencia JDBC de menor privilegio y los Guardrails de Seguridad SQL con JSqlParser.

### Modified Capabilities

*(Ninguna. La capacidad `oracle-reportes-db` construida en la Fase 1 se mantiene intacta)*

## Impact

- **Código Java/Backend**: Creación del proyecto backend en la carpeta del repositorio.
- **Dependencias**: Spring Boot 3.3+, Spring AI starter, JSqlParser, Oracle JDBC Driver (`ojdbc11`), Jackson.
- **Seguridad & BD**: Uso exclusivo de la conexión JDBC configurada con el usuario de menor privilegio `USR_DBTEST_APP`.
- **APIs**: Contratos REST definidos para interactuar con la futura SPA (Fase 3).

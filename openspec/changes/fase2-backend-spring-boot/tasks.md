# Tasks: Fase 2 - Core Backend Java 21 + Spring AI + Guardrails SQL

## 1. Setup de Proyecto Spring Boot & Configuración Java 21

- [x] 1.1 Inicializar la estructura del proyecto backend Spring Boot 3.3+ con Java 21 usando Apache Maven (`pom.xml`), Maven Wrapper (`mvnw`), Virtual Threads (`spring.threads.virtual.enabled: true`) y dependencias en `pom.xml`. Verificar con compilación limpia (`./mvnw clean compile` o `mvn clean compile`).
- [x] 1.2 Configurar `application.yml` con el Datasource JDBC apuntando a Oracle DB (`USR_DBTEST_APP`) usando el driver `ojdbc11`. Verificar la inicialización del HikariCP Connection Pool durante el arranque.

## 2. Módulo de Guardrails AST de Seguridad SQL con JSqlParser

- [x] 2.1 Crear `SqlSecurityGuardrailService` respaldado por JSqlParser para parsear y validar la AST de las sentencias SQL (permitir solo `SELECT`/`WITH`, prohibir DDL/DML/comentarios e inyectar `FETCH FIRST 500 ROWS ONLY`). Verificar mediante suite JUnit 5 con pruebas de inyección y sentencias destructivas.

## 3. Integración con Spring AI (Google Gemini)

- [x] 3.1 Configurar la integración de Spring AI (`spring-ai-google-genai` / Vertex AI) con `gemini-1.5-pro` y desarrollar `AiSqlGeneratorService` para la generación Text-to-SQL a partir del prompt del usuario y la query base. Verificar mediante invocación de prueba a la API de Gemini.

## 4. Capa de Servicios de Negocio & Persistencia JDBC

- [x] 4.1 Definir los DTOs inmutables en Java 21 Records (`ReporteSummaryDto`, `EjecutarReporteRequest`, `EjecutarReporteResponse`, `ColumnaMetadataDto`) y crear `ReporteRepository` para leer de `CATALOGO_REPORTES`. Verificar consulta al catálogo mediante JDBC.
- [x] 4.2 Desarrollar `DynamicReportService` orquestando la pipeline completa (Recuperación metadatos -> Prompt Gemini -> Sanitización -> Guardrail JSqlParser -> Ejecución `JdbcTemplate` -> Formateo dinámico de `ResultSetMetaData`). Verificar la ejecución completa de extremo a extremo.

## 5. Exposición de API REST y Manejo de Excepciones

- [x] 5.1 Crear `ReporteController` con los endpoints REST `GET /api/v1/reportes` y `POST /api/v1/reportes/ejecutar`. Verificar el contrato JSON de respuesta mediante pruebas con MockMvc / RestTemplate.
- [x] 5.2 Implementar `GlobalExceptionHandler` (`@RestControllerAdvice`) capturando `SqlSecurityViolationException` (HTTP 400), `ReporteNotFoundException` (HTTP 404) y fallos de infraestructura. Verificar enviando un prompt que provoque una violación de seguridad SQL y confirmando la respuesta HTTP 400.

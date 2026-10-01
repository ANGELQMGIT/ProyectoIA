# Java 21, Spring AI & Backend Security Specialist Agent Rule

Role Stance: Advisory & Code Review Only. This agent guides Java 21 backend architecture, Spring AI Gemini integration, REST contract design, and SQL Guardrails. It NEVER mutates or writes source code directly without user authorization in an execution workflow.

## 1. Java 21 & Spring Boot 3.3+ Core Standards
- **Virtual Threads**: Enable Virtual Threads (`spring.threads.virtual.enabled=true`) for non-blocking concurrent I/O.
- **Modern Java Idioms**: Use `record` classes for immutable DTOs, `Text Blocks` (`"""..."""`) for SQL and Prompts, and `Pattern Matching`.
- **Architecture**: Clean layered structure (`Controller` -> `Service` -> `GuardrailValidator` -> `Spring AI / JdbcTemplate`).

## 2. Spring AI & Google Gemini Integration
- **Dependency**: `spring-ai-vertex-ai-gemini-spring-boot-starter` or `spring-ai-google-genai`.
- **Model**: `gemini-1.5-pro` (for complex structural Text-to-SQL) or `gemini-1.5-flash`.
- **Temperature**: Low setting (`0.1`) for consistent, deterministic SQL generation.
- **Prompt Engineering**: Combine catalog `PROMPT_SISTEMA`, `QUERY_BASE`, and user natural language input into a strict Text-to-SQL context.

## 3. SQL Guardrails & Security Engine (JSqlParser)
- **AST Parsing**: Parse all AI-generated SQL strings using **JSqlParser** prior to JDBC execution.
- **Query Type Restriction**: Accept ONLY `SELECT` or `WITH` statements.
- **Prohibited Statements**: Reject any statement containing `INSERT`, `UPDATE`, `DELETE`, `DROP`, `ALTER`, `TRUNCATE`, `EXEC`, `CREATE`, `GRANT`.
- **Prohibited Comments**: Strip or reject SQL comments (`--`, `/* ... */`).
- **Defensive Row Limits**: Enforce maximum row fetching by appending `FETCH FIRST 500 ROWS ONLY`.
- **Security Exceptions**: Throw custom `InvalidQueryException` on validation failure and handle via `@RestControllerAdvice`.

## 4. JDBC & Credentials
- **Restricted Database Account**: Connect using `USR_DBTEST_APP` credentials.
- **Query Execution**: Use `JdbcTemplate.queryForList()` to execute validated SQL safely and return dynamic row maps.
- **CORS**: Configure global CORS allowing `GET`, `POST`, `OPTIONS` from frontend origins.
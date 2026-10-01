# Testing & Quality Assurance Specialist Agent Rule

Role Stance: Advisory & Test Design Only. This agent formulates test strategies, unit test suites, integration tests, and security penetration test cases. It NEVER modifies test files directly without authorization.

## 1. Backend Security & Guardrail Unit Testing (JUnit 5 + Mockito)
- **SQL Injection & DDL/DML Attack Tests**:
  - Test JSqlParser against malicious inputs: `DROP TABLE CLIENTES`, `UPDATE VENTAS SET ...`, `; DELETE FROM ...`, `-- comment injection`, `/* block comment */`.
  - Assert that `InvalidQueryException` is thrown for any disallowed statement.
- **Defensive Limits Verification**: Assert that `FETCH FIRST 500 ROWS ONLY` is appended if absent.
- **REST Controller Integration**: Use `@WebMvcTest` and `MockMvc` to verify endpoint contracts (`GET /api/v1/reportes`, `POST /api/v1/reportes/ejecutar`).
- **Virtual Threads Testing**: Verify concurrent handling of AI service calls under load without thread starvation.

## 2. Frontend Component & Integration Testing
- **Dynamic Grid Rendering**: Test column auto-inference with various JSON payload structures.
- **Export Utilities**: Verify CSV and Excel blob generation logic.
- **Error Boundary Testing**: Verify UI resilience when backend returns 400 SQL Validation errors or 500 AI timeouts.

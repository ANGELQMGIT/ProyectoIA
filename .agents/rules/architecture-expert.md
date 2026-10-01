# System Architecture Specialist Agent Rule

Role Stance: Advisory & Architectural Review Only. This agent evaluates overall system design, layer isolation, SOLID principles, Clean Code standards, and Non-Functional Requirements (NFRs). It NEVER applies architectural changes directly to the codebase.

## 1. Architectural Pattern & Layering
- **Decoupled 3-Tier Architecture**:
  1. **Presentation Layer**: SPA Frontend (Category Navigation, Prompt Console, Dynamic Grid, Export).
  2. **Application Layer**: Java 21 REST API, Virtual Threads, Spring AI Gemini, JSqlParser SQL Guardrails.
  3. **Data Layer**: Oracle DB (`USR_DBTEST` Owner Schema, `USR_DBTEST_APP` Read-only User, `TS_DBTEST_DAT`, `TS_DBTEST_IDX`).

## 2. Security Multi-Layering
- **Layer 1 (AST Guardrails)**: JSqlParser validates syntax before DB call.
- **Layer 2 (DB Privileges)**: `USR_DBTEST_APP` user restricted strictly to `GRANT SELECT`.
- **Layer 3 (CORS & Input Validation)**: Restrictive HTTP CORS headers and request DTO validation.

## 3. Non-Functional Requirements (NFRs)
- **Performance & Concurrency**: Java 21 Virtual Threads for non-blocking I/O.
- **Scalability**: Evolution path to Oracle Materialized Views (`MV_`) with `DBMS_SCHEDULER` for high-volume reports.
- **Observability**: SLF4J/Logback audit logging for prompt requests and generated SQL execution times.

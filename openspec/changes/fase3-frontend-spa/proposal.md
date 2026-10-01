# Proposal

## Why

This change addresses Phase 3 of the "Motor Dinámico de Reportes Orientado a Prompts" project, which focuses on delivering the user-facing SPA (Single Page Application). Having completed the Database setup (Phase 1) and Backend API (Phase 2), we now need a premium frontend to allow end users to interact with the system, submit natural language prompts, and view AI-generated SQL results in a rich, dynamic data grid.

## What Changes

- **Frontend SPA initialization**: Create a Vite+React or Vanilla JS application with a modern tech stack.
- **Premium UX/UI**: Implement a dynamic aesthetic featuring Glassmorphism, Dark Mode, and modern typography (Google Fonts like Inter or Roboto).
- **Data Visualization**: Integrate a robust data grid (AG-Grid or TanStack Table) capable of handling dynamic `ResultSetMetaData` (variable columns based on AI-generated SQL) and exporting to CSV/Excel.
- **SQL Inspector**: Provide a UI component (SQL Inspector) allowing users to view the generated AI SQL query before or after execution for transparency.
- **API Integration**: Connect the SPA to the existing Spring Boot backend REST API (`GET /api/v1/reportes` for the catalog and `POST /api/v1/reportes/ejecutar` for execution).
- **Architecture & Rules Alignment**: Maintain the strict 3-tier architecture. Acknowledge and rely on Backend Guardrails (JSqlParser) for security, ensuring no DDL/DML is allowed.
- **MCP usage**: Highlight the role of `oracle-mcp-server` during development for testing schemas and queries.

### Non-Goals
- **No Direct DB Access**: The frontend will not communicate directly with Oracle DB; all traffic flows through the backend.
- **No DDL/DML Modification**: The AI and frontend will not allow modification of database schemas or data. Security and rejection of destructive queries are enforced by the backend's `SqlSecurityGuardrailService`.

## Capabilities

### New Capabilities
- `frontend-spa-reportes`: Frontend Single Page Application that consumes the backend API to provide a Text-to-SQL premium user interface, including dynamic data grids and a SQL inspector.

### Modified Capabilities

## Impact

- Introduces a new `frontend/` directory housing the SPA project (Vite build).
- Connects to the existing Backend API, completing the 3-tier architecture.
- Delivers the final user experience, fulfilling the project's Text-to-SQL dynamic reporting goals.

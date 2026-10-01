# Design

## Context

The backend REST API and Oracle DB are fully implemented and operational (Phases 1 and 2). The system requires a frontend to capture natural language prompts, display AI-generated SQL for transparency, and render dynamic results. The UI must follow a premium aesthetic (Dark Mode, Glassmorphism, Google Fonts). See `proposal.md` for motivation.

## Goals / Non-Goals

**Goals:**
- Deliver a responsive SPA that acts as the primary user interface for the Text-to-SQL engine.
- Provide a robust data grid capable of rendering dynamic columns based on unpredictable SQL `ResultSetMetaData`.
- Offer a premium user experience with skeleton loaders, elegant error handling, and a modern dark theme.
- Enable data export (CSV/Excel) from the grid.

**Non-Goals:**
- User authentication/authorization (out of scope for this phase).
- Storing query history in the frontend (the backend catalog manages base queries).
- Admin dashboards for managing the database or catalog (the frontend is strictly for executing Text-to-SQL reporting).

## Decisions

### 1. Framework and Build Tool
**Decision**: Vite + Vanilla JS (or lightweight React).
**Rationale**: Vite provides an ultra-fast development environment and optimized production builds. Vanilla JS with modern ES6+ is sufficient for the scope of this application and avoids React overhead, but React can be utilized if component complexity grows.
**Alternatives**: Next.js (overkill for a pure CSR SPA with a single dashboard), Create React App (deprecated).

### 2. Styling Strategy
**Decision**: Vanilla CSS with CSS Variables.
**Rationale**: Ensures maximum flexibility for implementing the required Glassmorphism and Dark Mode aesthetics. We will use Google Fonts (Outfit or Inter) and define a dark color palette via CSS variables (`--bg-dark`, `--glass-bg`, `--glass-border`).
**Alternatives**: TailwindCSS (avoided unless explicitly requested to keep styles decoupled and bespoke).

### 3. Data Grid Component
**Decision**: AG-Grid Community Edition.
**Rationale**: AG-Grid excels at handling dynamic data sources where column definitions are not known at compile time. It natively supports sorting, filtering, and CSV export, mapping perfectly to our requirement of rendering dynamic `ResultSetMetaData`.
**Alternatives**: TanStack Table (excellent, but requires more boilerplate to achieve the same dynamic rendering and export features as AG-Grid out of the box).

### 4. UI/UX Elements
**Decision**: Skeleton loaders and elegant toast notifications.
**Rationale**: Text-to-SQL processing via Gemini can take a few seconds. Skeleton loaders maintain user engagement during the `POST /api/v1/reportes/ejecutar` request.
**Alternatives**: Simple spinners (less premium feel).

## Risks / Trade-offs

- **Risk**: Variable SQL execution times (AI generation + DB execution) leading to perceived slowness.
  **Mitigation**: Implement engaging skeleton loaders and progress indicators in the UI.
- **Risk**: Massive datasets crashing the browser.
  **Mitigation**: The backend Guardrail already forces `FETCH FIRST 500 ROWS ONLY`, ensuring the frontend grid never receives overwhelming payloads.
- **Risk**: Unpredictable column names from AI queries.
  **Mitigation**: AG-Grid will be configured to auto-generate column definitions directly from the JSON keys in the response data.

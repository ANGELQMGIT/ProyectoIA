# Design

## Context

The backend REST API and Oracle DB are fully implemented and operational (Phases 1 and 2). The system requires a frontend to capture natural language prompts, display AI-generated SQL for transparency, and render dynamic results. The UI must follow a premium aesthetic (Dark Mode, custom CSS tokens, Google Fonts). See `proposal.md` for motivation.

## Goals / Non-Goals

**Goals:**
- Deliver a responsive SPA that acts as the primary user interface for the Text-to-SQL engine.
- Provide a robust data grid capable of rendering dynamic columns based on unpredictable SQL `ResultSetMetaData`.
- Offer a premium user experience with skeleton loaders, elegant error handling, and a modern dark theme.
- Enable data export (CSV) from the results table.
- Provide a sidebar-based navigation for the reports catalog, responsive on mobile devices.

**Non-Goals:**
- User authentication/authorization (out of scope for this phase).
- Storing query history in the frontend (the backend catalog manages base queries).
- Admin dashboards for managing the database or catalog (the frontend is strictly for executing Text-to-SQL reporting).

## Decisions

### 1. Framework and Build Tool
**Decision**: Vite + React (JSX).
**Rationale**: Vite provides an ultra-fast development environment and optimized production builds. React is used for its component model, simplifying state management for the catalog, prompt, results, and sidebar open/close flows.
**Alternatives**: Next.js (overkill for a pure CSR SPA), Vanilla JS (viable but more boilerplate for reactive state).

### 2. Styling Strategy
**Decision**: Vanilla CSS with CSS Custom Properties (design token system).
**Rationale**: Maximum flexibility for implementing the Dark Mode aesthetic. Token-based palette (slate-950 base, sky-500 accent) defined in `styles.css`. Typography uses **Plus Jakarta Sans** (UI text) and **JetBrains Mono** (SQL/mono contexts) loaded from Google Fonts.
**Alternatives**: TailwindCSS (avoided to keep styles fully decoupled and bespoke, aligned with project rules).

### 3. Data Grid / Results Table
**Decision**: Native HTML `<table>` with dynamic column inference from `ResultSetMetaData`.
**Rationale**: The backend response already provides column metadata (`columnas[]`) and row data (`filas[]`). A native table driven by React state is lighter, fully controllable via CSS tokens, and avoids a heavy third-party dependency (AG-Grid was removed). Sorting and filtering are handled by a client-side live filter input.
**Alternatives**: AG-Grid Community Edition (evaluated and removed — excessive weight for the column set; native table satisfies all dynamic rendering and export requirements).

### 4. Sidebar Layout (Responsive Catalog Navigation)
**Decision**: Fixed sidebar on desktop (288px), collapsible with hamburger button + backdrop overlay on mobile.
**Rationale**: The reports catalog is categorized (VENTAS, FINANZAS, OPERACIONES, etc.) and requires persistent visibility on desktop for quick switching. On mobile, a slide-in panel with a dark backdrop overlay provides the same access without consuming screen real estate.
**Alternatives**: Tabs (loses category hierarchy), Dropdown select (loses discoverability).

### 5. Quick-Prompt Chips
**Decision**: Context-aware suggestion chips rendered below the prompt textarea.
**Rationale**: Reduces friction for first-time users by providing ready-to-use prompt examples. Chips populate the textarea on click. The `sugerencias` field on the report object is used when available; a static fallback set is used otherwise.
**Alternatives**: Placeholder text (less interactive, non-clickable).

### 6. UX Feedback Layer (Toast, SQL Copy, Execution Metrics)
**Decision**: 
- **Toast notifications**: animated slide-in/out notifications for execute, copy, and export actions.
- **Execution metrics**: prompt used and response time (`tiempoEjecucionMs`) displayed inline in the results header.
- **Copy SQL**: button with visual feedback ("¡Copiado!" state) using the Clipboard API.
- **DB status indicator**: animated pulsing dot in sidebar footer indicating Oracle connection state.
**Rationale**: Text-to-SQL processing takes 1–4 seconds. Clear feedback at every step (loading spinner, toast on completion, time display) keeps the user informed and builds trust in the system.

## Risks / Trade-offs

- **Risk**: Variable SQL execution times (AI generation + DB execution) leading to perceived slowness.
  **Mitigation**: Spinner on the execute button + animated skeleton messaging during `POST /api/v1/reportes/ejecutar`.
- **Risk**: Massive datasets arriving in the browser.
  **Mitigation**: The backend Guardrail forces `FETCH FIRST 500 ROWS ONLY`; the frontend live-filter operates client-side on those bounded rows.
- **Risk**: Unpredictable column names from AI queries.
  **Mitigation**: Columns are inferred directly from `result.columnas[]` keys; no hardcoded column definitions exist in the frontend.

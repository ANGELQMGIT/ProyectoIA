# Design

## Context

See proposal.md - Why and What Changes. The project already includes the Oracle data model and the backend endpoints for catalogue retrieval and report execution. The frontend gap is the user-facing layer that turns these capabilities into a usable operational experience.

## Goals / Non-Goals

**Goals:**
- Provide a single-page experience for browsing active reports and submitting natural-language prompts.
- Show execution status, dynamic table output, and the generated SQL used by the backend.
- Support exporting results and surface actionable errors for analysts and operators.

**Non-Goals:**
- Allowing direct SQL editing or data mutation from the browser.
- Replacing backend validation and guardrails with client-side logic.
- Designing a multi-page CMS or admin panel beyond the report execution workflow.

## Decisions

### 1. SPA shell centered on report selection and results
The interface will be organized around a two-panel layout: a left-side catalog or category navigator and a right-side execution workspace. This keeps the user’s primary action (choose a report, then ask for data) visible while maintaining enough room to inspect query results and metadata without context switching.

**Why**: The backend already exposes structured report metadata; the frontend should minimize cognitive load and let analysts move from discovery to execution quickly.

**Alternatives considered**:
- A single full-page form: simpler but weaker for large catalogs and multi-step diagnostics.
- A dashboard-centric design: broader but less focused on the prompt-to-result flow that defines this change.

### 2. State-driven UI with explicit loading and error states
The frontend will manage request lifecycle states explicitly: idle, loading, success, and error. Empty states, skeleton placeholders, and inline banners will be used instead of silent failures.

**Why**: The report-generation flow depends on remote execution and may fail for business, security, or connectivity reasons. Clear state boundaries reduce confusion and support troubleshooting.

**Alternatives considered**:
- Minimal toasts only: less discoverable and harder to debug for multi-step flows.
- Inline-only errors without empty/loading states: more compact but less robust in production use.

### 3. Dynamic grid over fixed schema assumptions
The table component will render from the backend response metadata rather than hardcoded columns. This aligns with the dynamic nature of report execution and reduces the need to predefine every report structure in the UI.

**Why**: Each report may produce different columns, and the backend response already includes column metadata.

**Alternatives considered**:
- Static column definitions per report: brittle and not scalable for the catalog-driven model.
- Rendering raw JSON only: functional but poor for analyst workflows and less user-friendly.

### 4. Premium dark theme with operational ergonomics
The visual design will use a dark glassmorphism-inspired aesthetic with high-contrast text, semantic color accents, and clear type hierarchy. This matches the project’s design direction while prioritizing readability and performance.

**Why**: The system is data-intensive and often used during focused analysis sessions; readability, contrast, and calm visual treatment matter more than decorative novelty.

**Alternatives considered**:
- Light theme only: less aligned with the product direction described in the project context.
- Dense enterprise table UI without polish: functional but not consistent with the premium UX target.

## Risks / Trade-offs

- [API response variability] → Mitigation: normalize column metadata at the client boundary and treat missing fields as empty values with explicit fallbacks.
- [Long-running AI query generation] → Mitigation: show loading progress, cancellation states, and a retry path when requests fail after timeout.
- [Export complexity across browsers] → Mitigation: keep exports as standard CSV/Excel file generation from the resolved data set and validate the file output in QA.
- [Security perception risk] → Mitigation: keep SQL inspection read-only and continue to enforce backend guardrails; the UI will display the executed SQL but never allow direct mutation.

## Migration Plan

1. Scaffold the frontend app and align its configuration with the existing backend base URL and CORS policy.
2. Implement the catalog-loading flow and report selection state from the backend API.
3. Add the execution workflow, success/error states, and dynamic result grid.
4. Add SQL inspection and export tools to complete the analyst workflow.
5. Validate the end-to-end flow against the backend endpoints and the existing Oracle-backed dataset.

## Open Questions

- No open questions are blocking this phase. The required behavior is already defined by the backend contract and the project context.

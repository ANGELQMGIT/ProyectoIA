# Tasks

## 1. Frontend Setup and API Contract

- [x] 1.1 Create the SPA project structure and confirm the app can boot with the expected dependencies and environment variables for the backend URL and API base path.
- [x] 1.2 Implement the service layer that consumes `GET /api/v1/reportes` and `POST /api/v1/reportes/ejecutar`, and verify the HTTP contract matches the backend responses.
- [x] 1.3 Add global app shell styles for the dark premium theme, responsive layout, empty states, and error banners, and verify the UI renders correctly at the primary breakpoints.

## 2. Report Discovery and Execution UX

- [x] 2.1 Build the catalog view with report categories, metadata cards, and selection behavior, and verify that clicking a report populates the execution context.
- [x] 2.2 Implement the prompt form and execution flow, including loading, success, and failure states, and verify the user can submit a valid report request end-to-end.
- [x] 2.3 Render the dynamic result grid from backend metadata and data rows, and verify that columns and values display correctly without hardcoded assumptions.

## 3. Audit, Export, and Hardening

- [x] 3.1 Add the SQL inspector and execution trace panel, and verify it shows the approved SQL and relevant execution metadata when available.
- [x] 3.2 Implement CSV/Excel export actions for the current result set and verify the downloaded file includes the visible rows and columns in a usable format.
- [x] 3.3 Validate the full flow against the backend API, including bad prompts, empty results, and backend error responses, and confirm the UX remains clear and actionable.

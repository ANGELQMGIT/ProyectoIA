# Tasks

## 1. Project Initialization

- [x] 1.1 Initialize Vite + React project in the `frontend/` directory, configure `index.html` and `styles.css` with CSS custom properties for Dark Mode, Glassmorphism, and Google Fonts (Inter/Outfit). Verify by running `npm run dev` and confirming the dark aesthetic in the browser.

## 2. API Integration & Services

- [x] 2.1 Create an `api.js` service to communicate with the Spring Boot backend (`GET /api/v1/reportes` and `POST /api/v1/reportes/ejecutar`). Verify by successfully fetching the catalog and logging it to the browser console.

## 3. UI Components Implementation

- [x] 3.1 Implement the Prompt Console (base query dropdown + text input) and a visual Skeleton Loader. Verify visually that the skeleton loader appears during a simulated API call.
- [x] 3.2 Integrate AG-Grid Community to dynamically map `ResultSetMetaData` (columns) and rows from the backend response into a reactive data grid. Verify by passing a mock JSON response and checking if columns auto-generate correctly.
- [x] 3.3 Implement the SQL Inspector UI component (e.g., a modal or collapsible panel) to display the AI-generated SQL query. Verify by toggling its visibility and confirming the SQL formatting.
- [x] 3.4 Add "Export to CSV" functionality utilizing AG-Grid's built-in export API. Verify by clicking the export button and successfully downloading a CSV file containing the grid's data.

## 4. End-to-End Integration

- [x] 4.1 Connect all UI components to the `api.js` service to achieve the full Text-to-SQL flow (select base query -> input prompt -> submit -> show skeleton -> render grid & SQL). Verify by submitting a real prompt to the running backend and viewing the dynamic results in the grid.

## 5. UX Redesign — Premium UI (based on ai_studio_code.html reference)

- [x] 5.1 Update typography system: replace Inter/Outfit with **Plus Jakarta Sans** (UI) and **JetBrains Mono** (SQL/mono) in `index.html` and rebuild the full CSS token system in `styles.css` (slate-950 base palette, sky-500 accent, CSS custom properties for all colors, radii, shadows, and fonts).
- [x] 5.2 Implement responsive **sidebar layout**: fixed 288px sidebar with categorized report list (icons per category: Finanzas/Operaciones/Ventas), collapsible on mobile via hamburger button, backdrop overlay, and animated slide transition. Includes DB connection indicator with animated pulse in footer.
- [x] 5.3 Replace AG-Grid with a **native HTML `<table>`** driven by `result.columnas[]` and `result.filas[]`. Implement **live filter input** that client-side filters rows in real time. Remove `ag-grid-community` and `ag-grid-react` from `package.json`. Implement native CSV export using Blob + URL.createObjectURL.
- [x] 5.4 Add **quick-prompt chips** below the prompt textarea (clickable suggestions that populate the textarea). Display **execution metrics** (prompt used + `tiempoEjecucionMs`) in the results card header after each execution.
- [x] 5.5 Add **toast notification** system (animated slide-in/out, auto-dismiss after 2.6s) for execute, copy SQL, and export CSV actions. Add **Copy SQL button** with Clipboard API and visual "¡Copiado!" feedback state. Display pagination status ("Mostrando N de M registros") in table footer.

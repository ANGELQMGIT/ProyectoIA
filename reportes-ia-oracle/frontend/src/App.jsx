import { useEffect, useMemo, useRef, useState, useCallback } from 'react';
import './styles.css';
import { apiService } from './api.js';
import { formatApiError } from './errorResponse.js';

/* ── Icons ─────────────────────────────────────────── */
const IconSparkles = () => (
  <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
    <path strokeLinecap="round" strokeLinejoin="round" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09zM18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 00-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 002.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 002.456 2.456L21.75 6l-1.035.259a3.375 3.375 0 00-2.456 2.456z" />
  </svg>
);

const IconChevron = () => (
  <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
    <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
  </svg>
);

const IconClose = () => (
  <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
    <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
  </svg>
);

const IconMenu = () => (
  <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
    <path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16M4 18h16" />
  </svg>
);

const IconPlay = () => (
  <svg viewBox="0 0 24 24" fill="currentColor">
    <path d="M8 5v14l11-7z" />
  </svg>
);

const IconCode = () => (
  <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
    <path strokeLinecap="round" strokeLinejoin="round" d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" />
  </svg>
);

const IconDownload = () => (
  <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
    <path strokeLinecap="round" strokeLinejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
  </svg>
);

const IconSearch = () => (
  <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
    <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
  </svg>
);

const IconCopy = () => (
  <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
    <path strokeLinecap="round" strokeLinejoin="round" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
  </svg>
);

const IconCheck = () => (
  <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
    <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
  </svg>
);

const IconClock = () => (
  <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
    <path strokeLinecap="round" strokeLinejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
  </svg>
);

/* ── Category icons ─────────────────────────────────── */
const IconFinanzas = () => (
  <svg className="icon-finanzas" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
    <path strokeLinecap="round" strokeLinejoin="round" d="M12 6v12m-3-2.818l.879.659c1.171.879 3.07.879 4.242 0 1.172-.879 1.172-2.303 0-3.182C13.536 12.219 12.768 12 12 12c-.725 0-1.45-.22-2.003-.659-1.106-.879-1.106-2.303 0-3.182s2.9-.879 4.006 0l.415.33M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
  </svg>
);

const IconOperaciones = () => (
  <svg className="icon-ops" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
    <path strokeLinecap="round" strokeLinejoin="round" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
  </svg>
);

const IconVentas = () => (
  <svg className="icon-ventas" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
    <path strokeLinecap="round" strokeLinejoin="round" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
  </svg>
);

const CATEGORY_ICONS = {
  FINANZAS:    <IconFinanzas />,
  OPERACIONES: <IconOperaciones />,
  VENTAS:      <IconVentas />,
};

/* ── Toast hook ─────────────────────────────────────── */
function useToast() {
  const [toast, setToast] = useState({ visible: false, message: '' });
  const timerRef = useRef(null);

  const showToast = useCallback((message) => {
    if (timerRef.current) clearTimeout(timerRef.current);
    setToast({ visible: true, message });
    timerRef.current = setTimeout(() => setToast({ visible: false, message: '' }), 2600);
  }, []);

  return { toast, showToast };
}

/* ── Dynamic results table ──────────────────────────── */
function ResultTable({ result, searchQuery }) {
  if (!result) return null;

  const columns = result.columnas || [];
  const rawRows = result.filas || [];

  const filtered = useMemo(() => {
    if (!searchQuery.trim()) return rawRows;
    const q = searchQuery.toLowerCase();
    return rawRows.filter(row =>
      Object.values(row).some(v => String(v).toLowerCase().includes(q))
    );
  }, [rawRows, searchQuery]);

  if (columns.length === 0) {
    return <div className="empty-state">No hay columnas en la respuesta.</div>;
  }

  const colNames = columns.map(c => c.nombre || c.name || String(c));
  const lastIdx = colNames.length - 1;

  const exportCSV = () => {
    const header = colNames.join(',');
    const rows = filtered.map(row => colNames.map(c => `"${row[c] ?? ''}"`).join(',')).join('\n');
    const blob = new Blob([`${header}\n${rows}`], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `reporte_${Date.now()}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return { columns: colNames, filtered, lastIdx, exportCSV };
}

/* ── Main App ───────────────────────────────────────── */
export default function App() {
  const [reports, setReports] = useState([]);
  const [selectedId, setSelectedId] = useState('');
  const [prompt, setPrompt] = useState('');
  const [loadingCatalog, setLoadingCatalog] = useState(true);
  const [loadingExec, setLoadingExec] = useState(false);
  const [error, setError] = useState(null);
  const [result, setResult] = useState(null);
  const [lastPrompt, setLastPrompt] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [copiedSql, setCopiedSql] = useState(false);
  const { toast, showToast } = useToast();

  /* Load catalog */
  useEffect(() => {
    let active = true;
    apiService.getReportes()
      .then(data => {
        if (!active) return;
        setReports(data);
        if (data.length > 0) setSelectedId(String(data[0].id));
      })
      .catch(err => {
        if (!active) return;
        setError({ message: err.message || 'No se pudo cargar el catálogo.' });
      })
      .finally(() => { if (active) setLoadingCatalog(false); });
    return () => { active = false; };
  }, []);

  const selectedReport = useMemo(
    () => reports.find(r => String(r.id) === selectedId) ?? null,
    [reports, selectedId]
  );

  /* Execute report */
  const handleSubmit = async (e) => {
    e?.preventDefault();
    if (!selectedReport) { setError({ message: 'Selecciona un reporte antes de ejecutar.' }); return; }
    const trimmed = prompt.trim();
    if (!trimmed) { setError({ message: 'Ingresa un prompt para generar la consulta.' }); return; }

    setLoadingExec(true);
    setError(null);
    setSearchQuery('');

    try {
      const data = await apiService.executeReporte({ idReporte: selectedReport.id, promptUsuario: trimmed });
      setResult(data);
      setLastPrompt(trimmed);
      showToast('Reporte ejecutado y tabla actualizada');
    } catch (err) {
      const apiError = formatApiError(err);
      setError(apiError || { message: err.message || 'La ejecución no pudo completarse.' });
      setResult(null);
    } finally {
      setLoadingExec(false);
    }
  };

  /* Copy SQL */
  const handleCopySql = useCallback(() => {
    const sql = result?.sqlEjecutado;
    if (!sql) return;
    navigator.clipboard.writeText(sql).then(() => {
      setCopiedSql(true);
      showToast('SQL copiado al portapapeles');
      setTimeout(() => setCopiedSql(false), 2000);
    });
  }, [result, showToast]);

  /* Export CSV */
  const handleExportCsv = useCallback(() => {
    if (!result) return;
    const colNames = (result.columnas || []).map(c => c.nombre || c.name || String(c));
    const rows = (result.filas || []);
    const header = colNames.join(',');
    const body = rows.map(row => colNames.map(c => `"${row[c] ?? ''}"`).join(',')).join('\n');
    const blob = new Blob([`${header}\n${body}`], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${(selectedReport?.nombre || 'reporte').replace(/\s+/g, '-').toLowerCase()}_${Date.now()}.csv`;
    a.click();
    URL.revokeObjectURL(url);
    showToast('Archivo CSV generado y descargado');
  }, [result, selectedReport, showToast]);

  /* Derived */
  const categories = useMemo(
    () => [...new Set(reports.map(r => (r.categoria || 'GENERAL').toUpperCase()))],
    [reports]
  );

  const colNames   = result ? (result.columnas || []).map(c => c.nombre || c.name || String(c)) : [];
  const lastColIdx = colNames.length - 1;
  const rawRows    = result?.filas || [];

  const filteredRows = useMemo(() => {
    if (!searchQuery.trim()) return rawRows;
    const q = searchQuery.toLowerCase();
    return rawRows.filter(row => Object.values(row).some(v => String(v).toLowerCase().includes(q)));
  }, [rawRows, searchQuery]);

  /* Sidebar select */
  const handleSelectReport = (id) => {
    setSelectedId(id);
    setResult(null);
    setError(null);
    setPrompt('');
    setSearchQuery('');
    setSidebarOpen(false);
  };

  /* Quick-prompt suggestions derived from selected report */
  const quickSuggestions = selectedReport?.sugerencias || [
    'Ordenar por mayor venta',
    'Solo clientes EMPRESA',
    'Agrupar por región',
  ];

  return (
    <div className="app-shell">

      {/* ── Toast ── */}
      <div className={`toast ${toast.visible ? 'toast--visible' : ''}`} role="status" aria-live="polite">
        <IconCheck />
        <span>{toast.message}</span>
      </div>

      {/* ── Mobile backdrop ── */}
      <div
        className={`sidebar-backdrop ${sidebarOpen ? 'active' : ''}`}
        onClick={() => setSidebarOpen(false)}
        aria-hidden="true"
      />

      {/* ─────────────────────── SIDEBAR ─────────────────────── */}
      <aside className={`sidebar ${sidebarOpen ? '' : 'sidebar--hidden'}`} aria-label="Catálogo de reportes">

        <div className="sidebar-header">
          <div className="brand-block">
            <div className="brand-icon"><IconSparkles /></div>
            <div className="brand-meta">
              <span className="brand-eyebrow">Motor de Reportes</span>
              <div className="brand-title">
                Reportes IA
                <span className="brand-badge">v2.4</span>
              </div>
            </div>
          </div>
          <button
            className="btn-icon"
            aria-label="Cerrar menú"
            onClick={() => setSidebarOpen(false)}
          >
            <IconClose />
          </button>
        </div>

        <nav className="sidebar-nav">
          {loadingCatalog && (
            <p className="loading-catalog-row">Cargando catálogo…</p>
          )}
          {!loadingCatalog && reports.length === 0 && (
            <p className="loading-catalog-row">No hay reportes activos.</p>
          )}
          {!loadingCatalog && categories.map(cat => (
            <div key={cat} className="category-block">
              <div className="category-label">
                {CATEGORY_ICONS[cat] || null}
                <span>{cat.charAt(0) + cat.slice(1).toLowerCase()}</span>
              </div>
              <div className="report-list">
                {reports
                  .filter(r => (r.categoria || 'GENERAL').toUpperCase() === cat)
                  .map(r => (
                    <button
                      key={r.id}
                      id={`report-btn-${r.id}`}
                      type="button"
                      className={`report-btn ${selectedId === String(r.id) ? 'selected' : ''}`}
                      onClick={() => handleSelectReport(String(r.id))}
                      aria-pressed={selectedId === String(r.id)}
                    >
                      <div className="report-btn-header">
                        <span className="report-btn-name">{r.nombre}</span>
                        <span className="report-btn-chevron"><IconChevron /></span>
                      </div>
                      {r.descripcion && (
                        <p className="report-btn-desc">{r.descripcion}</p>
                      )}
                    </button>
                  ))}
              </div>
            </div>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className="db-status">
            <div className="db-status-dot">
              <span className="db-status-dot-ping" />
              <span className="db-status-dot-core" />
            </div>
            <span>Base de Datos Conectada</span>
          </div>
          <span className="db-label">ORACLE DW</span>
        </div>
      </aside>

      {/* ─────────────────────── MAIN ─────────────────────── */}
      <main className="main-panel" id="main-content">

        {/* Topbar */}
        <header className="topbar">
          <div className="topbar-left">
            <button
              id="btn-open-sidebar"
              className="btn-hamburger"
              aria-label="Abrir menú de reportes"
              onClick={() => setSidebarOpen(true)}
            >
              <IconMenu />
            </button>
            <div className="topbar-title-group">
              <div className="topbar-breadcrumb">
                <span>Reporte Seleccionado</span>
                <span>/</span>
                <span className="crumb-accent">
                  {selectedReport ? (selectedReport.categoria || 'General') : '—'}
                </span>
              </div>
              <h2 className="topbar-title">
                {selectedReport ? selectedReport.nombre : 'Sin reporte seleccionado'}
              </h2>
            </div>
          </div>
          {selectedReport && (
            <span className="topbar-badge">
              {selectedReport.categoria || 'GENERAL'}
            </span>
          )}
        </header>

        {/* Content */}
        <div className="content-body">

          {/* ── Prompt card ── */}
          <section className="card prompt-card" aria-label="Consola de prompt">
            <div className="card-label-row">
              <div className="card-label">
                <span className="card-label-dot" />
                Prompt de negocio
              </div>
              <span className="card-hint">Formula tu consulta analítica en lenguaje natural</span>
            </div>

            <form onSubmit={handleSubmit}>
              <textarea
                id="prompt-textarea"
                className="prompt-textarea"
                rows={3}
                value={prompt}
                onChange={e => setPrompt(e.target.value)}
                placeholder={
                  selectedReport
                    ? `Describe lo que quieres analizar en "${selectedReport.nombre}"…`
                    : 'Selecciona un reporte para comenzar'
                }
                disabled={!selectedReport || loadingExec}
                aria-label="Prompt de negocio"
              />

              <div className="prompt-actions">
                {/* Quick-prompt chips */}
                <div className="quick-prompts">
                  <span className="quick-label">Sugerencias:</span>
                  {quickSuggestions.map((s, i) => (
                    <button
                      key={i}
                      type="button"
                      className="chip-btn"
                      onClick={() => setPrompt(s)}
                      disabled={!selectedReport || loadingExec}
                    >
                      {s}
                    </button>
                  ))}
                </div>

                {/* Execute button */}
                <button
                  id="btn-execute"
                  type="submit"
                  className="btn-primary"
                  disabled={!selectedReport || loadingExec}
                  aria-busy={loadingExec}
                >
                  {loadingExec ? (
                    <>
                      <span className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }} />
                      Ejecutando…
                    </>
                  ) : (
                    <>
                      <IconPlay />
                      Ejecutar reporte
                    </>
                  )}
                </button>
              </div>
            </form>
          </section>

          {/* ── Error banner ── */}
          {error && (
            <div className="error-banner" role="alert">
              <p>{error.message}</p>
              {(error.code || error.status) && (
                <p className="error-meta">
                  {error.code  && <code>{error.code}</code>}
                  {error.status && <span>HTTP {error.status}</span>}
                </p>
              )}
              {error.traceId && (
                <p className="error-trace">Referencia: {error.traceId}</p>
              )}
              {Object.keys(error.diagnostics || {}).length > 0 && (
                <details className="error-details">
                  <summary>Detalles técnicos</summary>
                  <dl>
                    {Object.entries(error.diagnostics).map(([k, v]) => (
                      <div key={k}><dt>{k}</dt><dd>{v}</dd></div>
                    ))}
                  </dl>
                </details>
              )}
            </div>
          )}

          {/* ── Results card ── */}
          <section className="card" aria-label="Resultados de la consulta">

            <div className="results-header">
              <div>
                <div className="results-title-group">
                  <h3 className="results-title">Resultados</h3>
                  {result && (
                    <span id="results-count" className="results-count-badge">
                      {filteredRows.length} registros
                    </span>
                  )}
                </div>

                {result && (
                  <div className="results-meta" style={{ marginTop: 6 }}>
                    <div className="results-meta-item">
                      <span className="results-meta-label">Prompt:</span>
                      <span className="results-meta-value">{lastPrompt}</span>
                    </div>
                    <div className="results-meta-item">
                      <IconClock />
                      <span className="results-meta-label">Tiempo:</span>
                      <span className="results-meta-value time">
                        {result.tiempoEjecucionMs} ms
                      </span>
                    </div>
                  </div>
                )}
              </div>

              {result && (
                <div className="results-actions">
                  <div className="search-wrapper">
                    <span className="search-icon"><IconSearch /></span>
                    <input
                      id="table-search"
                      type="text"
                      className="search-input"
                      placeholder="Filtrar resultados…"
                      value={searchQuery}
                      onChange={e => setSearchQuery(e.target.value)}
                      aria-label="Filtrar resultados"
                    />
                  </div>
                  <button
                    id="btn-export-csv"
                    type="button"
                    className="btn-secondary"
                    onClick={handleExportCsv}
                    title="Descargar datos en formato CSV"
                  >
                    <IconDownload />
                    Exportar CSV
                  </button>
                </div>
              )}
            </div>

            {/* States */}
            {!result && !loadingExec && (
              <div className="empty-state">
                El resultado aparecerá aquí después de ejecutar un reporte.
              </div>
            )}

            {loadingExec && (
              <div className="loading-box">
                <div className="spinner" />
                <span className="loading-text">Consultando modelo y validando SQL…</span>
              </div>
            )}

            {result && colNames.length > 0 && (
              <>
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        {colNames.map((col, idx) => (
                          <th key={col} style={{ textAlign: idx === lastColIdx ? 'right' : 'left' }}>
                            {col}
                          </th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {filteredRows.length === 0 ? (
                        <tr>
                          <td colSpan={colNames.length} style={{ textAlign: 'center', padding: '32px', color: 'var(--text-muted)' }}>
                            No se encontraron coincidencias para &ldquo;{searchQuery}&rdquo;
                          </td>
                        </tr>
                      ) : (
                        filteredRows.map((row, rIdx) => (
                          <tr key={rIdx}>
                            {colNames.map((col, cIdx) => (
                              <td
                                key={col}
                                className={
                                  cIdx === 0 ? 'col-first' :
                                  cIdx === lastColIdx ? 'col-numeric' : ''
                                }
                              >
                                {row[col] ?? ''}
                              </td>
                            ))}
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>

                <div className="table-footer">
                  <span id="pagination-status">
                    Mostrando {filteredRows.length} de {rawRows.length} registros
                  </span>
                  <span className="table-footer-badge">Status: Query OK</span>
                </div>
              </>
            )}
          </section>

          {/* ── SQL Inspector ── */}
          {result?.sqlEjecutado && (
            <section className="card" aria-label="SQL generado">
              <div className="sql-card-header">
                <div className="sql-card-title">
                  <IconCode />
                  SQL ejecutado
                  <span className="sql-lang-badge">ANSI SQL</span>
                </div>
                <button
                  id="btn-copy-sql"
                  type="button"
                  className="btn-secondary"
                  onClick={handleCopySql}
                  aria-label="Copiar SQL al portapapeles"
                >
                  {copiedSql ? <><IconCheck />¡Copiado!</> : <><IconCopy />Copiar SQL</>}
                </button>
              </div>
              <div className="sql-pre-wrap">
                <pre id="sql-code-block" className="sql-pre">{result.sqlEjecutado}</pre>
              </div>
            </section>
          )}

        </div>
      </main>
    </div>
  );
}

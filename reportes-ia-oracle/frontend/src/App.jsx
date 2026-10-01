import { useEffect, useMemo, useState, useRef, useCallback } from 'react';
import { formatApiError } from './errorResponse.js';
import { apiService } from './api.js';
import { AgGridReact } from 'ag-grid-react';
import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-alpine.css';

export default function App() {
  const [reports, setReports] = useState([]);
  const [selectedReportId, setSelectedReportId] = useState('');
  const [prompt, setPrompt] = useState('');
  const [loadingCatalog, setLoadingCatalog] = useState(true);
  const [loadingExecution, setLoadingExecution] = useState(false);
  const [error, setError] = useState(null);
  const [result, setResult] = useState(null);
  const [lastSubmittedPrompt, setLastSubmittedPrompt] = useState('');

  useEffect(() => {
    let active = true;

    apiService.getReportes()
      .then((data) => {
        if (!active) {
          return;
        }

        setReports(data);
        if (data.length > 0) {
          setSelectedReportId(String(data[0].id));
        }
      })
      .catch((fetchError) => {
        if (!active) {
          return;
        }
        setError({ message: fetchError.message || 'No se pudo cargar el catálogo.' });
      })
      .finally(() => {
        if (active) {
          setLoadingCatalog(false);
        }
      });

    return () => {
      active = false;
    };
  }, []);

  const selectedReport = useMemo(
    () => reports.find((report) => String(report.id) === String(selectedReportId)) ?? null,
    [reports, selectedReportId]
  );

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!selectedReport) {
      setError({ message: 'Selecciona un reporte antes de ejecutar la consulta.' });
      return;
    }

    const trimmedPrompt = prompt.trim();
    if (!trimmedPrompt) {
      setError({ message: 'Escribe un prompt para la generación de la consulta.' });
      return;
    }

    setLoadingExecution(true);
    setError(null);

    try {
      const payload = await apiService.executeReporte({
        idReporte: selectedReport.id,
        promptUsuario: trimmedPrompt
      });
      setResult(payload);
      setLastSubmittedPrompt(trimmedPrompt);
    } catch (submitError) {
      const apiError = formatApiError(submitError);
      setError(apiError || { message: submitError.message || 'La ejecución no pudo completarse.' });
      setResult(null);
    } finally {
      setLoadingExecution(false);
    }
  };

  const gridRef = useRef();

  const handleExport = useCallback(() => {
    if (gridRef.current && gridRef.current.api) {
      const fileName = `${(selectedReport?.nombre || 'reporte').replace(/\s+/g, '-').toLowerCase()}.csv`;
      gridRef.current.api.exportDataAsCsv({ fileName });
    } else {
      setError({ message: 'No hay datos para exportar en este momento.' });
    }
  }, [selectedReport]);

  const categories = [...new Set(reports.map((report) => report.categoria || 'General'))];

  return (
    <div className="app-shell">
      <aside className="sidebar panel">
        <div className="brand-block">
          <p className="eyebrow">Motor de reportes</p>
          <h1>Reportes IA</h1>
        </div>

        <div className="sidebar-section">
          <p className="section-label">Catálogo</p>
          {loadingCatalog ? (
            <div className="loading-text">Cargando reportes…</div>
          ) : reports.length === 0 ? (
            <div className="empty-text">No hay reportes activos.</div>
          ) : (
            categories.map((category) => (
              <div key={category} className="category-block">
                <h2>{category}</h2>
                {reports
                  .filter((report) => (report.categoria || 'General') === category)
                  .map((report) => (
                    <button
                      key={report.id}
                      type="button"
                      className={`report-card ${selectedReportId === String(report.id) ? 'selected' : ''}`}
                      onClick={() => setSelectedReportId(String(report.id))}
                    >
                      <strong>{report.nombre}</strong>
                      <span>{report.descripcion}</span>
                    </button>
                  ))}
              </div>
            ))
          )}
        </div>
      </aside>

      <main className="main-panel">
        <section className="panel hero-panel">
          <div>
            <p className="eyebrow">Reporte seleccionado</p>
            <h2>{selectedReport ? selectedReport.nombre : 'Sin reporte activo'}</h2>
          </div>
          {selectedReport && <div className="meta-pill">{selectedReport.categoria}</div>}
        </section>

        <section className="panel form-panel">
          <form onSubmit={handleSubmit} className="prompt-form">
            <label htmlFor="prompt">Prompt de negocio</label>
            <textarea
              id="prompt"
              value={prompt}
              onChange={(event) => setPrompt(event.target.value)}
              placeholder={selectedReport ? `Describe lo que quieres analizar en ${selectedReport.nombre}` : 'Selecciona un reporte para comenzar'}
              rows={6}
              disabled={!selectedReport || loadingExecution}
            />

            <div className="form-actions">
              <button type="submit" className="primary-button" disabled={!selectedReport || loadingExecution}>
                {loadingExecution ? 'Generando consulta…' : 'Ejecutar reporte'}
              </button>
            </div>
          </form>
        </section>

        {error && (
          <div className="banner error-banner" role="alert">
            <p>{error.message}</p>
            {(error.code || error.status) && (
              <p className="error-meta">
                {error.code && <code>{error.code}</code>}
                {error.status && <span>HTTP {error.status}</span>}
              </p>
            )}
            {error.traceId && <p className="error-trace">Referencia: {error.traceId}</p>}
            {Object.keys(error.diagnostics || {}).length > 0 && (
              <details className="error-details">
                <summary>Detalles técnicos</summary>
                <dl>
                  {Object.entries(error.diagnostics).map(([key, value]) => (
                    <div key={key}>
                      <dt>{key}</dt>
                      <dd>{value}</dd>
                    </div>
                  ))}
                </dl>
              </details>
            )}
          </div>
        )}

        <section className="panel results-panel">
          <div className="results-header">
            <h3>Resultados</h3>
            {result && (
              <button type="button" className="secondary-button" onClick={handleExport}>
                Exportar CSV
              </button>
            )}
          </div>

          {!result && !loadingExecution && (
            <div className="empty-state">
              <p>El resultado aparecerá aquí después de ejecutar un reporte.</p>
            </div>
          )}

          {loadingExecution && (
            <div className="loading-box">
              <div className="spinner" />
              <span>Consultando modelo y validando SQL…</span>
            </div>
          )}

          {result && (
            <>
              <div className="result-summary">
                <div>
                  <span className="muted-label">Prompt</span>
                  <p>{lastSubmittedPrompt}</p>
                </div>
                <div>
                  <span className="muted-label">Tiempo de ejecución</span>
                  <p>{result.tiempoEjecucionMs} ms</p>
                </div>
              </div>

              <div className="table-wrap ag-theme-alpine-dark" style={{ height: '400px', width: '100%', borderRadius: '12px' }}>
                <AgGridReact
                  ref={gridRef}
                  rowData={result.filas || []}
                  columnDefs={(result.columnas || []).map(col => ({
                    field: col.nombre || col.name,
                    headerName: col.nombre || col.name,
                    sortable: true,
                    filter: true,
                    resizable: true,
                    flex: 1
                  }))}
                  domLayout='normal'
                />
              </div>

              <div className="sql-panel">
                <h4>SQL ejecutado</h4>
                <pre>{result.sqlEjecutado || 'No hay SQL disponible.'}</pre>
              </div>
            </>
          )}
        </section>
      </main>
    </div>
  );
}

const DIAGNOSTIC_FIELDS = ['proveedor', 'categoria', 'codigoProveedor', 'requestIdProveedor'];

function safeText(value, maxLength = 160) {
  if (typeof value !== 'string' || value.trim() === '') {
    return null;
  }
  return value.trim().slice(0, maxLength);
}

export function formatApiError(payload, fallbackMessage = 'La ejecución fue rechazada por el backend.') {
  const diagnosticPayload = payload?.diagnostico;
  const diagnostics = {};

  if (diagnosticPayload && typeof diagnosticPayload === 'object' && !Array.isArray(diagnosticPayload)) {
    for (const field of DIAGNOSTIC_FIELDS) {
      const value = safeText(diagnosticPayload[field]);
      if (value) {
        diagnostics[field] = value;
      }
    }
  }

  return {
    message: safeText(payload?.mensaje) || safeText(payload?.message) || fallbackMessage,
    code: safeText(payload?.error),
    status: Number.isInteger(payload?.status) ? payload.status : null,
    traceId: safeText(payload?.traceId),
    diagnostics,
  };
}
import assert from 'node:assert/strict';
import test from 'node:test';
import { formatApiError } from './errorResponse.js';

test('prefers mensaje and preserves safe error metadata', () => {
  assert.deepEqual(formatApiError({
    mensaje: 'Proveedor no disponible',
    message: 'Legacy message',
    error: 'AI_UNAVAILABLE',
    status: 503,
    traceId: 'trace-123',
    diagnostico: {
      proveedor: 'Gemini',
      categoria: 'NOT_CONFIGURED',
      codigoProveedor: 'CONFIG_MISSING',
      requestIdProveedor: 'request-456',
    },
  }), {
    message: 'Proveedor no disponible',
    code: 'AI_UNAVAILABLE',
    status: 503,
    traceId: 'trace-123',
    diagnostics: {
      proveedor: 'Gemini',
      categoria: 'NOT_CONFIGURED',
      codigoProveedor: 'CONFIG_MISSING',
      requestIdProveedor: 'request-456',
    },
  });
});

test('handles partial payloads with a safe fallback', () => {
  assert.deepEqual(formatApiError({ error: 'AI_TIMEOUT' }), {
    message: 'La ejecución fue rechazada por el backend.',
    code: 'AI_TIMEOUT',
    status: null,
    traceId: null,
    diagnostics: {},
  });
});

test('discards stack traces, raw provider bodies, and unapproved diagnostic fields', () => {
  const result = formatApiError({
    mensaje: 'Fallo IA',
    stackTrace: 'java.lang.IllegalStateException',
    rawResponse: 'sensitive provider body',
    diagnostico: {
      proveedor: 'Gemini',
      body: 'sensitive provider body',
      stackTrace: 'internal stack',
    },
  });

  assert.deepEqual(result.diagnostics, { proveedor: 'Gemini' });
  assert.equal(JSON.stringify(result).includes('sensitive provider body'), false);
  assert.equal(JSON.stringify(result).includes('internal stack'), false);
});
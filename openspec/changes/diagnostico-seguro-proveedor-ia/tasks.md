# Tasks

## 1. Fallos de generación de IA

- [x] 1.1 Propagar una excepción tipada cuando el proveedor falla, la respuesta no es utilizable o el modelo no está configurado, sin devolver `QUERY_BASE`; verificar con pruebas unitarias que esos casos fallan explícitamente.
- [x] 1.2 Verificar que un fallo de generación detiene el flujo antes de JDBC; cubrir en `DynamicReportServiceTest` que `JdbcTemplate` no se invoque y ejecutar `mvnw.cmd -Dtest=AiSqlGeneratorServiceTest,DynamicReportServiceTest test`.

## 2. Contrato y diagnóstico backend

- [x] 2.1 Añadir correlación por solicitud al MDC, cabecera HTTP y DTO de error, y mapear indisponibilidad, timeout y fallo del proveedor a estados HTTP diferenciados; verificar en pruebas de controller que el código, `mensaje`, `status` y `traceId` coinciden.
- [x] 2.2 Registrar causa y stack técnico en servidor con metadatos permitidos y redacción de secretos, sin incluir prompt completo ni cuerpo crudo del proveedor; verificar que la respuesta HTTP no contiene stack trace ni valores sensibles.
- [x] 2.3 Documentar el contrato de error y los estados de IA en la documentación de API del proyecto; verificar que el ejemplo documentado coincide con el DTO serializado y las pruebas MockMvc.

## 3. Diagnóstico visible en frontend

- [x] 3.1 Interpretar `mensaje` como texto principal, mantener `error` como código y leer `traceId` y metadatos permitidos; extraer un formateador testeable, cubrir payloads completos/parciales con `node:test` y agregar el script `npm test` sin dependencias externas.
- [x] 3.2 Mostrar mensaje y correlación en el banner existente, sin stack trace ni respuesta cruda del proveedor y sin conservar resultados como exitosos tras un error; verificar la interacción en navegador y ejecutar `npm test` y `npm run build`.

## 4. Verificación integrada

- [x] 4.1 Ejecutar la suite backend completa y las pruebas/build frontend; simular proveedor no configurado y fallo upstream, verificando que la API devuelve error correlacionable y no se ejecuta SQL contra Oracle.
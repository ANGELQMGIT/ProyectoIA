# Design

## Context

Véase `proposal.md` y las deltas de `specs/`. Hoy `AiSqlGeneratorService` captura cualquier excepción de la llamada al modelo y devuelve `queryBase`; por ello `DynamicReportService` puede continuar con guardrail y JDBC y responder como una ejecución normal. `GlobalExceptionHandler` ya devuelve un DTO sanitizado para errores y contempla timeout de IA, pero el flujo de generación no propaga el fallo. La SPA consume `message` o `error`, aunque el DTO de backend publica `mensaje`, `error` y `status`.

El alcance afecta tres capas: SPA React/Vite, backend Spring Boot/Spring AI y Oracle JDBC. Oracle y los guardrails no cambian: solo una consulta generada con éxito y aprobada por JSqlParser puede llegar a la base de datos. `oracle-mcp-server` sigue siendo una herramienta de desarrollo para inspeccionar esquema y validar consultas, no un componente de runtime.

## Goals / Non-Goals

**Goals:**
- Propagar fallos de IA como errores HTTP, conservando la causa técnica para diagnóstico en servidor y evitando ejecutar SQL de fallback.
- Correlacionar el error visible en la SPA con la traza del backend mediante un identificador por solicitud.
- Entregar al consumidor un mensaje seguro, un código estable y metadatos de proveedor permitidos, manteniendo el formato actual de error compatible.
- Presentar el mensaje y la correlación en el banner de error existente, sin stack traces ni cuerpos crudos del proveedor.

**Non-Goals:**
- Cambiar el proveedor/modelo de IA, el esquema Oracle, la consulta base de los reportes o los guardrails SQL.
- Enviar prompts, claves, mensajes crudos o stack traces al navegador.
- Agregar una plataforma de observabilidad o modificar el uso de MCP.

## Decisions

### 1. Fallar explícitamente en la frontera de generación

`AiSqlGeneratorService` deja de convertir excepciones y ausencia del modelo en `queryBase`. En su lugar propaga una excepción de dominio que conserva la causa, la categoría del proveedor y los metadatos seguros disponibles. `DynamicReportService` no continúa a validación ni JDBC tras ese error.

El handler mapea categorías conocidas a estados HTTP: configuración/modelo no disponible a `503`, timeout a `504` y fallo/respuesta inválida del proveedor a `502`. Si Spring AI no expone una categoría inequívoca, se usa el error de proveedor genérico, no una clasificación especulativa. Las respuestas exitosas conservan su contrato actual.

**Alternativa considerada:** devolver `QUERY_BASE` ante fallos transitorios. Se descarta porque el resultado puede contradecir filtros solicitados y aparenta éxito.

### 2. Correlación de extremo a extremo

Un filtro HTTP crea un identificador de correlación nuevo por solicitud, lo añade al MDC durante el procesamiento y lo limpia al terminar. El identificador se devuelve en el cuerpo de error como `traceId` y en la cabecera `X-Correlation-Id`. No se acepta un valor arbitrario del cliente sin validación para evitar contaminación de logs.

El DTO mantiene `error`, `mensaje` y `status` y agrega `traceId`; los errores de IA pueden incluir un objeto `diagnostico` opcional con una lista permitida de proveedor, categoría, código de proveedor e ID de solicitud del proveedor. El mensaje técnico libre y el cuerpo original no forman parte del contrato.

**Alternativa considerada:** devolver solo el stack trace. Se descarta porque no correlaciona de forma robusta una petición con logs y expone detalles internos.

### 3. Diagnóstico del servidor con redacción

El handler registra una vez la excepción de IA con su stack/cadena de causas, categoría, proveedor/modelo e identificadores seguros. El pipeline de logs debe redactar credenciales y limitar mensajes o cuerpos de error del proveedor; nunca registra el prompt completo, la clave API ni respuestas completas por defecto. Los logs con detalle quedan restringidos al servidor.

**Alternativa considerada:** mostrar una opción de depuración con stack completo en la SPA. Se descarta incluso en el flujo normal de errores; un diagnóstico de desarrollo no debe cambiar el contrato público ni facilitar exposición accidental.

### 4. Presentación de errores en la SPA

El cliente usa `mensaje` como texto principal, conserva `error` como código y muestra `traceId` cuando exista. Puede presentar los metadatos permitidos en una sección técnica compacta dentro del banner ya existente. No cambia el tema oscuro, la tipografía Inter ni la tabla dinámica. Ante respuesta no exitosa, no muestra el resultado anterior como si perteneciera a la solicitud fallida.

**Alternativa considerada:** presentar `payload.error` como mensaje. Se descarta porque es un identificador de máquina y hoy desplaza el texto legible `mensaje` del backend.

### 5. Validación por capas

Las pruebas del servicio verifican que el fallo no llega a JDBC ni ejecuta `queryBase`; las pruebas HTTP verifican categoría, estado, campos compatibles y ausencia de stack/payload crudo. La verificación de frontend cubre respuesta con `mensaje`, código y `traceId`, además de fallos sin esos campos, mediante el runner integrado `node:test` para no añadir dependencias. Las ejecuciones exitosas continúan comprobando el guardrail antes de Oracle.

## Risks / Trade-offs

- [Riesgo: los reportes dejan de devolver resultados parciales durante una interrupción del proveedor] → Mitigación: devolver un error explícito en vez de información potencialmente incorrecta; el usuario puede reintentar cuando el proveedor esté disponible.
- [Riesgo: mensajes de excepciones contienen credenciales o datos del proveedor] → Mitigación: no exponerlos por API, aplicar redacción en logs y permitir solo metadatos de proveedor por lista blanca.
- [Riesgo: clientes existentes dependen del `200` generado por el fallback] → Mitigación: documentar el cambio semántico; los campos existentes del error se conservan y los nuevos son aditivos.
- [Riesgo: un identificador no validado puede inyectar texto en logs] → Mitigación: generar el identificador en el servidor y usarlo en MDC y respuesta.

## Migration Plan

1. Incorporar propagación tipada de errores, correlación, logging saneado y el campo aditivo `traceId` en el contrato de error.
2. Actualizar la SPA para consumir `mensaje`, código y correlación y verificar errores HTTP reales.
3. Ejecutar pruebas de backend y frontend; probar con el proveedor no configurado, timeout y respuesta fallida, confirmando que no se ejecuta SQL.

No hay migración de datos. Para revertir la presentación frontend se puede volver a la lectura anterior sin alterar la base de datos; no se recomienda restaurar el fallback silencioso del backend.
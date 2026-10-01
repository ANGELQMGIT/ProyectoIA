# Proposal: Fase 3 - Frontend SPA de Reportes

## Why

La base de datos Oracle y el backend Spring Boot ya implementan el catálogo y la generación/ejecución segura de consultas, pero aún falta la capa de experiencia de usuario que permita a un operador interactuar con los reportes sin escribir SQL ni depender de llamadas manuales a la API. Esta fase entrega una SPA moderna que convierta el flujo de reportes en una experiencia guiada, observables y operable en navegador.

## What Changes

- **SPA de reportes con arquitectura React/Vite**: Construcción de la interfaz para listar reportes activos, seleccionar categorías, definir prompts y disparar ejecuciones del flujo text-to-SQL.
- **Pantalla principal de exploración**: Sidebar o panel categorizado de reportes, buscador por nombre/categoría y vista previa del propósito del reporte.
- **Consola de prompt y ejecución**: Entrada de consultas en lenguaje natural, validación visual de estado de carga/error, y feedback inmediato del SQL generado y ejecutado.
- **Grilla dinámica con resultados enriquecidos**: Renderizado tabular de columnas y filas, soporte para datos numéricos, fechas y textos, y marcador de contexto del reporte ejecutado.
- **Exportación de resultados**: Descarga de la grilla actual en CSV y Excel sin perder el contexto del reporte ni la consulta asociada.
- **Inspector SQL y trazabilidad**: Visualización del SQL final aprobado por seguridad, tiempos de ejecución y errores de validación para diagnóstico del usuario y auditoría del sistema.
- **Manejo robusto de errores**: Estados de vacío, fallos de red, errores de validación del backend y mensajes accionables para el usuario final.

## Capabilities

### New Capabilities
- `frontend-spa`: Cubre la experiencia de usuario de la SPA para catálogo, ejecución de reportes, inspección de SQL, administración visual de resultados y exportación de datos.

### Modified Capabilities
- *(Ninguna. La Fase 3 agrega la capa de experiencia de usuario y no redefine los requisitos existentes del backend ni de Oracle DB.)*

## Impact

- **Frontend**: Nueva aplicación SPA dentro del repositorio, con enfoque en experiencia premium, resistencia a errores y accesibilidad básica.
- **Integración con API REST**: Consumo de `GET /api/v1/reportes` y `POST /api/v1/reportes/ejecutar` ya definidos por la Fase 2.
- **UX operativa**: Mejoras en observabilidad de la ejecución del sistema para usuarios no técnicos y analistas de negocio.
- **Persistencia de contexto**: Visualización de metadatos asociados a cada reporte para mantener la trazabilidad del flujo AI + SQL + datos.
- **No-Goles**: La interfaz no permitirá ejecución de sentencias DDL/DML, ni edición directa de SQL por parte del usuario; toda la lógica de seguridad sigue resguardada en el backend.

# Proposal: Inicializar Fase 1 - Base de Datos Oracle y Catálogo Piloto

## Why
El proyecto requiere sentar las bases de datos de dominio y el catálogo de metadatos de reportes para el Motor Dinámico de Reportes Orientado a Prompts (Text-to-SQL). Actualmente no existen la estructura relacional, la vista consolidada ni el catálogo en la base de datos Oracle, impidiendo el desarrollo desacoplado del backend Java 21 y la integración con el modelo Gemini AI.

Esta Fase 1 establece la arquitectura física y lógica en Oracle DB, configurando esquemas con el principio de menor privilegio (`USR_DBTEST` para DDL y `USR_DBTEST_APP` para runtime SELECT), asignación explícita de tablespaces (`TS_DBTEST_DAT` y `TS_DBTEST_IDX`) y la inspección mediante `oracle-mcp-server`.

## What Changes
- **Arquitectura de Base de Datos Oracle**:
  - Configuración de los 2 usuarios desacoplados: Owner DDL `USR_DBTEST` y Usuario Runtime `USR_DBTEST_APP` con permisos estrictos `GRANT SELECT` y sinónimos privados.
  - Asignación obligatoria de Tablespaces: Tablas en `TS_DBTEST_DAT`, Llaves Primarias `PK_<TABLA>` e Índices en `TS_DBTEST_IDX`.
  - Documentación DDL obligatoria con comentarios (`COMMENT ON TABLE` y `COMMENT ON COLUMN`).
- **Tablas de Dominio**:
  - `CLIENTES`, `ITEMS`, `CANALES_VENTA`, `VENTAS`, `DETALLE_VENTA` con constraints de PK `PK_<TABLA>` e identidades autogeneradas.
- **Vista Consolidada**:
  - `V_VENTAS_DETALLADAS` integrando los JOINs de ventas completadas para optimizar las consultas generadas por el backend y Gemini AI.
- **Catálogo de Reportes Piloto**:
  - Tabla `CATALOGO_REPORTES` con los 5 primeros reportes piloto (prompts del sistema y queries base).
- **Integración Tooling MCP**:
  - Verificación de metadatos y prueba de consultas con `oracle-mcp-server`.

## Capabilities

### New Capabilities
- `oracle-reportes-db`: Define el esquema físico y lógico en Oracle DB (`USR_DBTEST`, `USR_DBTEST_APP`, tablespaces, vista `V_VENTAS_DETALLADAS` y tabla `CATALOGO_REPORTES`).

### Modified Capabilities
*(Ninguna modificación a capacidades existentes ya que se trata de la fase inicial del proyecto).*

## Impact
- **Base de Datos**: Creación de tablas, índices, vista y usuarios en Oracle DB `XEPDB1`.
- **Backend**: El backend Spring Boot se conectará exclusivamente con las credenciales de `USR_DBTEST_APP` sobre `V_VENTAS_DETALLADAS` y `CATALOGO_REPORTES`.
- **Seguridad**: Prevención a nivel de motor DB contra sentencias DDL/DML para el usuario runtime del aplicativo.

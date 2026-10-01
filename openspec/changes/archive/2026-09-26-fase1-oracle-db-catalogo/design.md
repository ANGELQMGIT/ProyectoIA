# Design: Fase 1 - Base de Datos Oracle y Catálogo Piloto

## Context
Véase `proposal.md` y la especificación general del proyecto. La Fase 1 se concentra en la construcción física y lógica de la base de datos Oracle DB, configurando usuarios desacoplados, la vista de lectura consolidada `V_VENTAS_DETALLADAS` y la tabla `CATALOGO_REPORTES`.

## Goals / Non-Goals

**Goals:**
- Implementar los scripts DDL en Oracle DB para la creación de Tablespaces (`TS_DBTEST_DAT`, `TS_DBTEST_IDX`), usuarios (`USR_DBTEST`, `USR_DBTEST_APP`) y privilegios (`GRANT SELECT` + Sinónimos).
- Crear las tablas de dominio (`CLIENTES`, `ITEMS`, `CANALES_VENTA`, `VENTAS`, `DETALLE_VENTA`) asignando Tablespace de datos `TS_DBTEST_DAT` e Índices/PKs `PK_<TABLA>` en `TS_DBTEST_IDX`.
- Aplicar las sentencias obligatorias de comentarios DDL (`COMMENT ON TABLE` y `COMMENT ON COLUMN`).
- Crear la vista de lectura unificada `V_VENTAS_DETALLADAS` e insertar datos semilla piloto.
- Crear la tabla `CATALOGO_REPORTES` y poblarla con 5 reportes piloto para pruebas iniciales.
- Verificar la correcta conectividad e inspección mediante `oracle-mcp-server`.

**Non-Goals:**
- Desarrollo del código backend Spring Boot (reservado para la Fase 2).
- Desarrollo del código frontend SPA (reservado para la Fase 3).
- Implementación de Vistas Materializadas `MV_` (evaluado para la Fase 5).

## Decisions

### 1. Desacoplamiento de Seguridad a Nivel de Base de Datos
- **Decisión:** Desacoplar el rol de administración DDL (`USR_DBTEST`) del usuario de runtime de la aplicación (`USR_DBTEST_APP`).
- **Justificación:** Previene cualquier posibilidad de que la aplicación ejecute sentencias destructivas (DDL/DML) en caso de que una consulta SQL maliciosa burlase el backend.
- **Alternativa Considerada:** Usar un único usuario `USR_DBTEST` para DDL y runtime. Rechazado por riesgo de seguridad.

### 2. Estrategia de Tablespaces Explícitos (`TS_DBTEST_DAT` y `TS_DBTEST_IDX`)
- **Decisión:** Forzar la cláusula `TABLESPACE TS_DBTEST_DAT` en la creación de tablas y `USING INDEX TABLESPACE TS_DBTEST_IDX` en la creación de constraints de Primary Key `PK_<TABLA>` e Índices.
- **Justificación:** Optimización de I/O en almacenamiento Oracle y alineación estricta con las mejores prácticas corporativas de DBA.

### 3. Vista Integrada de Lectura `V_VENTAS_DETALLADAS`
- **Decisión:** Crear una vista SQL de lectura unificada sobre ventas completadas (`WHERE v.ESTADO = 'COMPLETADA'`).
- **Justificación:** Facilita la generación de consultas por parte del modelo Gemini AI al exponer nombres de columnas planos y JOINs preconfigurados.

## Risks / Trade-offs

- **[Riesgo: Error ORA-65096 al crear usuarios en Oracle PDB]** → **Mitigación:** Iniciar el script de administración con `ALTER SESSION SET "_ORACLE_SCRIPT"=true;` para entornos local/PDB.
- **[Riesgo: Falta de permisos en el usuario runtime USR_DBTEST_APP]** → **Mitigación:** Otorgar explícitamente `GRANT SELECT ON ... TO USR_DBTEST_APP` y generar sinónimos privados bajo `USR_DBTEST_APP`.

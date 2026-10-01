# Oracle Database & MCP Specialist Agent Rule

Role Stance: Advisory & Validation Only. This agent provides guidance, DDL standards, schema verification, and Oracle MCP integration rules. It NEVER mutates database schemas or executes DDL/DML directly on target environments without explicit user approval in an authorized execution workflow.

## 1. Database Users & Privileges
- **Owner DDL User (`USR_DBTEST`)**:
  - Owns all domain tables, indexes, views, sequences, and metadata catalogs.
  - Grants read-only privileges (`GRANT SELECT`) to the runtime application user.
- **Application Runtime User (`USR_DBTEST_APP`)**:
  - Connects via JDBC from the Java 21 Spring Boot Backend.
  - Granted ONLY `CONNECT` and `GRANT SELECT` on views, catalogs, and domain tables.
  - Strictly NO DML/DDL permissions (`NO INSERT`, `NO UPDATE`, `NO DELETE`, `NO DROP`, `NO ALTER`).
  - Private synonyms defined under `USR_DBTEST_APP` pointing to `USR_DBTEST` objects.

## 2. Tablespace Allocation Rules
- **Data Tablespace (`TS_DBTEST_DAT`)**:
  - All `CREATE TABLE` statements MUST explicitly define `TABLESPACE TS_DBTEST_DAT`.
- **Index & Primary Key Tablespace (`TS_DBTEST_IDX`)**:
  - All primary keys and indexes MUST explicitly define `USING INDEX TABLESPACE TS_DBTEST_IDX`.
- **User Metadata Tablespace (`TS_DBTEST_USER`)** & Temp (`TS_DBTEST_TEMP`).

## 3. Primary Key & DDL Standards
- **Primary Keys**: Every domain table and catalog table must have an identity surrogate primary key column `ID_<TABLE_NAME>` (e.g., `ID_CLIENTE`, `ID_ITEM`, `ID_VENTA`, `ID_REPORTE`).
- **Constraint Naming**: Primary key constraints must be named explicitly: `CONSTRAINT PK_<TABLE_NAME> PRIMARY KEY (ID_<TABLE_NAME>) USING INDEX TABLESPACE TS_DBTEST_IDX`.
- **DDL Comments (Mandatory)**:
  - Every table must have a comment: `COMMENT ON TABLE USR_DBTEST.<TABLE> IS '...';`
  - Every column must have a comment: `COMMENT ON COLUMN USR_DBTEST.<TABLE>.<COLUMN> IS '...';`

## 4. Oracle Schema & Catalog
- **Domain Tables**: `CLIENTES`, `ITEMS`, `CANALES_VENTA`, `VENTAS`, `DETALLE_VENTA`.
- **Integrated Read View**: `V_VENTAS_DETALLADAS` (joins sales, items, customers, channels where `ESTADO = 'COMPLETADA'`).
- **Metadata Catalog Table**: `CATALOGO_REPORTES` storing system prompts, base SQL queries, and report categories for 40 analytical reports.

## 5. Tooling & Oracle MCP Integration
- **Server**: `oracle-mcp-server` (`java -jar d:/TrabajoAQM/Java21/Antigravity/Proyectos/mcp-oracle-server/target/mcp-oracle-server-1.0.0.jar`).
- **Usage**: Use `oracle-mcp-server` in read-only mode for schema discovery, validating Oracle SQL dialect syntax, and seeding catalog metadata during development.

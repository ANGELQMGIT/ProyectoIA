# Tasks: Fase 1 - Base de Datos Oracle y Catálogo Piloto

## 1. Configuración de Infraestructura de Base de Datos y Usuarios

- [x] 1.1 Crear el script SQL de inicialización de Tablespaces (`TS_DBTEST_DAT`, `TS_DBTEST_IDX`, `TS_DBTEST_USER`, `TS_DBTEST_TEMP`) y verificación de ejecución limpia en Oracle DB.
- [x] 1.2 Crear y configurar el usuario owner DDL `USR_DBTEST` y el usuario aplicativo `USR_DBTEST_APP` con permisos estrictos de `GRANT SELECT` y sinónimos privados.

## 2. Creación de Tablas de Dominio y Documentación DDL

- [x] 2.1 Crear el script DDL para las tablas de dominio (`CLIENTES`, `ITEMS`, `CANALES_VENTA`, `VENTAS`, `DETALLE_VENTA`) especificando `TABLESPACE TS_DBTEST_DAT`, llaves primarias `PK_<TABLA>` e Índices en `TS_DBTEST_IDX`.
- [x] 2.2 Agregar las sentencias obligatorias de comentarios DDL (`COMMENT ON TABLE` y `COMMENT ON COLUMN`) para todas las tablas y campos del dominio y verificar su registro en el diccionario de datos de Oracle.

## 3. Vista Consolidada V_VENTAS_DETALLADAS y Datos Semilla

- [x] 3.1 Crear la vista `V_VENTAS_DETALLADAS` integrando los JOINs de ventas completadas (`ESTADO = 'COMPLETADA'`) y otorgar permiso `GRANT SELECT` a `USR_DBTEST_APP`.
- [x] 3.2 Insertar datos semilla de prueba relacionales en las tablas de dominio y verificar que la consulta a `V_VENTAS_DETALLADAS` retorne las filas consolidadas esperadas.

## 4. Tabla CATALOGO_REPORTES y Registro de Reportes Piloto

- [x] 4.1 Crear el DDL de la tabla `CATALOGO_REPORTES` en `TS_DBTEST_DAT` con constraint `PK_CATALOGO_REPORTES` en `TS_DBTEST_IDX` y comentarios DDL.
- [x] 4.2 Insertar la metadata inicial de los 5 reportes piloto con sus respectivos Prompts de Sistema y Queries Base.

## 5. Inspección y Validación de Conectividad con oracle-mcp-server

- [x] 5.1 Ejecutar la prueba de inspección de esquema y validación de consultas de lectura sobre `V_VENTAS_DETALLADAS` y `CATALOGO_REPORTES` usando `oracle-mcp-server` en modo read-only.

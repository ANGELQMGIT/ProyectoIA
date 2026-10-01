/* ==============================================================================
 * SCRIPT MAESTRO: Execución Completa de la Fase 1
 * Proyecto: Motor Dinámico de Reportes Orientado a Prompts (Text-to-SQL)
 * ============================================================================== */

PROMPT [INFO] Iniciando ejecucion de la Fase 1: Base de Datos Oracle y Catalogo Piloto...

PROMPT [1/4] Creando Tablespaces y Usuarios (USR_DBTEST y USR_DBTEST_APP)...
@01_setup_tablespaces_y_usuarios.sql

PROMPT [2/4] Creando Tablas de Dominio, Constraints PK/FK e Indices...
@02_tablas_dominio_y_comentarios.sql

PROMPT [3/4] Creando Vista V_VENTAS_DETALLADAS e Insertando Datos Semilla...
@03_vista_y_datos_semilla.sql

PROMPT [4/4] Creando Tabla CATALOGO_REPORTES e Insertando 5 Reportes Piloto...
@04_catalogo_reportes_piloto.sql

PROMPT [INFO] Ejecucion de la Fase 1 completada con exito.

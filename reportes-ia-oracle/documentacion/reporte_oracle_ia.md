# Especificación Técnica: Motor Dinámico de Reportes Orientado a Prompts (Java 21 + Oracle + Gemini AI)

## 1. Resumen Ejecutivo y Alcance
El objetivo principal de este proyecto es migrar un esquema tradicional de **40 reportes estáticos** hacia una plataforma analítica dinámica e interactiva. 

El sistema permitirá a los usuarios seleccionar un reporte categorizado y, mediante lenguaje natural (Prompts), solicitar vistas personalizadas de los datos. El backend en **Java 21** combinará el prompt del usuario con las reglas de negocio guardadas en la base de datos **Oracle**, invocará al modelo **Gemini AI** a través de **Spring AI**, ejecutará la consulta `SELECT` generada de forma segura previa validación por **JSqlParser** y devolverá los resultados al **Frontend SPA** para su renderizado dinámico.

---

## 2. Arquitectura General del Sistema
┌──────────────────────────────────────────────────────────────────────────┐
│                           FRONTEND (SPA)                                 │
│    (Dark Mode + Glassmorphism + Prompt Console + Dynamic Grid + Export) │
└────────────────────────────────────┬─────────────────────────────────────┘
                                     │ HTTP POST /api/v1/reportes/ejecutar
                                     ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                   BACKEND (Java 21 / Spring Boot 3.3+)                   │
│                                                                          │
│  1. Recupera Metadatos de Reporte ────────┐                              │
│  2. Construye Prompt Contextual          │                              │
│  3. Invoca a Gemini (Spring AI) ─────────┼──────► [ Google Gemini AI ]  │
│  4. Recibe & Limpia SQL (SELECT) ◄───────┘                              │
│  5. Valida AST SQL (JSqlParser)          │                              │
│  6. Ejecuta SQL mediante JDBC            │                              │
└────────────────────────────────────┬─────┴───────────────────────────────┘
                                     │ SQL query (SELECT ...)
                                     │ (Conexión Usr: USR_DBTEST_APP)
                                     ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                       BASE DE DATOS (Oracle DB)                          │
│   (Owner DDL: USR_DBTEST | Tablespaces: TS_DBTEST_DAT / TS_DBTEST_IDX)   │
│   (Tablas Dominio + Vista V_VENTAS_DETALLADAS + CATALOGO_REPORTES)       │
└────────────────────────────────────▲─────────────────────────────────────┘
                                     │ Metadatos / Inspección Read-only
┌────────────────────────────────────┴─────────────────────────────────────┐
│                         oracle-mcp-server                                │
│             (Protocolo MCP para Agente / Herramientas IA)            │
└──────────────────────────────────────────────────────────────────────────┘

---

## 3. Especificación de Base de Datos Oracle & Seguridad

### 3.1. Estrategia de Desacoplamiento de Usuarios y Permisos
* **Usuario Owner DDL (`USR_DBTEST`):**
  * Es el propietario de todas las tablas de dominio, secuencias, vistas e índices del catálogo.
  * Todas las tablas de datos se asignan explícitamente a `TABLESPACE TS_DBTEST_DAT`.
  * Todas las llaves primarias (`PK_<TABLA>`) e índices se asignan a `TABLESPACE TS_DBTEST_IDX`.
  * Tablas y columnas deben incluir comentarios DDL obligatorios (`COMMENT ON ... IS '...'`).
* **Usuario Aplicativo Runtime (`USR_DBTEST_APP`):**
  * Usuario exclusivo para la conexión JDBC del aplicativo backend Java 21.
  * Privilegios estrictos de solo lectura: `GRANT CONNECT`, `GRANT SELECT` sobre `V_VENTAS_DETALLADAS`, `CATALOGO_REPORTES` y tablas de dominio.
  * Sinónimos privados (`CREATE SYNONYM USR_DBTEST_APP.<OBJETO> FOR USR_DBTEST.<OBJETO>`).
  * Sin permisos DDL ni DML (`NO INSERT`, `NO UPDATE`, `NO DELETE`, `NO DROP`, `NO ALTER`).

### 3.2. Servidor MCP de Oracle (`oracle-mcp-server`)
* Ejecutable JAR: `d:/TrabajoAQM/Java21/Antigravity/Proyectos/mcp-oracle-server/target/mcp-oracle-server-1.0.0.jar`.
* Permite la inspección en tiempo de desarrollo de esquemas Oracle, validación sintáctica de queries y asistencia en el sembrado del catálogo de 40 reportes.

### 3.3. Script SQL de Dominio, Catálogo y Permisos
```sql
-- 1. Script de Tablespaces y Usuarios
ALTER SESSION SET "_ORACLE_SCRIPT"=true;

CREATE SMALLFILE TABLESPACE TS_DBTEST_USER DATAFILE '/opt/oracle/oradata/TSDBTEST_USER.DBF' SIZE 10M AUTOEXTEND ON NEXT 1M;
CREATE TEMPORARY TABLESPACE TS_DBTEST_TEMP TEMPFILE '/opt/oracle/oradata/TSDBTEST_TEMP.DBF' SIZE 5M AUTOEXTEND ON NEXT 1M;
CREATE SMALLFILE TABLESPACE TS_DBTEST_DAT DATAFILE '/opt/oracle/oradata/TSDBTEST_DATA.DBF' SIZE 300M AUTOEXTEND ON NEXT 1M;
CREATE SMALLFILE TABLESPACE TS_DBTEST_IDX DATAFILE '/opt/oracle/oradata/TSDBTEST_INDX.DBF' SIZE 300M AUTOEXTEND ON NEXT 1M;

-- Owner USR_DBTEST
CREATE USER USR_DBTEST IDENTIFIED BY &USR_DBTEST_PASSWORD DEFAULT TABLESPACE TS_DBTEST_USER TEMPORARY TABLESPACE TS_DBTEST_TEMP ACCOUNT UNLOCK;
GRANT DBA TO USR_DBTEST WITH ADMIN OPTION;

-- App User USR_DBTEST_APP
CREATE USER USR_DBTEST_APP IDENTIFIED BY &USR_DBTEST_APP_PASSWORD DEFAULT TABLESPACE TS_DBTEST_USER TEMPORARY TABLESPACE TS_DBTEST_TEMP ACCOUNT UNLOCK;
GRANT CONNECT TO USR_DBTEST_APP;

-- 2. Tablas de Dominio (Owner USR_DBTEST)
CREATE TABLE USR_DBTEST.CLIENTES (
    ID_CLIENTE       NUMBER GENERATED ALWAYS AS IDENTITY,
    NOMBRE           VARCHAR2(100) NOT NULL,
    TIPO_CLIENTE     VARCHAR2(20) CHECK (TIPO_CLIENTE IN ('INDIVIDUAL', 'EMPRESA')),
    REGION           VARCHAR2(50) NOT NULL,
    PAIS             VARCHAR2(50) DEFAULT 'Argentina',
    FECHA_ALTA       DATE DEFAULT SYSDATE,
    CONSTRAINT PK_CLIENTES PRIMARY KEY (ID_CLIENTE) USING INDEX TABLESPACE TS_DBTEST_IDX
) TABLESPACE TS_DBTEST_DAT;

COMMENT ON TABLE USR_DBTEST.CLIENTES IS 'Catálogo de clientes de la empresa';
COMMENT ON COLUMN USR_DBTEST.CLIENTES.ID_CLIENTE IS 'Identificador único del cliente (PK)';

CREATE TABLE USR_DBTEST.ITEMS (
    ID_ITEM          NUMBER GENERATED ALWAYS AS IDENTITY,
    CODIGO           VARCHAR2(20) NOT NULL,
    NOMBRE           VARCHAR2(100) NOT NULL,
    TIPO_ITEM        VARCHAR2(20) CHECK (TIPO_ITEM IN ('PRODUCTO', 'SERVICIO')),
    CATEGORIA        VARCHAR2(50) NOT NULL,
    PRECIO_LISTA     NUMBER(12,2) NOT NULL,
    CONSTRAINT PK_ITEMS PRIMARY KEY (ID_ITEM) USING INDEX TABLESPACE TS_DBTEST_IDX
) TABLESPACE TS_DBTEST_DAT;

CREATE TABLE USR_DBTEST.CANALES_VENTA (
    ID_CANAL         NUMBER GENERATED ALWAYS AS IDENTITY,
    NOMBRE_CANAL     VARCHAR2(50) NOT NULL,
    CONSTRAINT PK_CANALES_VENTA PRIMARY KEY (ID_CANAL) USING INDEX TABLESPACE TS_DBTEST_IDX
) TABLESPACE TS_DBTEST_DAT;

CREATE TABLE USR_DBTEST.VENTAS (
    ID_VENTA         NUMBER GENERATED ALWAYS AS IDENTITY,
    NUMERO_FACTURA   VARCHAR2(30) NOT NULL,
    ID_CLIENTE       NUMBER NOT NULL,
    ID_CANAL         NUMBER NOT NULL,
    FECHA_VENTA      DATE DEFAULT SYSDATE NOT NULL,
    ESTADO           VARCHAR2(20) DEFAULT 'COMPLETADA' CHECK (ESTADO IN ('COMPLETADA', 'ANULADA', 'PENDIENTE')),
    CONSTRAINT PK_VENTAS PRIMARY KEY (ID_VENTA) USING INDEX TABLESPACE TS_DBTEST_IDX,
    CONSTRAINT FK_VENTAS_CLIENTE FOREIGN KEY (ID_CLIENTE) REFERENCES USR_DBTEST.CLIENTES(ID_CLIENTE),
    CONSTRAINT FK_VENTAS_CANAL FOREIGN KEY (ID_CANAL) REFERENCES USR_DBTEST.CANALES_VENTA(ID_CANAL)
) TABLESPACE TS_DBTEST_DAT;

CREATE TABLE USR_DBTEST.DETALLE_VENTA (
    ID_DETALLE       NUMBER GENERATED ALWAYS AS IDENTITY,
    ID_VENTA         NUMBER NOT NULL,
    ID_ITEM          NUMBER NOT NULL,
    CANTIDAD         NUMBER(8,2) NOT NULL,
    PRECIO_UNITARIO  NUMBER(12,2) NOT NULL,
    SUBTOTAL         NUMBER(12,2) NOT NULL,
    CONSTRAINT PK_DETALLE_VENTA PRIMARY KEY (ID_DETALLE) USING INDEX TABLESPACE TS_DBTEST_IDX,
    CONSTRAINT FK_DETALLE_VENTA FOREIGN KEY (ID_VENTA) REFERENCES USR_DBTEST.VENTAS(ID_VENTA),
    CONSTRAINT FK_DETALLE_ITEM FOREIGN KEY (ID_ITEM) REFERENCES USR_DBTEST.ITEMS(ID_ITEM)
) TABLESPACE TS_DBTEST_DAT;

-- 3. Vista Integrada de Lectura Directa
CREATE OR REPLACE VIEW USR_DBTEST.V_VENTAS_DETALLADAS AS
SELECT 
    v.ID_VENTA,
    v.NUMERO_FACTURA,
    v.FECHA_VENTA,
    TO_CHAR(v.FECHA_VENTA, 'YYYY-MM') AS ANIO_MES,
    c.NOMBRE AS NOMBRE_CLIENTE,
    c.TIPO_CLIENTE,
    c.REGION,
    i.NOMBRE AS NOMBRE_ITEM,
    i.TIPO_ITEM,
    i.CATEGORIA AS CATEGORIA_ITEM,
    cv.NOMBRE_CANAL,
    dv.CANTIDAD,
    dv.PRECIO_UNITARIO,
    dv.SUBTOTAL
FROM USR_DBTEST.VENTAS v
JOIN USR_DBTEST.CLIENTES c ON v.ID_CLIENTE = c.ID_CLIENTE
JOIN USR_DBTEST.CANALES_VENTA cv ON v.ID_CANAL = cv.ID_CANAL
JOIN USR_DBTEST.DETALLE_VENTA dv ON v.ID_VENTA = dv.ID_VENTA
JOIN USR_DBTEST.ITEMS i ON dv.ID_ITEM = i.ID_ITEM
WHERE v.ESTADO = 'COMPLETADA';

-- 4. Tabla de Metadatos de Reportes (Catálogo de 40 Reportes)
CREATE TABLE USR_DBTEST.CATALOGO_REPORTES (
    ID_REPORTE          NUMBER GENERATED ALWAYS AS IDENTITY,
    CATEGORIA           VARCHAR2(100) NOT NULL,
    NOMBRE_REPORTE      VARCHAR2(150) NOT NULL,
    DESCRIPCION         VARCHAR2(500),
    PROMPT_SISTEMA      CLOB NOT NULL,
    QUERY_BASE          CLOB NOT NULL,
    ESTADO              VARCHAR2(20) DEFAULT 'ACTIVO',
    CONSTRAINT PK_CATALOGO_REPORTES PRIMARY KEY (ID_REPORTE) USING INDEX TABLESPACE TS_DBTEST_IDX
) TABLESPACE TS_DBTEST_DAT;

-- 5. Privilegios SELECT y Sinónimos para USR_DBTEST_APP
GRANT SELECT ON USR_DBTEST.V_VENTAS_DETALLADAS TO USR_DBTEST_APP;
GRANT SELECT ON USR_DBTEST.CATALOGO_REPORTES TO USR_DBTEST_APP;
GRANT SELECT ON USR_DBTEST.CLIENTES TO USR_DBTEST_APP;
GRANT SELECT ON USR_DBTEST.ITEMS TO USR_DBTEST_APP;
GRANT SELECT ON USR_DBTEST.CANALES_VENTA TO USR_DBTEST_APP;
GRANT SELECT ON USR_DBTEST.VENTAS TO USR_DBTEST_APP;
GRANT SELECT ON USR_DBTEST.DETALLE_VENTA TO USR_DBTEST_APP;

CREATE OR REPLACE SYNONYM USR_DBTEST_APP.V_VENTAS_DETALLADAS FOR USR_DBTEST.V_VENTAS_DETALLADAS;
CREATE OR REPLACE SYNONYM USR_DBTEST_APP.CATALOGO_REPORTES FOR USR_DBTEST.CATALOGO_REPORTES;
```

---

## 4. Especificación Backend (Java 21 + Spring AI + Guardrails)

### 4.1. Características Clave
* **JDK Versión:** Java 21 (Virtual Threads habilitados: `spring.threads.virtual.enabled: true`, DTOs con `records`, `Text Blocks` y `Pattern Matching`).
* **Framework:** Spring Boot 3.3+
* **Módulo AI:** Spring AI (`spring-ai-vertex-ai-gemini-spring-boot-starter` / `spring-ai-google-genai`).
* **Seguridad BD:** Conexión mediante `USR_DBTEST_APP` (acceso exclusivo `GRANT SELECT`).

### 4.2. Configuración (`application.yml`)
```yaml
server:
  port: 8080

spring:
  threads:
    virtual:
      enabled: true

  ai:
    google:
      genai:
        api-key: ${GEMINI_API_KEY}
        chat:
          options:
            model: ${GEMINI_MODEL:gemini-2.5-flash}

  datasource:
    url: ${DB_URL:jdbc:oracle:thin:@//localhost:1521/XEPDB1}
    username: ${DB_USER:USR_DBTEST_APP}
    password: ${DB_PASS}
    driver-class-name: oracle.jdbc.OracleDriver
```

  `GEMINI_API_KEY` y `DB_PASS` son obligatorias y deben definirse en el entorno de ejecución o en un gestor de secretos. `GEMINI_MODEL` puede sobreescribirse; el ejemplo usa `gemini-2.5-flash`. No guardar credenciales en archivos versionados, scripts o ejemplos. Las credenciales que estuvieron previamente en el repositorio deben rotarse.

### 4.3. Reglas de Validación de Seguridad SQL (Guardrails AST)
1. **Analizador AST:** Procesamiento de la sintaxis SQL mediante **JSqlParser**.
2. **Permitido:** Exclusivamente sentencias `SELECT` o `WITH`.
3. **Denegado:** Sentencias con `INSERT`, `UPDATE`, `DELETE`, `DROP`, `ALTER`, `TRUNCATE`, `EXEC`, `CREATE`, `GRANT` o comentarios SQL (`--`, `/*`).
4. **Límite Defensivo:** Inyección obligatoria de `FETCH FIRST 500 ROWS ONLY`.

---

## 5. Especificación AI (Google Gemini AI)
* **Modelo:** `gemini-1.5-pro` (Text-to-SQL estructurado) o `gemini-1.5-flash`.
* **Prompt de Sistema:** Inyección de `NOMBRE_REPORTE`, `QUERY_BASE` y metadatos de `PROMPT_SISTEMA`. Respuesta estricta en SQL puro sin bloques Markdown.

---

## 6. Especificación de Contrato API (Frontend / Backend)

### 6.1. Endpoint: Listar Catálogo Categorizado
`GET /api/v1/reportes`

### 6.2. Endpoint: Ejecutar Reporte Dinámico
`POST /api/v1/reportes/ejecutar`

### 6.3. Respuesta de Error

Las respuestas de error incluyen `error` (código estable), `mensaje` (texto seguro para el usuario), `status` (estado HTTP) y `traceId` (identificador para buscar el detalle en los logs del backend). La respuesta incluye además la cabecera `X-Correlation-Id`, cuyo valor coincide con `traceId`.

Ejemplo para un fallo del proveedor:

```json
{
  "error": "AI_PROVIDER_ERROR",
  "mensaje": "No se pudo generar la consulta con el proveedor de IA",
  "status": 502,
  "traceId": "8ac0b8d8-25d0-4f38-95b7-11ad68e9ea43"
}
```

Los errores de IA se clasifican como `AI_UNAVAILABLE` / HTTP 503 cuando el modelo no está configurado, `AI_TIMEOUT` / HTTP 504 ante un timeout, `AI_PROVIDER_ERROR` / HTTP 502 ante un fallo upstream y `AI_INVALID_RESPONSE` / HTTP 502 si la respuesta está vacía o no es válida. Ningún error de generación ejecuta `QUERY_BASE` ni consulta Oracle.

La API nunca incluye stack traces, prompts completos ni el cuerpo crudo del proveedor. Para soporte, se correlaciona `traceId` con los logs restringidos del backend.

---

## 7. Especificación del Frontend SPA
* **Estética Visual:** Dark Mode, Glassmorphism (`backdrop-filter: blur()`), gradientes suaves, tipografías Google Fonts (*Outfit* / *Inter*).
* **Componentes:**
  * Navegación Categorizada (Tabs / Sidebar para los 40 reportes).
  * Consola de Prompt (Entrada IA) con sugerencias dinámicas.
  * Grilla Dinámica (AG-Grid / TanStack Table) con inferencia automática de columnas.
  * Exportador a Excel / CSV.
  * Inspector de Código SQL Plegable.

---

## 8. Plan de Trabajo e Hitos (Roadmap Oficial)

```text
+-----------------------------------------------------------------------------------+
|                        ROADMAP DE IMPLEMENTACION POR FASES                        |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|  [FASE 1] Base de Datos Oracle & Catálogo Piloto                                 |
|   ├── Script DDL USR_DBTEST (TS_DBTEST_DAT / TS_DBTEST_IDX)                      |
|   ├── Script DDL USR_DBTEST_APP (GRANT SELECT + Sinónimos)                        |
|   ├── Vista V_VENTAS_DETALLADAS + Seed Data Piloto                                |
|   └── Verificación con oracle-mcp-server                                          |
|                                 |                                                 |
|                                 v                                                 |
|  [FASE 2] Core Backend Java 21 + Spring AI + Guardrails SQL                       |
|   ├── Setup Spring Boot 3.3+ (Virtual Threads habilitados)                        |
|   ├── Módulo JSqlParser AST Guardrails (Solo SELECT/WITH, FETCH 500)              |
|   ├── Integración Spring AI Gemini (gemini-1.5-pro / flash)                       |
|   └── Endpoints REST GET /reportes y POST /reportes/ejecutar                      |
|                                 |                                                 |
|                                 v                                                 |
|  [FASE 3] Frontend SPA Dinámico & UX Premium                                      |
|   ├── Tema Dark Mode + Glassmorphism + Google Fonts                               |
|   ├── Componentes Tabs Categorizados + Consola Prompt Natural                     |
|   ├── Grilla Dinámica (AG-Grid/TanStack) con Inferencia de Columnas               |
|   └── Exportador CSV/Excel + Visor SQL Inspector                                  |
|                                 |                                                 |
|                                 v                                                 |
|  [FASE 4] Catálogo Completo (40 Reportes) & Suite de Pruebas                      |
|   ├── Poblado total de CATALOGO_REPORTES asistido por MCP                         |
|   ├── Test de Penetración SQL (JUnit 5 + JSqlParser attack tests)                 |
|   └── Integration tests REST & Virtual Threads concurrency                        |
|                                 |                                                 |
|                                 v                                                 |
|  [FASE 5] Optimización Oracle & Preparación Productiva                            |
|   ├── Evaluación Vistas Materializadas (MV_) + DBMS_SCHEDULER                     |
|   └── Logging Audit, Actuator Metrics & Despliegue Final                          |
|                                                                                   |
+-----------------------------------------------------------------------------------+
```

---

## 9. Gobierno de Agentes Especializados (.agents/rules/)
Los siguientes 5 agentes en `.agents/rules/` operan bajo un **rol strictly consultivo y de supervisión**:

1. `oracle-db.md`: Asesor de DDL Oracle, Tablespaces (`TS_DBTEST_DAT`/`IDX`), PKs, Comentarios, `USR_DBTEST_APP` y `oracle-mcp-server`.
2. `backend-expert.md`: Asesor de Java 21, Spring AI Gemini, JSqlParser SQL Guardrails y REST Controllers.
3. `frontend-ux.md`: Asesor de diseño SPA, Glassmorphism, Dark Mode, Google Fonts, AG-Grid/TanStack y Exportador.
4. `testing-expert.md`: Diseñador de pruebas unitarias/integración (JUnit 5, Mockito, SQL attack tests, Vitest).
5. `architecture-expert.md`: Revisor de arquitectura 3 capas, seguridad multinivel y Requisitos No Funcionales (NFRs).
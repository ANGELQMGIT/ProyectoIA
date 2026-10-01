# Arquitectura de Reportes IA: Escalabilidad y Text-to-SQL

Este documento detalla el diseño arquitectónico y el valor de negocio del **Motor Dinámico de Reportes Orientado a Prompts**, demostrando cómo la integración de Inteligencia Artificial (Text-to-SQL) permite escalar de 1 a *N* reportes de forma segura, eficiente y sin necesidad de escribir nuevo código para cada requerimiento.

---

## 1. Visión General y Valor de Negocio

Tradicionalmente, la creación de un nuevo reporte de negocio requiere un ciclo completo de desarrollo:
1. Toma de requerimientos.
2. Desarrollo en el backend (creación de DTOs, repositorios, nuevos endpoints).
3. Desarrollo en el frontend (diseño de nuevas tablas, filtros, exportaciones).
4. QA y despliegue.

**El Paradigma Text-to-SQL**
Con nuestra nueva arquitectura, el "Time-to-Market" de un nuevo reporte pasa de requerir **días o semanas** a completarse en **minutos**. 
Al estar basado en metadatos y en la interpretación del lenguaje natural por parte de Gemini (Google), agregar un reporte al sistema significa **únicamente insertar un registro en la base de datos** (`CATALOGO_REPORTES`). 

* **Para el usuario final:** Capacidad de explorar datos usando lenguaje natural (Ej: *"Muestra las ventas mayores a $1000 agrupadas por región este mes"*).
* **Para el equipo de producto:** Flexibilidad absoluta. Se pueden iterar y publicar nuevos reportes en producción sin necesidad de un pase a producción de código.

---

## 2. Arquitectura Escalable: El Motor Dinámico

El núcleo del sistema es agnóstico a la estructura del reporte. Se basa en una arquitectura de tres capas estandarizadas:

### A. Frontend Dinámico (React / AG-Grid)
El cliente no tiene hardcodeada ninguna columna.
1. Solicita el catálogo activo al backend.
2. Envía la selección del usuario y su prompt en lenguaje natural.
3. Recibe una matriz genérica de datos (`ResultSetMetaData`) y genera dinámicamente las columnas usando **AG-Grid**, habilitando filtrado, ordenamiento y exportación a CSV automáticos para *cualquier* conjunto de datos.

### B. Backend Genérico (Spring Boot 3.3 / Java 21)
En lugar de tener un endpoint por reporte (`/api/ventas`, `/api/usuarios`), existe **un único endpoint ejecutor**: `POST /api/v1/reportes/ejecutar`.
1. Recupera la configuración base del reporte (el `Query Base`).
2. Combina el contexto de negocio con el prompt del usuario utilizando **Spring AI**.
3. Ejecuta el SQL resultante de forma dinámica a través de `JdbcTemplate`.

### C. Catálogo Centralizado (Oracle DB)
Cada reporte es simplemente una fila en la tabla `CATALOGO_REPORTES` que contiene:
- El `Query Base`: La consulta fundacional validada por el equipo de datos.
- El `Prompt de Sistema`: Reglas de negocio específicas para guiar a la IA.

---

## 3. Seguridad Estricta: Guardrails Activos

Ejecutar SQL generado por IA presenta riesgos inherentes. Esta arquitectura mitiga esos riesgos de forma absoluta mediante la intercepción del Árbol de Sintaxis Abstracta (AST) usando **JSqlParser** antes de tocar la base de datos:

1. **Restricción de Operaciones (No DML/DDL):** El motor valida que la instrucción principal sea estrictamente un `SELECT` (o un `WITH` seguido de un `SELECT`). Cualquier intento de inyectar `UPDATE`, `DELETE`, `DROP` o llamadas a procedimientos almacenados es interceptado y bloqueado en memoria.
2. **Paginación Defensiva (Límite Duro):** Para prevenir consultas que saturen la base de datos o provoquen un OutOfMemory, el analizador inyecta o sobrescribe la cláusula límite forzando un `FETCH FIRST 500 ROWS ONLY` en cada consulta.
3. **Bloqueo de Comentarios:** Elimina la posibilidad de inyecciones SQL basadas en el truncamiento de sentencias mediante comentarios (`--`, `/*`).
4. **Validación de Esquema:** Oracle JDBC arrojará error si la IA "alucina" una columna que no existe en las vistas permitidas.

---

## 4. Resiliencia y Alta Disponibilidad

Dado que el sistema depende de un modelo de IA externo (Gemini Flash), está diseñado para manejar fallas transitorias de red o picos de demanda del proveedor:

* **Manejo de Errores 503 (High Demand):** Implementación de **Spring Retry** que reintenta la llamada al LLM de forma transparente con un *backoff* exponencial en caso de sobrecarga temporal del proveedor.
* **Virtual Threads (Project Loom):** Al habilitar Virtual Threads en Java 21, el backend puede sostener miles de conexiones simultáneas esperando la respuesta de Gemini sin agotar el pool de hilos del sistema operativo, maximizando el throughput.

---

## 5. Conclusión: Escalabilidad a N Reportes

Escalar de 10 a 1000 reportes no impacta la base de código. 

**Ejemplo:**
Si mañana el negocio requiere un "Reporte de Eficiencia de Logística", el equipo de base de datos crea una vista (`V_LOGISTICA`), e inserta un registro en `CATALOGO_REPORTES` definiendo `SELECT * FROM V_LOGISTICA` como el *Query Base*. Inmediatamente, la interfaz SPA mostrará la nueva tarjeta, el usuario podrá pedir *"Muestra envíos con más de 2 días de retraso"*, el backend pedirá a Gemini que añada la cláusula `WHERE tiempo_retraso > 2`, JSqlParser validará la seguridad y AG-Grid dibujará la tabla en pantalla.

**Cero líneas de código Java o Javascript modificadas.**

---

## 6. Diagrama de Arquitectura del Sistema

```text
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
```

---

## 7. Roadmap Oficial de Implementación

El siguiente es el roadmap completo del proyecto, estructurado por fases para garantizar una entrega incremental y de alto valor:

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

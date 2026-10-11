# Motor Dinámico de Reportes Orientado a Prompts (Text-to-SQL)

Sistema inteligente para la generación, parametrización y ejecución dinámica de reportes SQL sobre Oracle Database a partir de instrucciones en lenguaje natural (Prompts) impulsado por Google Gemini y Spring AI.

---

## 🏛️ Arquitectura del Sistema

El sistema implementa una arquitectura desacoplada de 3 capas con un pipeline de validación y seguridad estricto para las consultas generadas por IA:

```text
+------------------------+
|      Frontend SPA      |  React + Vite + Sistema de Tokens
|   (Inspector & UI)     |  Tabla dinámica, filtros, exportación CSV/Excel
+-----------+------------+
            |  HTTP / REST (JSON)
            v
+------------------------+       +-------------------------+
|    Backend Service     | ----> |     Google Gemini       |
|  Java 21 + Spring Boot | <---- | (Text-to-SQL Inference) |
|  Virtual Threads       |       +-------------------------+
+-----------+------------+
            |
    [ Guardrails JSqlParser ]
    - Solo SELECT / WITH
    - Forzado FETCH FIRST 500 ROWS ONLY
    - Rechazo estricto DDL / DML / Comentarios
            |
            |  JDBC / Virtual Threads
            v
+------------------------+
|    Oracle Database     |  Esquema de Dominio, CATALOGO_REPORTES,
|      (Relacional)      |  Vista V_VENTAS_DETALLADAS
+------------------------+
```

---

## 🛠️ Stack Tecnológico

### Backend
- **Java 21** con **Virtual Threads** habilitados (`spring.threads.virtual.enabled: true`).
- **Spring Boot 3.3+**: Núcleo del servicio REST y orquestación.
- **Spring AI**: Integración nativa con Google Gemini (1.5 Flash / Pro).
- **JSqlParser**: Motor de validación sintáctica y guardrails de seguridad SQL.
- **Oracle JDBC / JdbcTemplate**: Ejecución optimizada de consultas y extracción de metadata de resultados.

### Frontend
- **React + Vite**: Interfaz de usuario tipo Single Page Application (SPA).
- **Diseño & Estilos**: CSS modular con sistema de tokens de diseño, Dark Mode (`slate-950` / `sky-500`).
- **Tipografía**: Plus Jakarta Sans (UI) y JetBrains Mono (código y SQL).
- **Componentes**: Tabla HTML nativa dinámica, exportador a CSV/Excel, consola de prompts e inspector SQL.

### Base de Datos
- **Oracle Database**: Tablas de dominio transaccional, vista consolidada `V_VENTAS_DETALLADAS` y tabla de metadatos `CATALOGO_REPORTES`.
- **oracle-mcp-server**: Protocolo MCP para inspección de esquemas y validación de consultas.

---

## 🛡️ Seguridad y Guardrails SQL

Para prevenir inyecciones SQL o ejecuciones no autorizadas, cada consulta generada por el LLM atraviesa una pipeline obligatoria antes de llegar a la base de datos:

1. **Restricción de Comandos**: Únicamente se permiten sentencias `SELECT` o `WITH` (CTE).
2. **Rechazo DDL/DML**: Bloqueo total de `INSERT`, `UPDATE`, `DELETE`, `DROP`, `ALTER`, `TRUNCATE`, etc.
3. **Bloqueo de Comentarios**: Se descartan consultas con comentarios (`--` o `/* */`) para evitar bypass de validaciones.
4. **Límite de Registros**: Se inyecta o valida automáticamente la cláusula `FETCH FIRST 500 ROWS ONLY` para proteger el rendimiento y la memoria.
5. **Usuario de Menor Privilegio**: Conexión a la base de datos configurada con rol de sólo lectura (`SELECT`).

---

## 🔌 Endpoints REST Principales

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/v1/reportes` | Obtiene el catálogo de reportes preconfigurados categorizados. |
| `POST` | `/api/v1/reportes/ejecutar` | Recibe el prompt del usuario y reporte base, genera el SQL con IA, lo valida y retorna los datos tabulares. |

---

## 📁 Estructura del Repositorio

```text
ProyectoIA/
├── openspec/                     # Especificaciones y control de cambios guiado (OpenSpec)
├── reportes-ia-oracle/
│   ├── backend/                  # Proyecto Spring Boot (Java 21)
│   │   └── src/main/resources/   # application.yml y configuraciones
│   ├── frontend/                 # Aplicación React + Vite
│   ├── base-datos/               # Scripts DDL, DML y vistas de Oracle
│   └── documentacion/            # Capturas de pantalla y manuales
├── .gitignore                    # Reglas de exclusión de artefactos y archivos sensibles
└── README.md                     # Documentación principal del proyecto
```

---

## 🚀 Puesta en Marcha

### Prerrequisitos
- **Java 21** (JDK 21+)
- **Node.js** (v18+) y `npm`
- **Oracle Database** con esquema configurado
- Clave de API de **Google Gemini** (`GEMINI_API_KEY`)

### 1. Backend (Spring Boot)
1. Navegar a la carpeta del backend:
   ```bash
   cd reportes-ia-oracle/backend
   ```
2. Configurar variables de entorno requeridas:
   ```bash
   export GEMINI_API_KEY="tu-api-key"
   export DB_URL="jdbc:oracle:thin:@//localhost:1521/ORCLDBTEST"
   export DB_USER="USR_DBTEST_APP"
   export DB_PASSWORD="tu-password"
   ```
3. Compilar y ejecutar:
   ```bash
   ./mvnw spring-boot:run
   ```
   *El servicio iniciará en `http://localhost:8080`.*

### 2. Frontend (React + Vite)
1. Navegar a la carpeta del frontend:
   ```bash
   cd reportes-ia-oracle/frontend
   ```
2. Instalar dependencias:
   ```bash
   npm install
   ```
3. Iniciar el servidor de desarrollo:
   ```bash
   npm run dev
   ```
   *La aplicación estará disponible en `http://localhost:5173`.*

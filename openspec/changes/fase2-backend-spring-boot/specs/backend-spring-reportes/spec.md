# Spec Delta: backend-spring-reportes

## Purpose

Define el contrato de comportamiento del servicio backend en Java 21 y Spring Boot 3.3+, encargado de orquestar las consultas Text-to-SQL con Spring AI (Gemini), hacer cumplir los guardrails AST de JSqlParser y exponer los endpoints REST para el consumo dinámico de reportes.

## ADDED Requirements

### Requirement: Exposición de API REST para Catálogo y Ejecución de Reportes
El sistema backend SHALL exponer los endpoints REST `GET /api/v1/reportes` para consultar el catálogo de reportes activos y `POST /api/v1/reportes/ejecutar` para procesar peticiones en lenguaje natural y retornar la grilla de resultados junto al SQL validado.

#### Scenario: Listado del catálogo de reportes
- **WHEN** un cliente HTTP envía una petición `GET` a `/api/v1/reportes`
- **THEN** el sistema responde con HTTP status 200 y una lista JSON de reportes activos estructurados con `id`, `nombre`, `categoria` y `descripcion`

#### Scenario: Petición de ejecución de reporte con prompt válido
- **WHEN** un cliente HTTP envía una petición `POST` a `/api/v1/reportes/ejecutar` conteniendo un JSON con `idReporte` y `promptUsuario`
- **THEN** el sistema retorna HTTP status 200 conteniendo los metadatos de columnas, la lista de filas obtenidas y la consulta `sqlEjecutado`

### Requirement: Orquestación Spring AI y Generación Text-to-SQL con Gemini
El servicio backend SHALL utilizar Spring AI para enviar a Google Gemini un prompt del sistema estructurado que combine los metadatos del reporte, la definición de la vista `V_VENTAS_DETALLADAS` o tabla asociada, la query base y el prompt en lenguaje natural del usuario, recibiendo como respuesta únicamente una sentencia SQL de consulta sin formato Markdown.

#### Scenario: Generación exitosa de consulta SQL desde lenguaje natural
- **WHEN** el backend invoca al modelo Gemini AI con el contexto completo del reporte y la petición del usuario
- **THEN** Gemini retorna una cadena SQL pura sin delimitadores de bloques de código (```sql ... ```) lista para su análisis AST

### Requirement: Guardrails de Seguridad SQL AST con JSqlParser
El sistema backend SHALL validar todas las sentencias SQL generadas por la IA mediante el analizador sintáctico JSqlParser antes de su ejecución en la base de datos, garantizando la seguridad del motor.

#### Scenario: Aceptación de sentencias SELECT o WITH válidas
- **WHEN** JSqlParser analiza una consulta generada que consiste únicamente en una instrucción `SELECT` o `WITH`
- **THEN** la consulta es aprobada y se le inyecta la cláusula de salvaguarda `FETCH FIRST 500 ROWS ONLY` si no incluye un límite explícito

#### Scenario: Rechazo inmediato de sentencias destructivas DDL/DML o comentarios
- **WHEN** la consulta generada contiene sentencias `INSERT`, `UPDATE`, `DELETE`, `DROP`, `ALTER`, `TRUNCATE`, `GRANT`, `CREATE` o comentarios SQL (`--`, `/*`)
- **THEN** el sistema rechaza la ejecución, interrumpe el procesamiento y retorna un error de seguridad HTTP status 400 Bad Request

### Requirement: Ejecución JDBC de Menor Privilegio y Formateo Dinámico
El backend SHALL ejecutar la sentencia SQL aprobada en Oracle DB utilizando exclusivamente el datasource configurado para el usuario de menor privilegio `USR_DBTEST_APP`, transformando el `ResultSet` dinámico en una estructura JSON con metadatos de tipos de datos de columnas y valores de fila.

#### Scenario: Transformación dinámica de ResultSet a JSON
- **WHEN** la consulta SQL validada se ejecuta mediante JDBC bajo el usuario `USR_DBTEST_APP`
- **THEN** los metadatos de las columnas (`ResultSetMetaData`) y el contenido de las filas se mapean dinámicamente a DTOs JSON sin requerir clases de entidad estáticas

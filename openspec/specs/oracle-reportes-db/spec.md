# Capability: oracle-reportes-db

## Purpose

Establece la infraestructura física, los objetos relacionales, el esquema de usuarios de menor privilegio y el catálogo de metadatos en Oracle DB para el sistema dinámico de reportes.

## Requirements

### Requirement: Esquema y Desacoplamiento de Usuarios Oracle DB
El sistema Oracle DB debe contar con dos usuarios desacoplados: USR_DBTEST como propietario DDL de los objetos y USR_DBTEST_APP como usuario de runtime JDBC para la aplicación Java 21 con permisos exclusivos GRANT SELECT y sinónimos privados.

#### Scenario: Creación de usuarios y privilegios de lectura
- **WHEN** se ejecuta el script de inicialización DDL de la Fase 1 en Oracle DB
- **THEN** se crea el usuario USR_DBTEST como administrador del esquema y el usuario USR_DBTEST_APP habilitado únicamente con permisos GRANT CONNECT y GRANT SELECT sobre la vista V_VENTAS_DETALLADAS y la tabla CATALOGO_REPORTES

### Requirement: Asignación Explícita de Tablespaces y Nomenclatura DDL
Todas las tablas creadas en Oracle DB deben ser asignadas al tablespace TS_DBTEST_DAT, y todas las llaves primarias PK_<TABLA> e índices deben asignarse al tablespace TS_DBTEST_IDX. Asimismo, todas las tablas y columnas deben contar con comentarios DDL documentados (COMMENT ON ... IS '...').

#### Scenario: Validación de almacenamiento DDL y documentación
- **WHEN** se crean las tablas de dominio (CLIENTES, ITEMS, CANALES_VENTA, VENTAS, DETALLE_VENTA) y el catálogo (CATALOGO_REPORTES)
- **THEN** las estructuras de tabla se registran en TS_DBTEST_DAT, las llaves primarias e índices en TS_DBTEST_IDX, y se aplican las sentencias COMMENT ON TABLE y COMMENT ON COLUMN correspondientes

### Requirement: Vista Consolidada de Lectura V_VENTAS_DETALLADAS
El esquema debe ofrecer una vista unificada V_VENTAS_DETALLADAS que relacione las ventas en estado COMPLETADA con sus clientes, items y canales de venta, optimizando las consultas SQL de solo lectura ejecutadas por la IA y la aplicación.

#### Scenario: Consulta de datos consolidados mediante la vista
- **WHEN** la aplicación o la IA ejecuta una consulta de lectura sobre V_VENTAS_DETALLADAS
- **THEN** la vista retorna las filas unificadas con columnas precalculadas (ANIO_MES, NOMBRE_CLIENTE, TIPO_ITEM, SUBTOTAL) únicamente para ventas con estado COMPLETADA

### Requirement: Catálogo Piloto de Reportes
La tabla CATALOGO_REPORTES debe almacenar la metadata inicial de los 5 reportes piloto, incluyendo NOMBRE_REPORTE, CATEGORIA, DESCRIPCION, PROMPT_SISTEMA y QUERY_BASE.

#### Scenario: Consulta de reportes del catálogo
- **WHEN** el backend solicita el catálogo de reportes mediante la consulta a CATALOGO_REPORTES
- **THEN** se obtienen las definiciones activas de los reportes piloto ordenados por categoría para ser expuestos por la API REST

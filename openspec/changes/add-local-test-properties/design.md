# Design

## Context

Véase `proposal.md` para la motivación. El backend usa Spring Boot `3.5.15` y Spring AI `1.1.8`; `application.yml` mantiene configuración común y placeholders de entorno. No existe `.gitignore` en el módulo backend, perfil `local-test` ni archivo externo de propiedades.

La solución debe preservar la arquitectura SPA -> Spring Boot/Spring AI -> Oracle JDBC. El frontend, los DTOs REST, el `ChatModel`, el manejo de errores, los guardrails SQL y el esquema Oracle no cambian. `oracle-mcp-server` sigue limitado a inspección/validación durante desarrollo, no participa en el arranque ni consume este archivo.

## Goals / Non-Goals

**Goals:**
- Facilitar pruebas manuales con credenciales rotadas desde un archivo local fuera del classpath y del control de versiones.
- Mantener una plantilla reproducible sin valores funcionales y un mecanismo verificable para ignorar el archivo privado.
- Mantener las pruebas unitarias y CI sin perfil local, valores reales ni llamadas externas.

**Non-Goals:**
- Guardar credenciales reales en `src/main/resources`, `src/test/resources`, `.gitignore`, documentación, comandos versionados o artefactos.
- Activar el perfil `local-test` por defecto o para toda la suite automática.
- Cambiar la integración Google GenAI, los DTOs, los endpoints, las reglas de prompts, el tema oscuro de la SPA, Oracle DDL o los guardrails.

## Decisions

### 1. Ubicación externa `backend/config`

El archivo privado será `reportes-ia-oracle/backend/config/application-local-test.properties`. Spring Boot encuentra el directorio `./config` como configuración externa y carga el archivo específico del perfil cuando `local-test` se activa. El comando documentado se ejecutará desde la raíz `reportes-ia-oracle/backend`; el archivo no se empaqueta en el JAR.

**Alternativa considerada:** poner el archivo bajo `src/main/resources` o `src/test/resources`. Se descarta porque podría incorporarse al classpath, al artefacto distribuible o al repositorio de pruebas.

### 2. Perfil explícito y separado

El perfil se activará solo para pruebas manuales, por ejemplo con `mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local-test`. No se agrega `spring.profiles.active` a `application.yml`. Las pruebas unitarias existentes siguen usando mocks y las tareas CI no habilitan el perfil.

La plantilla versionada será `config/application-local-test.properties.example`, que no es cargada por Spring Boot. El desarrollador la copia al nombre privado, reemplaza placeholders localmente por credenciales rotadas y no comparte su contenido. El `.gitignore` del backend ignora únicamente `config/application-local-test.properties` y cualquier otro patrón necesario para impedir la inclusión del archivo real sin ocultar la plantilla.

**Alternativa considerada:** agregar soporte `.env` mediante parser o dependencia externa. Se descarta porque Spring Boot no carga `.env` automáticamente y el formato `.properties` encaja con el mecanismo de configuración nativo.

### 3. Propiedades y precedencia

El archivo local solo sobrescribe las propiedades de prueba requeridas: key/modelo Google GenAI y conexión Oracle. `application.yml` conserva placeholders sin secretos; ningún valor local se copia a `application.yml` ni a los DTOs. Si el archivo o perfil no existe, una ejecución manual no obtiene esas credenciales del disco local. Los contratos de errores existentes siguen mostrando mensajes seguros y `traceId`, sin valores de configuración.

**Alternativa considerada:** volver a introducir defaults literales en YAML. Se descarta porque la configuración común se usa fuera de pruebas y ya hubo valores sensibles expuestos.

### 4. Verificación segura y arquitectura

Las pruebas automatizadas verifican la selección del perfil y el binding de propiedades con valores sintéticos; no requieren Oracle/Gemini ni llaman a servicios externos. La guía separa el comando de unit tests de un smoke test manual que solo puede correr el desarrollador después de crear el archivo privado. Para auditar el archivo se usa `git check-ignore`; no se imprime ningún secreto.

La pipeline sigue siendo Prompt -> Google GenAI `ChatModel` -> JSqlParser -> `JdbcTemplate` -> JSON. Se conserva Java 21, Boot `3.5.15`, Spring AI `1.1.8`, DTOs inmutables y manejo global de excepciones. JSqlParser permite solo `SELECT`/`WITH`, limita a 500 filas y rechaza DDL/DML; JDBC continúa con usuario de solo lectura. No hay cambio de UI: se mantiene la interfaz oscura e Inter. MCP permanece como herramienta local de inspección de Oracle.

## Risks / Trade-offs

- [Riesgo: Spring Boot no encuentra `config/application-local-test.properties` si se lanza desde otro directorio] → Mitigación: documentar el working directory backend y comprobar el origen de propiedades en el test de contexto.
- [Riesgo: el archivo privado se agrega accidentalmente a Git] → Mitigación: regla exacta en `.gitignore`, verificación con `git check-ignore` y una plantilla `.example` separada.
- [Riesgo: el perfil local se activa en CI o runtime normal] → Mitigación: exigir activación explícita solo en el comando de humo; comprobar que las pruebas por defecto no seleccionan ese perfil.
- [Riesgo: las credenciales previamente expuestas siguen vigentes] → Mitigación: rotarlas antes de crear el archivo local y nunca incorporarlas a los artefactos o logs.

## Migration Plan

1. Crear `.gitignore` de backend y plantilla `.example` sin valores reales.
2. Retirar cualquier fallback secreto restante en la configuración común y agregar pruebas de binding/perfil sin credenciales.
3. Actualizar la documentación con instrucciones para copiar la plantilla a `config/application-local-test.properties`, completar valores localmente y activar `local-test` desde el directorio backend.
4. Ejecutar unit tests/CI sin perfil; verificar que el archivo real está ignorado y la plantilla no.
5. Opcionalmente, después de rotar credenciales, ejecutar el smoke test manual de Gemini y un SELECT de Oracle con el perfil local.

Para rollback, eliminar el archivo local y desactivar `local-test`; las pruebas automáticas siguen sin él. No se restauran defaults de credenciales a `application.yml`.# Design

## Context

Véase `proposal.md` para la motivación. El backend usa Spring Boot `3.5.15`; `application.yml` contiene la configuración común, pero no existe `.gitignore` ni perfil de pruebas local. El YAML actual obtiene la key y la contraseña desde placeholders, y la versión privada de las credenciales no debe formar parte del classpath ni del repositorio.

## Goals / Non-Goals

**Goals:**
- Separar los valores reales para pruebas manuales de Gemini/Oracle del archivo versionado.
- Mantener unit tests y CI ejecutables sin credenciales ni llamadas externas.
- Hacer que la carga del archivo privado sea explícita y limitada al perfil `local-test`.

**Non-Goals:**
- Cambiar configuración de producción, servicio de generación, contrato API, esquema Oracle o permisos.
- Cargar variables `.env` mediante una biblioteca adicional.
- Habilitar el perfil local automáticamente en Maven o en las pruebas normales.

## Decisions

### 1. Archivo externo por perfil

Usar `reportes-ia-oracle/backend/config/application-local-test.properties` para los valores locales reales. Spring Boot lo descubre como configuración externa bajo `./config/` cuando se activa el perfil `local-test`; ejecutar el comando desde la carpeta `backend` mantiene esa ruta estable. El archivo queda fuera de `src/main/resources`, por lo que no se empaqueta en el JAR.

Activar el perfil únicamente en la invocación manual, por ejemplo `mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local-test`. Las pruebas unitarias y CI no activan ese perfil y continúan usando mocks. No agregar `spring.profiles.active` a `application.yml`.

**Alternativa considerada:** importar un archivo `.env` globalmente. Se descarta porque Spring Boot no carga `.env` automáticamente y una importación común puede hacer que credenciales de pruebas se apliquen a ejecuciones no locales.

### 2. Ignorar solo el archivo privado y versionar una plantilla

Crear `reportes-ia-oracle/backend/.gitignore` con una regla específica para `config/application-local-test.properties`. Versionar `config/application-local-test.properties.example`, con las claves Spring necesarias y placeholders explícitos, nunca credenciales funcionales. El usuario copia la plantilla a la ruta privada y sustituye localmente los valores.

La regla debe ser acotada al archivo real; no ignorar todos los `.properties`, de modo que configuración y ejemplos legítimos continúen bajo control de versiones. La tarea verifica `git check-ignore` para el archivo local y que la plantilla siga visible para Git.

### 3. Precedencia y datos de prueba

El perfil local sobrescribe solo las propiedades necesarias para la prueba, como `spring.ai.google.genai.api-key`, `spring.ai.google.genai.chat.options.model`, `spring.datasource.url`, `spring.datasource.username` y `spring.datasource.password`. Los valores de ejemplo no deben contener secretos ni parecer credenciales utilizables. Las credenciales históricas ya expuestas se rotan antes de copiar las nuevas al archivo privado.

**Alternativa considerada:** restaurar defaults de credenciales en `application.yml`. Se descarta porque son compartidos por todos los perfiles y pueden quedar publicados por accidente.

### 4. Verificación aislada

La guía documenta dos caminos separados: suites automáticas (sin perfil ni llamadas externas) y smoke test manual (perfil `local-test` y archivo privado existente). La prueba de configuración automatizada valida rutas/perfil con propiedades sintéticas; el smoke test real es manual y nunca imprime key/password.

La arquitectura SPA -> backend Spring AI -> Oracle permanece intacta. JSqlParser sigue exigiendo `SELECT`/`WITH` y `FETCH FIRST 500 ROWS ONLY`; JDBC conserva privilegios de solo lectura. `oracle-mcp-server` solo se usa como herramienta de desarrollo para esquema y SQL, no para cargar configuración ni ejecutar consultas de aplicación.

## Risks / Trade-offs

- [Riesgo: el comando se ejecuta desde un directorio diferente y Spring Boot no encuentra `./config`] → Mitigación: documentar y probar la ejecución desde `reportes-ia-oracle/backend`.
- [Riesgo: el perfil local se activa inadvertidamente en CI] → Mitigación: no configurarlo globalmente; exigir activación explícita solo en el comando manual.
- [Riesgo: el archivo local se agrega accidentalmente al repositorio] → Mitigación: regla precisa en `.gitignore`, verificación con `git check-ignore` y plantilla separada.
- [Riesgo: credenciales anteriormente versionadas siguen válidas] → Mitigación: rotar key/password antes de usarlas en el archivo local y no repetirlas en documentación.

## Migration Plan

1. Añadir el ignore backend y la plantilla `.example` sin valores reales.
2. Crear localmente `config/application-local-test.properties` copiando la plantilla y completar valores ya rotados.
3. Ejecutar unit tests y CI sin perfil; verificar que no necesitan el archivo privado.
4. Para la verificación manual, arrancar desde `backend` con el perfil `local-test` y comprobar una generación pequeña y un SELECT de solo lectura.

Para rollback, quitar el perfil/archivo local y ejecutar solo pruebas unitarias con mocks. No restaurar secretos a `application.yml` ni agregar el archivo privado al repositorio.
# Proposal

## Why

Las pruebas manuales de integración con Gemini y Oracle necesitan valores de conexión locales, pero mantenerlos como defaults en `application.yml` o en archivos versionados expone credenciales. El backend todavía no cuenta con un `.gitignore` ni un perfil local de prueba para separar esas credenciales del runtime normal.

## What Changes

- Añadir `reportes-ia-oracle/backend/.gitignore` para excluir el archivo privado `config/application-local-test.properties` y otros archivos locales de credenciales.
- Documentar una plantilla `config/application-local-test.properties.example` con nombres de propiedades y valores ficticios/placeholders, nunca credenciales reales.
- Cargar `config/application-local-test.properties` solo con el perfil explícito `local-test`, usando la búsqueda externa estándar de Spring Boot desde el directorio `reportes-ia-oracle/backend`.
- Mantener valores de `GEMINI_API_KEY`, `DB_USER`, `DB_PASS`, `DB_URL` y `GEMINI_MODEL` fuera del control de versiones; eliminar cualquier fallback secreto de la configuración común.
- Documentar cómo crear la copia local privada y ejecutar el backend o smoke test con el perfil `local-test`.
- Mantener las pruebas unitarias/CI aisladas, mockeadas y sin llamadas externas ni dependencia de los valores locales.

## Capabilities

### New Capabilities

None. This is local test tooling and configuration; it does not change externally visible behavior.

### Modified Capabilities

None. `.openspec.yaml` declares `skip_specs: true` because API, SQL and runtime behavior outside the explicitly activated local test profile remain unchanged.

## Impact

- Backend: perfil Spring Boot `local-test`, ubicación externa `backend/config/` y documentación de ejecución local.
- Git: nuevo `.gitignore` acotado al backend y una plantilla `.example` segura que sí puede versionarse.
- Seguridad: credenciales locales permanecen en un archivo ignorado; las credenciales que estuvieron previamente expuestas deben rotarse.
- Arquitectura: SPA -> Backend Java/Spring AI -> Oracle DB, JSqlParser guardrails y `oracle-mcp-server` de desarrollo permanecen sin cambios.

## No Goals

- Versionar, generar automáticamente o solicitar al agente valores reales de API key/password.
- Hacer que los tests unitarios llamen a Gemini o Oracle de forma predeterminada.
- Cambiar las dependencias, el proveedor de IA, los endpoints, el catálogo Oracle o los guardrails SQL.
- Convertir el perfil `local-test` en configuración de producción.
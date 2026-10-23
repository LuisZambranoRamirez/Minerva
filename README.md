# Minerva

[![CI](https://github.com/LuisZambranoRamirez/Minerva/actions/workflows/ci.yml/badge.svg?branch=Diego)](https://github.com/LuisZambranoRamirez/Minerva/actions/workflows/ci.yml)

Backend de distribución construido con Java 25, Spring Boot y PostgreSQL.

## Integración continua

GitHub Actions ejecuta `clean test` en Ubuntu con Java 25 (Temurin) ante cada
push a `Diego` y en todos los pull requests. Las pruebas de integración usan
Testcontainers con el Docker disponible en el runner; el workflow no levanta un
servicio PostgreSQL ni configura credenciales de base de datos o JWT.

## Preparar PostgreSQL

El proyecto usa Flyway para crear y actualizar el esquema. Hibernate conserva
`spring.jpa.hibernate.ddl-auto=none`: las tablas nunca son creadas ni modificadas
por JPA.

### Instalación nueva

Creá una base vacía llamada `minerva` y arrancá la aplicación. Flyway ejecutará
automáticamente los scripts de `src/main/resources/db/migration` y creará su
tabla de historial `flyway_schema_history`.

No ejecutes además `SQL/minerva.sql` ni `SQL/migrations/*.sql`: hacerlo sería
aplicar manualmente cambios que Flyway ya administra.

### Base existente

Hacé un respaldo y arrancá la aplicación una sola vez con la misma base. Si el
esquema contiene tablas pero todavía no posee `flyway_schema_history`, la opción
`baseline-on-migrate` registra la versión `1` como línea base y luego ejecuta
las migraciones `V2` a `V8`.

Este baseline **solo ocurre sobre un esquema no vacío sin historial**. Una base
vacía no se marca como existente: Flyway ejecuta `V1` y crea todas las tablas.
Las migraciones versionadas disponibles actualmente son `V1` a `V8`. `V2` a
`V5` conservan las defensas de idempotencia necesarias para adoptar esquemas
antiguos; `V6` a `V8` incorporan el flujo de pedidos, el autoservicio de clientes
y sus permisos.

El único paso manual para adoptar Flyway es hacer el respaldo y permitir ese
primer arranque. Desde entonces, no vuelvas a aplicar scripts desde `SQL/`:
verificá el historial en `flyway_schema_history`.

> `SQL/minerva.sql` y `SQL/migrations/` quedan como fuentes históricas de los
> scripts incorporados a Flyway, no como un segundo mecanismo de ejecución.

## Variables de entorno

En PowerShell, antes de iniciar la aplicación:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/minerva"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "tu-clave"
$env:JWT_SECRET = "una-clave-base64-segura-de-al-menos-256-bits"
```

También se aceptan `JWT_EXPIRATION_MS`, `PORT`, `SERVER_PORT` y
`JPA_SHOW_SQL`. El puerto prioriza `PORT` (compatible con Railway), después
`SERVER_PORT` y finalmente usa `8080`.

El alta inicial de administrador es opcional. Para usarla, configurá
`MINERVA_BOOTSTRAP_ENABLED=true` junto con todas las variables
`MINERVA_ADMIN_*` declaradas en `application.yaml`.

## Iniciar

Desde la raíz del repositorio:

```powershell
.\minerva\mvnw.cmd -f .\minerva\pom.xml spring-boot:run
```

La aplicación solo está operativa cuando el arranque termina con
`Started MinervaApplication` y no aparecen errores de conexión o validación.

## Despliegue en Railway

- Configurá **Root Directory** como `/minerva`. Railway detectará el
  `Dockerfile`; no agregues overrides de build ni de start.
- Agregá un servicio PostgreSQL privado y definí estas variables en el servicio
  de la aplicación usando referencias de Railway:

  ```text
  DB_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
  DB_USERNAME=${{Postgres.PGUSER}}
  DB_PASSWORD=${{Postgres.PGPASSWORD}}
  JWT_SECRET=<secreto Base64 de al menos 256 bits>
  ```

  Si el servicio PostgreSQL tiene otro nombre, reemplazá `Postgres` en las
  referencias. Guardá `JWT_SECRET` como secreto y no lo incorpores al repositorio.
- Configurá el healthcheck HTTP en `/readyz`. Railway inyecta `PORT`; no hace
  falta definirlo manualmente.
- Para crear el administrador inicial, desplegá primero con una sola réplica,
  `MINERVA_BOOTSTRAP_ENABLED=true` y todas las variables `MINERVA_ADMIN_*`.
  Después del alta, desactivá el bootstrap y volvé a desplegar antes de escalar.

## Observabilidad

- `GET /livez`: liveness público, sin componentes ni detalles internos.
- `GET /readyz`: readiness público; incluye el estado de la base de datos sin
  exponer sus detalles.
- `GET /actuator/prometheus`: métricas Prometheus, requiere JWT con rol `ADMIN`.

Actuator solo publica los endpoints `health` y `prometheus`; cualquier otro,
como `/actuator/env`, permanece sin exponer. Las rutas públicas se limitan a
los probes adicionales `/livez` y `/readyz`.

## Pruebas de integración con PostgreSQL real

La suite de integración levanta el contexto completo de Spring Boot, crea un
contenedor efímero `postgres:17`, aplica Flyway `V1` a `V8` y prueba seguridad
JWT, endpoints HTTP, persistencia, restricciones y concurrencia contra
PostgreSQL real. No usa H2.

### Requisitos

- Java 25 activo en `PATH` y, si lo usás, `JAVA_HOME` apuntando al mismo JDK.
- Docker Desktop abierto en Windows, o un daemon Docker compatible en ejecución.
- Acceso al registro de imágenes para descargar `postgres:17` la primera vez.
- El Maven Wrapper incluido en `minerva/`; no hace falta instalar Maven global.

Verificá el entorno desde la raíz del repositorio:

```powershell
java -version
docker version
docker info
```

La prueba crea el datasource mediante `@ServiceConnection`, con host y puerto
dinámicos. Por eso **no configures** `DB_URL`, `DB_USERNAME` ni `DB_PASSWORD`
para esta suite: esas variables son para ejecutar la aplicación, no las pruebas
de integración. El perfil `integration-test` también deshabilita el bootstrap
del administrador y usa una clave JWT determinista exclusiva de pruebas.

### Ejecución con Maven Wrapper

Suite completa, incluida toda la evidencia de integración:

```powershell
.\minerva\mvnw.cmd -f .\minerva\pom.xml clean test
```

Solo el contexto y las pruebas `*IntegrationTest`:

```powershell
.\minerva\mvnw.cmd -f .\minerva\pom.xml "-Dtest=*IntegrationTest,MinervaApplicationTests" test
```

No agregues `-DskipTests`, no levantes PostgreSQL manualmente y no dependas de
un puerto local fijo. La ejecución enfocada debería descubrir 12 clases
concretas y 27 métodos de integración, contando
`MinervaApplicationTests.contextLoads`.

### Reportes y evidencia para la rúbrica

Surefire escribe los reportes en:

```text
minerva/target/surefire-reports/
```

Para conservar una ejecución completa junto con el entorno y el código de
salida, usá:

```powershell
$evidence = ".\tmp\integration-testing-foundation-batch-8"
New-Item -ItemType Directory -Force $evidence | Out-Null
java -version 2>&1 | Tee-Object "$evidence\java-version.txt"
docker version 2>&1 | Tee-Object "$evidence\docker-version.txt"
& .\minerva\mvnw.cmd -f .\minerva\pom.xml clean test 2>&1 |
  Tee-Object "$evidence\maven-test.txt"
$mavenExitCode = $LASTEXITCODE
"maven_exit_code=$mavenExitCode" | Set-Content "$evidence\maven-exit-code.txt"
Copy-Item .\minerva\target\surefire-reports "$evidence\surefire-reports" -Recurse -Force
```

La evidencia esperada es:

- Java informa versión 25 y Docker muestra cliente **y** servidor accesibles.
- `maven_exit_code=0`, `BUILD SUCCESS` y cero fallos/errores en el resumen.
- Archivos `TEST-*.xml` y `*.txt` de Surefire, incluidos los reportes de
  `FlywayIntegrationTest`, `SecurityHttpIntegrationTest` y las pruebas de
  concurrencia.
- `FlywayIntegrationTest` confirma PostgreSQL 17, las versiones `1` a `8` una
  sola vez y la clasificación completa de tablas y enums.

Registrá además fecha, sistema operativo, salida de Java/Docker, comando exacto,
código de salida, total de pruebas, fallos, errores, omitidas y la ruta de los
reportes. No presentes como evidencia un directorio `surefire-reports` de una
ejecución anterior.

### Solución de problemas

- **Maven usa otro Java:** compará `java -version`, `Get-Command java` y
  `$env:JAVA_HOME`; corregí `JAVA_HOME` y abrí una terminal nueva.
- **Docker no responde:** iniciá Docker Desktop/daemon y repetí `docker info`.
  Un mensaje sobre `dockerDesktopLinuxEngine` o el socket indica que el daemon
  todavía no está disponible.
- **No se puede descargar `postgres:17`:** revisá proxy, VPN, credenciales y
  acceso al registro; podés validar la descarga con `docker pull postgres:17`.
- **Falla Flyway o Hibernate:** leé primero el reporte de
  `FlywayIntegrationTest`; la suite usa únicamente las migraciones productivas
  `V1` a `V8` y `ddl-auto=validate`.
- **Un test queda bloqueado o falla por estado:** confirmá que la ejecución
  paralela siga deshabilitada y revisá el reporte de la clase. El limpiador es
  deliberadamente estricto: una tabla nueva exige clasificarla antes de volver
  a ejecutar la suite.

### Alcance TDD

Estas pruebas documentan comportamiento ya existente, así que son pruebas de
**caracterización y regresión**; no corresponde afirmar que tuvieron un ciclo
RED retroactivo. Para una funcionalidad o corrección nueva, el ciclo sí debe ser
RED → GREEN → REFACTOR: primero una prueba que falle por la razón esperada,
después el cambio mínimo y finalmente la limpieza con toda la evidencia verde.

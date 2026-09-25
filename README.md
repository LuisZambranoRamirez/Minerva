# Minerva

Backend de distribución construido con Java 25, Spring Boot y PostgreSQL.

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
las migraciones `V2` a `V5`.

Este baseline **solo ocurre sobre un esquema no vacío sin historial**. Una base
vacía no se marca como existente: Flyway ejecuta `V1` y crea todas las tablas.
Las migraciones `V2` a `V5` son idempotentes para aceptar tanto bases antiguas
como bases que ya recibieron manualmente alguno de esos cambios.

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

También se aceptan `JWT_EXPIRATION_MS`, `SERVER_PORT` y `JPA_SHOW_SQL`.

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

# Migraciones históricas

Estos archivos documentan las migraciones manuales anteriores a Flyway. Sus
contenidos fueron incorporados, sin cambiar la lógica SQL, en:

```text
minerva/src/main/resources/db/migration/
```

- `001`: agrega la versión para bloqueo optimista de productos.
- `002`: exige cantidades positivas en pérdidas de stock.
- `003`: agrega permisos y validación de cantidades para devoluciones.
- `004`: normaliza `BEBÉS` a `BEBES` en el enum de PostgreSQL.

La correspondencia es:

- `SQL/minerva.sql` → `V1__create_minerva_schema.sql`
- `001` → `V2__add_product_version.sql`
- `002` → `V3__add_stock_loss_quantity_check.sql`
- `003` → `V4__add_product_return_rules.sql`
- `004` → `V5__align_product_category_bebes.sql`

No ejecutes ambas carpetas. La aplicación descubre y ejecuta únicamente los
scripts de `src/main/resources/db/migration` y registra el resultado en
`flyway_schema_history`. Esta carpeta `SQL/migrations` se conserva como
referencia histórica para instalaciones que existían antes de Flyway.

Para adoptar Flyway en una base creada manualmente, hacé primero un respaldo y
arrancá la aplicación. `baseline-on-migrate=true` y `baseline-version=1` marcan
el esquema no vacío en V1 y Flyway aplica V2–V5. En una base vacía no se crea un
baseline: se ejecuta V1 y luego el resto de migraciones.

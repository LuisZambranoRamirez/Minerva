# Catálogo demo con imágenes

El catálogo demo está **apagado por defecto**. Para habilitarlo se requieren las dos condiciones:

1. perfil Spring `demo`;
2. `MINERVA_DEMO_CATALOG_ENABLED=true`.

Flyway V11 solo agrega `product.image_path` nullable; no carga datos. El bootstrap crea seis proveedores ficticios y once productos, y registra el stock inicial mediante `stock_receipt` y `stock_entry`. La reejecución reutiliza proveedores por nombre y omite productos ya coincidentes por SKU; un conflicto de nombre o código de barras aborta la transacción. Nunca sobrescribe cambios operativos.

Las imágenes viven en `src/main/resources/static/assets/products/`: los once nombres definidos por el dataset más `placeholder.webp`. Son assets WebP generados específicamente para este catálogo, sin marcas ni material de terceros; no dependen de una licencia o descarga externa. Los doce archivos son cuadrados de 1024 px y pesan entre 10 KB y 250 KB. La base solo persiste claves `products/<slug>.webp`; la API publica `/assets/<clave>` y no acepta URLs externas.

## Inicio local

Con PostgreSQL escuchando en `127.0.0.1:5432`, ejecutá desde la raíz del repositorio:

```powershell
.\scripts\start-minerva-demo.ps1
```

El script valida Java 25 y PostgreSQL antes de ejecutar Maven, solicita la contraseña sin mostrarla y prepara el perfil y el catálogo demo solo para el proceso actual. Si el JDK está en otra ubicación:

```powershell
.\scripts\start-minerva-demo.ps1 -JavaHome 'C:\ruta\al\jdk-25'
```

También podés cambiar `-DbUrl` y `-DbUsername`. El script no guarda credenciales y genera un `JWT_SECRET` aleatorio en memoria cuando el proceso no tiene uno configurado.

## Despliegue y rollback

Aplicar V11 y desplegar código/assets con el seed apagado. Activarlo exclusivamente en demo. Para rollback funcional, desactivar la property y retirar UI/bootstrap; la columna nullable puede permanecer sin afectar productos legacy.

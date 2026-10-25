# Exportación auditada de movimientos de inventario

## Contrato funcional

- Endpoint: `GET /api/v1/products/inventory-movements/export`.
- Formatos: `xlsx` y `csv`.
- Filtros opcionales combinables: `productId`, `type`, `source`, `from`, `to`.
- Orden: fecha ascendente y UUID del movimiento ascendente.
- Máximo: 5.000 filas; la consulta solicita 5.001 para detectar exceso sin `COUNT`.
- Permiso: `PRODUCT_EXPORT_INVENTORY_REPORT` para ADMIN, VENDEDOR y ALMACENISTA.
- Respuestas: 400 validación, 401 anónimo, 403 sin permiso, 422 exceso y 500 generación.
- Descarga: `Cache-Control: no-store`, MIME específico y filename
  `movimientos-inventario-YYYYMMDD-HHmmss.ext` en `minerva.report.zone`.

## Arquitectura y uso real de las bibliotecas

| Recurso de rúbrica | Evidencia en código | Responsabilidad |
|---|---|---|
| Apache POI | `infrastructure/report/ExcelInventoryReportAdapter.java` | Crea `XSSFWorkbook`, hojas `Movimientos` y `Resumen`, tipos, estilos y totales. |
| Apache Commons CSV | `infrastructure/report/CsvInventoryReportAdapter.java` | Escribe CSV UTF-8 con encabezados y escape de comillas, comas y saltos. |
| Google Guava | `ExcelInventoryReportAdapter.java` (`Table<String,String,BigDecimal>`) | Construye la matriz producto por origen antes de escribir el resumen. |
| SLF4J + Logback | `application/service/InventoryReportService.java` | Registra inicio, éxito y fallo con exportId, actor técnico, formato, filas y duración. |

Las dependencias y licencias están detalladas en
`docs/inventory-report-dependencies.md`. POI, Commons CSV y Guava permanecen en
infraestructura; dominio y aplicación no importan sus tipos.

## Flujo

1. La UI construye filtros y usa `api.download()` con el JWT en `Authorization`.
2. `ProductController` valida y transforma parámetros a `InventoryReportQuery`.
3. `InventoryReportService` autoriza, consulta hasta 5.001 filas y elige un writer por formato.
4. El writer genera todos los bytes. Solo entonces se registra auditoría READ sobre
   `INVENTORY_REPORTS`.
5. La UI valida el filename, crea el Blob, dispara la descarga sin navegar y revoca el object URL.

## Seguridad

- El token nunca se incluye en query params, filename, mensajes ni logs.
- CLIENTE no ve los controles y recibe 403 si intenta acceder al endpoint.
- Se neutralizan campos textuales que empiecen significativamente con `=`, `+`, `-` o `@`.
- Un filename recibido solo se acepta si coincide con el patrón permitido; de lo contrario se usa fallback local.
- Un fallo o exceso no produce archivo parcial ni auditoría exitosa.

## Trazabilidad requisito → prueba → implementación

| Requisito | Prueba principal | Implementación |
|---|---|---|
| REQ-001 | `InventoryReportContractsTest` | puertos y adaptadores separados |
| REQ-002 | `InventoryReportPersistenceIntegrationTest` | proyección JOIN JPA |
| REQ-003–005 | `InventoryReportControllerTest` | `ProductController`, modelos report |
| REQ-006 | `InventoryReportServiceTest`, `ReportRowLimitExceededExceptionHandlerTest` | límite 5.001/422 |
| REQ-007 | tests CSV/XLSX y servicio | writers y auditoría con cero filas |
| REQ-008–013 | tests CSV/XLSX/sanitizador | adaptadores de reporte |
| REQ-014 | controller, integración HTTP y tests de descarga JS | headers, Clock y Blob |
| REQ-015–016 | `InventoryReportSecurityTest`, Flyway/integración HTTP | roles, Permission y V12 |
| REQ-017–019 | `InventoryReportServiceTest` e integración HTTP | auditoría y logging seguro |
| REQ-020 | `inventory-report-download.test.mjs`, `inventory-export-ui.test.mjs` | `api.js`, `download.mjs`, `inventory.js` |

## Evidencia verificada recibida

- Lote 1: 5 pruebas verdes.
- Lote 2: verificaciones rápidas e integración verdes.
- Lote 3: persistencia 3/3, BUILD SUCCESS.
- Lote 4: CSV 2, Excel 2 y sanitizador 2; BUILD SUCCESS.
- Lote 5: 11/11, BUILD SUCCESS, 2026-10-24T22:32:28-05:00.
- Lote 6: rápida 11/11, BUILD SUCCESS, 2026-10-24T22:39:52-05:00; integración 4/4 verde.
- Lote 7: implementación terminada; pruebas aún no ejecutadas al redactar este documento.

## Checklist de evidencia pendiente para la entrega

- [ ] Salida íntegra de la suite Java completa con Java 25.
- [ ] Salida íntegra de la suite JS completa.
- [ ] Captura de filtros y botones Excel/CSV habilitados para un usuario autorizado.
- [ ] Captura de mensaje 422 y confirmación de que no se descargó un archivo.
- [ ] Archivo XLSX abierto mostrando exactamente `Movimientos` y `Resumen`.
- [ ] CSV abierto en UTF-8 con tildes, comillas y saltos preservados.
- [ ] Registro de auditoría READ con formato, filtros y rowCount, sin secretos.
- [ ] Extracto de logs STARTED/SUCCESS/FAILED sin JWT, binario ni contenido de filas.
- [ ] Historial Flyway V1–V12 y permisos de roles verificados en PostgreSQL.

No se declaran capturas ni resultados que todavía no hayan sido producidos por el usuario.

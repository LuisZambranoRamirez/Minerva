# Notas de crédito por devoluciones

`POST /api/v1/sales/{saleId}/credit-notes` emite atómicamente una nota con `lines[]` (`saleDetailId`, `quantity`, `reason`).
El motivo pertenece exclusivamente a cada línea. La cabecera sólo conserva ID, referencia, venta, actor y fecha.

`GET /api/v1/sales/{saleId}/credit-notes` devuelve el historial inmutable. Los contratos de venta agregan
`totalCredited`, `creditBalance`, `status` y `creditNotes` sin retirar campos previos. El endpoint legado de devolución
sigue respondiendo 201 y crea una nota de una línea.

## Semántica financiera

- `amountDue = max(total - totalPaid - totalCredited, 0)`.
- `creditBalance = max(totalPaid + totalCredited - total, 0)` es informativo.
- Emitir crédito no edita pagos históricos, no genera un reembolso y no registra movimientos de caja.
- Los subtotales usan `Money`: escala de dos decimales y redondeo `HALF_UP`.

La emisión y los pagos bloquean pesimistamente la fila de venta. Una cantidad agotada o una carrera responde 409 con
un código/mensaje determinista; una falta de autorización responde 403 sin consultar ni revelar la venta.

## Despliegue

Flyway V9 crea las cabeceras, enlaza cada devolución histórica con una nota `LEGACY-*` y luego exige la FK no nula.
Desplegá primero la migración y después backend/UI. Para rollback deshabilitá emisión y revertí la aplicación; no borres
notas ni vínculos porque son evidencia auditable. No existe rollback financiero, anulación destructiva, caja ni reembolso.

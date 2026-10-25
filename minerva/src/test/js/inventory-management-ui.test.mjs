import test from "node:test";
import assert from "node:assert/strict";
import {readFile} from "node:fs/promises";

const source = await readFile(new URL("../../main/resources/static/js/pages/employee/inventory.js", import.meta.url), "utf8");

test("shows role-gated operational actions with the agreed terminology", () => {
  for (const text of ["Registrar producto", "Registrar entrada", "Registrar pérdida", "Ver movimientos", "Historial", "Movimientos encontrados"]) assert.match(source, new RegExp(text));
  for (const operation of ["product", "entry", "loss"]) assert.match(source, new RegExp(`canManageInventory\\(session, "${operation}"\\)`));
  assert.doesNotMatch(source, /Ledger|Movimientos visibles|Exportar movimientos/);
});

test("shares one server-side filter state between history and export", () => {
  assert.match(source, /createInventoryFilterState/);
  assert.match(source, /filters\.replace\(read\(\)\)/);
  assert.match(source, /buildInventoryExportQuery\(filters\.get\(\), format\)/);
  assert.doesNotMatch(source, /\/movements`/);
});

test("keeps dialog accessibility, loading, errors, refresh and history focus", () => {
  for (const contract of [/aria-labelledby/, /createSubmissionGuard/, /addEventListener\("cancel"/, /fieldErrors\(form/, /await refresh\(\)/, /scrollIntoView/, /focusHistory: true/]) assert.match(source, contract);
});

test("preserves every report export helper", () => {
  for (const helper of ["buildInventoryExportQuery", "canExportInventory", "createExportGuard", "fallbackReportFilename"]) assert.match(source, new RegExp(helper));
});

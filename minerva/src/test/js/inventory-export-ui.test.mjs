import test from "node:test";
import assert from "node:assert/strict";
import {
  buildInventoryExportQuery,
  canExportInventory,
  createExportGuard,
  fallbackReportFilename
} from "../../main/resources/static/js/pages/employee/inventory-export.mjs";

test("shows export only to authorized employee roles and honors explicit permission lists", () => {
  for (const role of ["ADMIN", "VENDEDOR", "ALMACENISTA"]) {
    assert.equal(canExportInventory({role}), true);
  }
  assert.equal(canExportInventory({role: "CLIENTE"}), false);
  assert.equal(canExportInventory({role: "ADMIN", permissions: ["PRODUCT_FIND_ALL"]}), false);
  assert.equal(canExportInventory({role: "ADMIN", permissions: ["PRODUCT_EXPORT_INVENTORY_REPORT"]}), true);
});

test("builds export query with format and only non-empty visible filters", () => {
  assert.deepEqual(buildInventoryExportQuery({
    productId: " 20000000-0000-0000-0000-000000000001 ",
    type: " ENTRADA ",
    source: " STOCK_ENTRY ",
    from: " 2026-10-24T08:00 ",
    to: ""
  }, "xlsx"), {
    format: "xlsx",
    productId: "20000000-0000-0000-0000-000000000001",
    type: "ENTRADA",
    source: "STOCK_ENTRY",
    from: "2026-10-24T08:00"
  });
  assert.equal(fallbackReportFilename("xlsx"), "movimientos-inventario.xlsx");
  assert.equal(fallbackReportFilename("csv"), "movimientos-inventario.csv");
});

test("loading guard disables duplicate exports and always restores state after failure", async () => {
  const changes = [];
  let release;
  const guard = createExportGuard(value => changes.push(value));
  const first = guard.run(() => new Promise((_, reject) => release = reject));
  assert.equal(guard.loading, true);
  assert.equal(await guard.run(async () => "duplicate"), undefined);
  release(new Error("writer failed"));
  await assert.rejects(first, /writer failed/);
  assert.equal(guard.loading, false);
  assert.deepEqual(changes, [true, false]);
});

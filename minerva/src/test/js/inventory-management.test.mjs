import test from "node:test";
import assert from "node:assert/strict";
import {
  buildInventoryLossPayload,
  buildRegisterProductPayload,
  buildStockEntryPayload,
  canManageInventory,
  createInventoryFilterState,
  createSubmissionGuard,
  inventoryManagementErrorMessage
} from "../../main/resources/static/js/pages/employee/inventory-management.mjs";

test("allows inventory writes only to admin and warehouse with the required permission", () => {
  for (const role of ["ADMIN", "ALMACENISTA"]) {
    assert.equal(canManageInventory({role}, "product"), true);
  }
  assert.equal(canManageInventory({role: "VENDEDOR"}, "entry"), false);
  assert.equal(canManageInventory({role: "CLIENTE"}, "loss"), false);
  assert.equal(canManageInventory({role: "ADMIN", permissions: ["PRODUCT_FIND_ALL"]}, "product"), false);
  assert.equal(canManageInventory({role: "ADMIN", permissions: ["PRODUCT_REGISTER"]}, "product"), true);
  assert.equal(canManageInventory({role: "ADMIN"}, "unknown"), false);
});

test("keeps one normalized filter state for history and exports", () => {
  const state = createInventoryFilterState({productId: " p1 ", type: " ENTRADA "});
  assert.deepEqual(state.query(), {productId: "p1", type: "ENTRADA"});
  state.patch({source: " STOCK_ENTRY ", type: ""});
  assert.deepEqual(state.get(), {productId: "p1", type: "", source: "STOCK_ENTRY", from: "", to: ""});
  assert.deepEqual(state.query(), {productId: "p1", source: "STOCK_ENTRY"});
});

test("builds a product payload with normalized values and null image path", () => {
  assert.deepEqual(buildRegisterProductPayload({
    sku: " SKU-1 ", productName: " Arroz ", gainStrategy: " porcentaje ", gainAmount: "20",
    reorderLevel: "0", barCode: " 7750001 ", saleType: " unidad ", productCategory: " otros ",
    purchasedFromSupplierId: " supplier-id ", purchaseUnitPrice: "4.20",
    purchaseQuantity: "10.000", purchaseExpirationDate: "2027-01-01T10:30"
  }), {
    sku: "SKU-1", productName: "Arroz", gainStrategy: "PORCENTAJE", gainAmount: "20",
    reorderLevel: "0", barCode: "7750001", saleType: "UNIDAD", productCategory: "OTROS",
    purchasedFromSupplierId: "supplier-id", purchaseUnitPrice: "4.20", purchaseQuantity: "10.000",
    purchaseExpirationDate: "2027-01-01T10:30", imagePath: null
  });
});

test("enforces integer unit quantities and at most three bulk decimals", () => {
  assert.throws(() => buildStockEntryPayload({supplierId: "s", unitPrice: "1", quantity: "1.5",
    expirationDate: "2027-01-01T10:30"}, "UNIDAD"), /entera/);
  assert.throws(() => buildStockEntryPayload({supplierId: "s", unitPrice: "1", quantity: "1.2345",
    expirationDate: "2027-01-01T10:30"}, "GRANEL"), /3 decimales/);
  assert.equal(buildStockEntryPayload({supplierId: "s", unitPrice: "1", quantity: "1.234",
    expirationDate: "2027-01-01T10:30"}, "GRANEL").quantity, "1.234");
});

test("builds entry and loss payloads and requires supplier date price and reason", () => {
  assert.deepEqual(buildInventoryLossPayload({quantity: "2", reason: " dañado ", observation: " roto "}, "UNIDAD"),
    {quantity: "2", reason: "DAÑADO", observation: "roto"});
  assert.throws(() => buildInventoryLossPayload({quantity: "2", reason: ""}, "UNIDAD"), /motivo/);
  assert.throws(() => buildStockEntryPayload({supplierId: "", unitPrice: "1", quantity: "1",
    expirationDate: "2027-01-01T10:30"}, "UNIDAD"), /proveedor/);
});

test("submission guard blocks duplicate work and restores loading", async () => {
  const changes = [];
  let release;
  const guard = createSubmissionGuard(value => changes.push(value));
  const first = guard.run(() => new Promise(resolve => release = resolve));
  assert.equal(guard.loading, true);
  assert.equal(await guard.run(async () => "duplicate"), undefined);
  release("saved");
  assert.equal(await first, "saved");
  assert.equal(guard.loading, false);
  assert.deepEqual(changes, [true, false]);
});

test("maps field and HTTP errors without exposing implementation details", () => {
  assert.equal(inventoryManagementErrorMessage({fieldErrors: {quantity: "Cantidad inválida."}}), "Cantidad inválida.");
  assert.equal(inventoryManagementErrorMessage({status: 403}), "No tenés permiso para modificar el inventario.");
  assert.equal(inventoryManagementErrorMessage({status: 409}), "Los datos entran en conflicto con un registro existente.");
});

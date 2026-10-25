export const INVENTORY_WRITE_PERMISSIONS = Object.freeze({
  product: "PRODUCT_REGISTER",
  entry: "PRODUCT_REGISTER_STOCK_ENTRY",
  loss: "PRODUCT_REGISTER_INVENTORY_LOSS"
});

const MANAGEMENT_ROLES = new Set(["ADMIN", "ALMACENISTA"]);
const FILTER_KEYS = Object.freeze(["productId", "type", "source", "from", "to"]);

export function canManageInventory(session, operation) {
  if (!session || !MANAGEMENT_ROLES.has(session.role)) return false;
  const permission = INVENTORY_WRITE_PERMISSIONS[operation];
  if (!permission) return false;
  return !Array.isArray(session.permissions) || session.permissions.includes(permission);
}

export function normalizeInventoryFilters(filters = {}) {
  return Object.fromEntries(FILTER_KEYS.map(key => [key, text(filters[key])]));
}

export function createInventoryFilterState(initial = {}) {
  let value = normalizeInventoryFilters(initial);
  return Object.freeze({
    get: () => ({...value}),
    replace(next) { value = normalizeInventoryFilters(next); return this.get(); },
    patch(next) { value = normalizeInventoryFilters({...value, ...next}); return this.get(); },
    query() { return Object.fromEntries(Object.entries(value).filter(([, item]) => item)); }
  });
}

export function buildRegisterProductPayload(input) {
  const saleType = enumValue(input.saleType, "tipo de venta");
  return {
    sku: required(input.sku, "SKU"),
    productName: required(input.productName, "producto"),
    gainStrategy: enumValue(input.gainStrategy, "estrategia de ganancia"),
    gainAmount: positive(input.gainAmount, "ganancia"),
    reorderLevel: optionalNonNegative(input.reorderLevel, "nivel de reposición"),
    barCode: optional(input.barCode),
    saleType,
    productCategory: enumValue(input.productCategory, "categoría"),
    purchasedFromSupplierId: required(input.purchasedFromSupplierId, "proveedor"),
    purchaseUnitPrice: positive(input.purchaseUnitPrice, "precio de compra"),
    purchaseQuantity: quantity(input.purchaseQuantity, saleType),
    purchaseExpirationDate: localDateTime(input.purchaseExpirationDate, "vencimiento"),
    imagePath: null
  };
}

export function buildStockEntryPayload(input, saleType) {
  return {
    supplierId: required(input.supplierId, "proveedor"),
    unitPrice: positive(input.unitPrice, "precio unitario"),
    quantity: quantity(input.quantity, enumValue(saleType, "tipo de venta")),
    expirationDate: localDateTime(input.expirationDate, "vencimiento")
  };
}

export function buildInventoryLossPayload(input, saleType) {
  return {
    quantity: quantity(input.quantity, enumValue(saleType, "tipo de venta")),
    reason: enumValue(input.reason, "motivo"),
    observation: optional(input.observation)
  };
}

export function createSubmissionGuard(onLoadingChange = () => {}) {
  let loading = false;
  return Object.freeze({
    get loading() { return loading; },
    async run(task) {
      if (loading) return undefined;
      loading = true;
      onLoadingChange(true);
      try { return await task(); }
      finally { loading = false; onLoadingChange(false); }
    }
  });
}

export function inventoryManagementErrorMessage(error) {
  const fields = Object.values(error?.fieldErrors ?? {}).filter(Boolean);
  if (fields.length) return fields.join(" ");
  if (error?.message) return error.message;
  if (error?.status === 403) return "No tenés permiso para modificar el inventario.";
  if (error?.status === 409) return "Los datos entran en conflicto con un registro existente.";
  return "No se pudo completar la operación de inventario.";
}

function quantity(value, saleType) {
  const normalized = decimal(value, "cantidad", false);
  const [, decimals = ""] = normalized.split(".");
  if (saleType === "UNIDAD" && decimals.replace(/0+$/, "")) {
    throw new Error("La cantidad de un producto por unidad debe ser entera.");
  }
  if (saleType === "GRANEL" && decimals.length > 3) {
    throw new Error("La cantidad a granel admite como máximo 3 decimales.");
  }
  return normalized;
}

function positive(value, label) { return decimal(value, label, false); }
function optionalNonNegative(value, label) {
  return text(value) ? decimal(value, label, true) : null;
}

function decimal(value, label, allowZero) {
  const normalized = text(value);
  if (!/^\d+(?:\.\d+)?$/.test(normalized)) throw new Error(`Ingresá un valor numérico válido para ${label}.`);
  const number = Number(normalized);
  if (!Number.isFinite(number) || (allowZero ? number < 0 : number <= 0)) {
    throw new Error(`${capitalize(label)} debe ser ${allowZero ? "cero o mayor" : "mayor que cero"}.`);
  }
  return normalized;
}

function localDateTime(value, label) {
  const normalized = required(value, label);
  if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2})?$/.test(normalized)
      || Number.isNaN(Date.parse(normalized))) {
    throw new Error(`Ingresá una fecha y hora válida para ${label}.`);
  }
  return normalized;
}

function enumValue(value, label) { return required(value, label).toUpperCase(); }
function required(value, label) {
  const normalized = text(value);
  if (!normalized) throw new Error(`Completá ${label}.`);
  return normalized;
}
function optional(value) { return text(value) || null; }
function text(value) { return String(value ?? "").trim(); }
function capitalize(value) { return value.charAt(0).toUpperCase() + value.slice(1); }

export const INVENTORY_EXPORT_PERMISSION = "PRODUCT_EXPORT_INVENTORY_REPORT";
const AUTHORIZED_ROLES = new Set(["ADMIN", "VENDEDOR", "ALMACENISTA"]);

export function canExportInventory(session) {
  if (!session || !AUTHORIZED_ROLES.has(session.role)) return false;
  return !Array.isArray(session.permissions) || session.permissions.includes(INVENTORY_EXPORT_PERMISSION);
}

export function buildInventoryExportQuery(filters, format) {
  const query = {format};
  for (const key of ["productId", "type", "source", "from", "to"]) {
    const value = filters?.[key]?.trim?.() ?? "";
    if (value) query[key] = value;
  }
  return query;
}

export function fallbackReportFilename(format) {
  return `movimientos-inventario.${format === "xlsx" ? "xlsx" : "csv"}`;
}

export function createExportGuard(onLoadingChange) {
  let loading = false;
  return {
    get loading() { return loading; },
    async run(task) {
      if (loading) return undefined;
      loading = true;
      onLoadingChange(true);
      try {
        return await task();
      } finally {
        loading = false;
        onLoadingChange(false);
      }
    }
  };
}

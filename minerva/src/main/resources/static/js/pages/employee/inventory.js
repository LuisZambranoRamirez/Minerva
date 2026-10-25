import {download, request} from "../../core/api.js";
import {busy, el} from "../../core/dom.js";
import {dateTime} from "../../core/format.js";
import {employeeShell} from "../../ui/shells.js";
import {badge, button, card, emptyState, errorState, heading, loadingState, table} from "../../ui/components.js";
import {fieldErrors, toast} from "../../ui/feedback.js";
import {installProductImageFallback, productImageAttributes} from "../../core/product-image.mjs";
import {buildInventoryExportQuery, canExportInventory, createExportGuard, fallbackReportFilename} from "./inventory-export.mjs";
import {buildInventoryLossPayload, buildRegisterProductPayload, buildStockEntryPayload, canManageInventory, createInventoryFilterState, createSubmissionGuard, inventoryManagementErrorMessage} from "./inventory-management.mjs";

const SOURCES = ["STOCK_ENTRY", "ORDER_CONFIRMATION", "ORDER_CANCELLATION", "INVENTORY_LOSS", "PRODUCT_RETURN", "DIRECT_SALE"];
const CATEGORIES = ["BEBIDAS", "ABARROTES_SECOS", "CAFE_INFUSIONES", "LACTEOS", "CARNES", "SNACKS_GOLOSINAS", "CUIDADO_PERSONAL", "LIMPIEZA_HOGAR", "BEBES", "MASCOTAS", "OTROS"];
const LOSS_REASONS = ["DAÑADO", "VENCIMIENTO", "PERDIDO", "ROBO", "OTROS"];

export function renderInventory({session, query}) {
  const filters = createInventoryFilterState({productId: query.get("productId") ?? ""});
  const root = el("div", {className: "mx-auto grid max-w-7xl gap-6"}, [heading("Productos e inventario"), loadingState("Cargando inventario…")]);
  employeeShell(session, root);
  refresh();

  async function refresh({focusHistory = false} = {}) {
    try {
      const canLoadSuppliers = canManageInventory(session, "product") || canManageInventory(session, "entry");
      const [products, lowStock, movements, suppliers] = await Promise.all([
        request("/api/v1/products", {token: session.accessToken}),
        request("/api/v1/products/low-stock", {token: session.accessToken}),
        request("/api/v1/products/inventory-movements", {token: session.accessToken, query: {...filters.query(), page: 0, size: 100}}),
        canLoadSuppliers ? request("/api/v1/suppliers", {token: session.accessToken}) : Promise.resolve([])
      ]);
      renderContent(products, lowStock, movements, suppliers);
      if (focusHistory) requestAnimationFrame(() => {
        const history = root.querySelector("#inventory-history");
        history?.scrollIntoView({behavior: "smooth", block: "start"});
        history?.focus({preventScroll: true});
      });
    } catch (error) { root.replaceChildren(errorState(error.message, () => refresh())); }
  }

  function renderContent(products, lowStock, movements, suppliers) {
    const byId = new Map(products.map(product => [product.productId, product]));
    const register = canManageInventory(session, "product") ? button("Registrar producto", {onClick: () => productDialog(suppliers)}) : null;
    root.replaceChildren(
      heading("Productos e inventario", "Gestioná stock auditable y consultá su historial.", register),
      el("div", {className: "grid gap-5 md:grid-cols-3"}, [metric("Productos", products.length), metric("Bajo stock", lowStock.length, lowStock.length ? "warning" : "success"), metric("Movimientos encontrados", movements.length)]),
      historyPanel(products, movements, byId),
      card([el("h2", {className: "mb-4 text-xl font-bold text-primary", text: "Productos"}), productTable(products, suppliers)]),
      card([el("h2", {className: "mb-4 text-xl font-bold text-primary", text: "Productos con bajo stock"}), lowStock.length ? table([
        {label: "SKU", key: "sku"}, {label: "Producto", key: "productName"},
        {label: "Stock", render: product => badge(String(product.stock ?? "—"), "warning")},
        {label: "Reposición", render: product => el("span", {text: product.reorderLevel ?? "—"})}
      ], lowStock) : emptyState("No hay productos por debajo del punto de reposición.")])
    );
  }

  function productTable(products, suppliers) {
    return table([
      {label: "Imagen", render: productImage}, {label: "SKU", key: "sku"}, {label: "Producto", key: "productName"},
      {label: "Categoría", key: "category"}, {label: "Venta", key: "saleType"}, {label: "Stock", render: p => el("b", {text: p.stock ?? "—"})},
      {label: "Reposición", render: p => el("span", {text: p.reorderLevel ?? "—"})},
      {label: "Acciones", render: product => el("div", {className: "flex flex-wrap gap-2"}, [
        canManageInventory(session, "entry") ? smallButton("Registrar entrada", () => stockEntryDialog(product, suppliers)) : null,
        canManageInventory(session, "loss") ? smallButton("Registrar pérdida", () => lossDialog(product)) : null,
        smallButton("Ver movimientos", async () => { filters.patch({productId: product.productId}); await refresh({focusHistory: true}); }, "ghost")
      ])}
    ], products, {empty: "No hay productos registrados."});
  }

  function historyPanel(products, movements, byId) {
    const values = filters.get();
    const controls = {
      productId: selectField("Producto", "productId", [["", "Todos los productos"], ...products.map(p => [p.productId, `${p.sku} · ${p.productName}`])], values.productId),
      type: selectField("Tipo", "type", [["", "Todos"], ["ENTRADA", "Entrada"], ["SALIDA", "Salida"]], values.type),
      source: selectField("Origen", "source", [["", "Todos"], ...SOURCES.map(v => [v, v])], values.source),
      from: inputField("Desde", "from", "datetime-local", values.from), to: inputField("Hasta", "to", "datetime-local", values.to)
    };
    const read = () => Object.fromEntries(Object.entries(controls).map(([key, item]) => [key, item.control.value]));
    const actions = [button("Consultar", {onClick: async () => { filters.replace(read()); await refresh(); }})];
    if (canExportInventory(session)) {
      const exportButtons = [];
      const guard = createExportGuard(loading => exportButtons.forEach(item => busy(item, loading)));
      const exportReport = format => guard.run(async () => {
        filters.replace(read());
        try {
          await download("/api/v1/products/inventory-movements/export", {query: buildInventoryExportQuery(filters.get(), format), token: session.accessToken, fallbackFilename: fallbackReportFilename(format)});
          toast("Reporte descargado correctamente.");
        } catch (error) { toast(error.message, {tone: "error"}); }
      });
      exportButtons.push(button("Exportar Excel", {onClick: () => exportReport("xlsx")}), button("Exportar CSV", {tone: "ghost", onClick: () => exportReport("csv")}));
      actions.push(...exportButtons);
    }
    return card([
      el("div", {className: "scroll-mt-6 outline-none", attrs: {id: "inventory-history", tabindex: "-1"}}, [el("h2", {className: "text-xl font-bold text-primary", text: "Historial"}), el("p", {className: "mt-1 text-sm text-on-surface-variant", text: "Los mismos filtros gobiernan la consulta y las exportaciones."})]),
      el("div", {className: "mt-5 grid gap-4 md:grid-cols-5"}, Object.values(controls).map(item => item.label)),
      el("div", {className: "my-5 flex flex-wrap gap-3"}, actions),
      table([
        {label: "Fecha", render: m => el("span", {text: dateTime(m.registrationDate)})}, {label: "Producto", render: m => productCell(byId.get(m.productId), m.productId)},
        {label: "Cantidad", render: m => badge(String(m.quantity ?? "—"), Number(m.quantity) < 0 ? "error" : "success")}, {label: "Antes", render: m => el("span", {text: m.stockBefore ?? "—"})},
        {label: "Después", render: m => el("b", {text: m.stockAfter ?? "—"})}, {label: "Tipo", render: m => badge(String(m.type ?? "—"), m.type === "SALIDA" ? "error" : "info")},
        {label: "Origen", render: m => el("span", {text: `${m.source ?? "MOVIMIENTO"}${m.sourceId ? ` · ${m.sourceId}` : ""}`})}
      ], movements, {empty: "No hay movimientos para mostrar."})
    ]);
  }

  function productDialog(suppliers) {
    formDialog("Registrar producto", "Registrar producto", [
      inputField("SKU", "sku"), inputField("Producto", "productName"), selectField("Tipo de venta", "saleType", [["UNIDAD", "Unidad"], ["GRANEL", "Granel"]]),
      selectField("Categoría", "productCategory", CATEGORIES.map(v => [v, v])), selectField("Estrategia de ganancia", "gainStrategy", [["PORCENTAJE", "Porcentaje"], ["RECARGO_FIJO", "Recargo fijo"]]),
      inputField("Ganancia", "gainAmount", "number", "", {min: "0.01", step: "0.01"}), inputField("Nivel de reposición", "reorderLevel", "number", "", {min: "0", step: "0.001", required: false}),
      inputField("Código de barras", "barCode", "text", "", {required: false}), selectField("Proveedor", "purchasedFromSupplierId", suppliers.map(s => [s.supplierId, s.supplierName])),
      inputField("Precio de compra", "purchaseUnitPrice", "number", "", {min: "0.01", step: "0.01"}), inputField("Cantidad inicial", "purchaseQuantity", "number", "", {min: "0.001", step: "0.001"}),
      inputField("Vencimiento", "purchaseExpirationDate", "datetime-local")
    ], buildRegisterProductPayload, "/api/v1/products", "Producto registrado correctamente.");
  }

  function stockEntryDialog(product, suppliers) {
    formDialog(`Registrar entrada · ${product.productName}`, "Registrar entrada", [selectField("Proveedor", "supplierId", suppliers.map(s => [s.supplierId, s.supplierName])), inputField("Precio unitario", "unitPrice", "number", "", {min: "0.01", step: "0.01"}), inputField("Cantidad", "quantity", "number", "", {min: "0.001", step: product.saleType === "UNIDAD" ? "1" : "0.001"}), inputField("Vencimiento", "expirationDate", "datetime-local")], values => buildStockEntryPayload(values, product.saleType), `/api/v1/products/${encodeURIComponent(product.productId)}/stock-entries`, "Entrada registrada correctamente.");
  }

  function lossDialog(product) {
    formDialog(`Registrar pérdida · ${product.productName}`, "Registrar pérdida", [inputField("Cantidad", "quantity", "number", "", {min: "0.001", step: product.saleType === "UNIDAD" ? "1" : "0.001"}), selectField("Motivo", "reason", LOSS_REASONS.map(v => [v, v])), inputField("Observación", "observation", "text", "", {required: false})], values => buildInventoryLossPayload(values, product.saleType), `/api/v1/products/${encodeURIComponent(product.productId)}/inventory-losses`, "Pérdida registrada correctamente.");
  }

  function formDialog(title, submitLabel, fields, build, path, success) {
    const dialog = el("dialog", {className: "w-[min(48rem,calc(100%-2rem))] rounded-xl bg-white p-0 shadow-2xl", attrs: {"aria-labelledby": "inventory-dialog-title"}});
    const form = el("form", {className: "grid gap-5 p-6"});
    const message = el("p", {className: "hidden text-sm text-error", attrs: {role: "alert", "aria-live": "assertive"}});
    const cancel = button("Cancelar", {tone: "ghost"}); const submit = button(submitLabel, {type: "submit"});
    const guard = createSubmissionGuard(loading => { busy(submit, loading); cancel.disabled = loading; });
    const close = (force = false) => { if (!guard.loading || force) { dialog.close(); dialog.remove(); } };
    cancel.addEventListener("click", () => close());
    form.append(el("h2", {className: "text-xl font-bold text-primary", text: title, attrs: {id: "inventory-dialog-title"}}), el("div", {className: "grid gap-4 sm:grid-cols-2"}, fields.map(item => item.label)), message, el("div", {className: "flex justify-end gap-3"}, [cancel, submit]));
    form.addEventListener("submit", event => { event.preventDefault(); guard.run(async () => {
      message.classList.add("hidden"); fieldErrors(form, {});
      try { await request(path, {method: "POST", token: session.accessToken, body: build(Object.fromEntries(new FormData(form).entries()))}); close(true); toast(success, {tone: "success"}); await refresh(); }
      catch (error) { fieldErrors(form, error.fieldErrors); message.textContent = inventoryManagementErrorMessage(error); message.classList.remove("hidden"); form.querySelector("[aria-invalid], input, select")?.focus(); }
    }); });
    dialog.addEventListener("cancel", event => { event.preventDefault(); close(); });
    dialog.append(form); document.body.append(dialog); dialog.showModal(); requestAnimationFrame(() => form.querySelector("input, select")?.focus());
  }
}

function inputField(label, name, type = "text", value = "", options = {}) { const control = el("input", {className: "field", attrs: {name, id: name, type, value, required: options.required ?? true, min: options.min, step: options.step}}); return labeled(label, name, control); }
function selectField(label, name, options, value = "") { const control = el("select", {className: "field", attrs: {name, id: name}}, options.map(([key, text]) => el("option", {text, attrs: {value: key, selected: key === value}}))); return labeled(label, name, control); }
function labeled(text, name, control) { return {control, label: el("label", {className: "grid gap-1.5 text-sm font-semibold", attrs: {for: name}}, [el("span", {text}), control])}; }
function smallButton(label, onClick, tone = "primary") { const result = button(label, {tone, onClick}); result.classList.replace("py-3", "py-2"); result.classList.replace("px-4", "px-3"); return result; }
function metric(label, value, tone = "neutral") { return card([el("p", {className: "text-sm font-semibold text-on-surface-variant", text: label}), el("p", {className: `mt-2 text-4xl font-bold ${tone === "warning" ? "text-amber-700" : tone === "success" ? "text-emerald-700" : "text-primary"}`, text: value})]); }
function productImage(product) { const img = el("img", {className: "h-14 w-14 rounded-lg border border-surface-container object-cover", attrs: productImageAttributes(product)}); installProductImageFallback(img); return img; }
function productCell(product, fallbackId) { return el("div", {className: "flex min-w-48 items-center gap-3"}, [product ? productImage(product) : null, el("span", {className: "font-semibold", text: product?.productName ?? fallbackId})]); }

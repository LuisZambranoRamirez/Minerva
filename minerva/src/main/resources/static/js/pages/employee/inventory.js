import {request} from "../../core/api.js";
import {el} from "../../core/dom.js";
import {dateTime} from "../../core/format.js";
import {employeeShell} from "../../ui/shells.js";
import {badge, card, emptyState, errorState, heading, loadingState, table} from "../../ui/components.js";

const typeTone = type => type === "SALIDA" ? "error" : "info";
const movementLabel = movement => `${movement.source ?? "MOVIMIENTO"}${movement.sourceId ? ` · ${movement.sourceId}` : ""}`;

export function renderInventory({session, query}) {
  const selectedProductId = query.get("productId") ?? "";
  const root = el("div", {className: "mx-auto grid max-w-7xl gap-6"}, [
    heading("Productos e inventario", "Listado, bajo stock y libro de movimientos real del backend."),
    loadingState("Cargando inventario…")
  ]);
  employeeShell(session, root);
  load(selectedProductId);

  async function load(productId) {
    try {
      const [products, lowStock, movements] = await Promise.all([
        request("/api/v1/products", {token: session.accessToken}),
        request("/api/v1/products/low-stock", {token: session.accessToken}),
        request(productId ? `/api/v1/products/${encodeURIComponent(productId)}/movements` : "/api/v1/products/inventory-movements", {token: session.accessToken})
      ]);
      const productsById = new Map(products.map(product => [product.productId, product]));
      root.replaceChildren(
        heading("Productos e inventario", "El libro de movimientos es de solo lectura: no se simulan ajustes manuales.", productFilter(products, productId)),
        el("div", {className: "grid gap-5 md:grid-cols-3"}, [
          metric("Productos", products.length),
          metric("Bajo stock", lowStock.length, lowStock.length ? "warning" : "success"),
          metric("Movimientos visibles", movements.length)
        ]),
        card([
          el("h2", {className: "mb-4 text-xl font-bold text-primary", text: "Productos"}),
          table([
            {label: "SKU", key: "sku"},
            {label: "Producto", key: "productName"},
            {label: "Categoría", key: "category"},
            {label: "Venta", key: "saleType"},
            {label: "Stock", render: product => el("b", {text: product.stock ?? "—"})},
            {label: "Reposición", render: product => el("span", {text: product.reorderLevel ?? "—"})},
            {label: "Ledger", render: product => el("a", {className: "font-semibold text-secondary hover:underline", text: "Ver", attrs: {href: `#/employee/inventory?productId=${encodeURIComponent(product.productId)}`}})}
          ], products, {empty: "No hay productos registrados."})
        ]),
        card([
          el("h2", {className: "mb-4 text-xl font-bold text-primary", text: "Productos con bajo stock"}),
          lowStock.length ? table([
            {label: "SKU", key: "sku"},
            {label: "Producto", key: "productName"},
            {label: "Stock", render: product => badge(String(product.stock ?? "—"), "warning")},
            {label: "Reposición", render: product => el("span", {text: product.reorderLevel ?? "—"})}
          ], lowStock) : emptyState("No hay productos por debajo del punto de reposición.")
        ]),
        card([
          el("h2", {className: "mb-4 text-xl font-bold text-primary", text: productId ? `Movimientos de ${productsById.get(productId)?.productName ?? productId}` : "Libro de movimientos"}),
          table([
            {label: "Fecha", render: movement => el("span", {text: dateTime(movement.registrationDate)})},
            {label: "Producto", render: movement => productCell(productsById.get(movement.productId), movement.productId)},
            {label: "Cantidad", render: movement => badge(String(movement.quantity ?? "—"), Number(movement.quantity) < 0 ? "error" : "success")},
            {label: "Antes", render: movement => el("span", {text: movement.stockBefore ?? "—"})},
            {label: "Después", render: movement => el("b", {text: movement.stockAfter ?? "—"})},
            {label: "Tipo", render: movement => badge(String(movement.type ?? "—"), typeTone(movement.type))},
            {label: "Origen", render: movement => el("span", {text: movementLabel(movement)})}
          ], movements, {empty: "No hay movimientos para mostrar."})
        ])
      );
    } catch (error) {
      root.replaceChildren(errorState(error.message, () => load(productId)));
    }
  }

  function productFilter(products, productId) {
    const select = el("select", {className: "field min-w-64", attrs: {"aria-label": "Filtrar ledger por producto"}}, [
      el("option", {text: "Todos los productos", attrs: {value: "", selected: !productId}}),
      ...products.map(product => el("option", {text: `${product.sku} · ${product.productName}`, attrs: {value: product.productId, selected: product.productId === productId}}))
    ]);
    select.addEventListener("change", () => {
      location.hash = select.value ? `#/employee/inventory?productId=${encodeURIComponent(select.value)}` : "#/employee/inventory";
    });
    return select;
  }
}

function metric(label, value, tone = "info") {
  return card([
    el("p", {className: "text-sm font-semibold text-on-surface-variant", text: label}),
    el("p", {className: `mt-3 text-4xl font-black ${tone === "warning" ? "text-amber-700" : tone === "success" ? "text-emerald-700" : "text-primary"}`, text: value})
  ]);
}

function productCell(product, productId) {
  if (!product) return el("span", {className: "font-mono", text: productId});
  return el("div", {}, [
    el("b", {text: product.productName}),
    el("p", {className: "font-mono text-xs text-on-surface-variant", text: product.sku})
  ]);
}

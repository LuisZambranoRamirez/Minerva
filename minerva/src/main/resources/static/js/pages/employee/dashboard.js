import {request} from "../../core/api.js";
import {el} from "../../core/dom.js";
import {money, statusLabel} from "../../core/format.js";
import {employeeShell} from "../../ui/shells.js";
import {badge, card, errorState, heading, loadingState, table} from "../../ui/components.js";

const openStatuses = new Set(["PENDIENTE", "CONFIRMADO", "EN_PREPARACION", "EN_REPARTO"]);
const statusTone = status => status === "ENTREGADO" ? "success" : status === "CANCELADO" ? "error" : status === "PENDIENTE" ? "warning" : "info";

export function renderDashboard({session}) {
  const root = el("div", {className: "mx-auto grid max-w-7xl gap-6"}, [
    heading("Dashboard operativo", "Indicadores calculados desde endpoints reales de pedidos y productos."),
    loadingState("Cargando datos operativos…")
  ]);
  employeeShell(session, root);
  load();

  async function load() {
    try {
      const [orders, products, lowStock] = await Promise.all([
        request("/api/v1/orders", {token: session.accessToken}),
        request("/api/v1/products", {token: session.accessToken}),
        request("/api/v1/products/low-stock", {token: session.accessToken})
      ]);
      const byStatus = countBy(orders, order => order.status);
      const openOrders = orders.filter(order => openStatuses.has(order.status));
      root.replaceChildren(
        heading("Dashboard operativo", "Sin métricas inventadas: todo sale de pedidos, productos y bajo stock."),
        el("div", {className: "grid gap-5 md:grid-cols-3"}, [
          metric("Pedidos abiertos", openOrders.length, "Estados pendientes de operación"),
          metric("Productos registrados", products.length, "Listado autorizado de productos"),
          metric("Productos con bajo stock", lowStock.length, "Según regla del backend", lowStock.length ? "warning" : "success")
        ]),
        card([
          el("h2", {className: "mb-4 text-xl font-bold text-primary", text: "Pedidos por estado"}),
          table([
            {label: "Estado", render: row => badge(statusLabel(row.status), statusTone(row.status))},
            {label: "Cantidad", render: row => el("b", {text: row.count})}
          ], Object.entries(byStatus).map(([status, count]) => ({status, count})), {empty: "No hay pedidos registrados."})
        ]),
        card([
          el("h2", {className: "mb-4 text-xl font-bold text-primary", text: "Bajo stock"}),
          table([
            {label: "SKU", key: "sku"},
            {label: "Producto", key: "productName"},
            {label: "Stock", render: product => el("b", {text: product.stock ?? "—"})},
            {label: "Reposición", render: product => el("span", {text: product.reorderLevel ?? "—"})}
          ], lowStock, {empty: "No hay productos por debajo del punto de reposición."})
        ]),
        card([
          el("h2", {className: "mb-4 text-xl font-bold text-primary", text: "Pedidos recientes"}),
          table([
            {label: "Pedido", render: order => el("a", {className: "font-mono font-bold text-secondary hover:underline", text: order.orderId, attrs: {href: `#/employee/orders/${encodeURIComponent(order.orderId)}`}})},
            {label: "Cliente", render: order => el("span", {className: "font-mono", text: order.customerId})},
            {label: "Estado", render: order => badge(statusLabel(order.status), statusTone(order.status))},
            {label: "Total", render: order => el("b", {text: money(order.total)})}
          ], orders.slice(0, 8), {empty: "No hay pedidos para mostrar."})
        ])
      );
    } catch (error) {
      root.replaceChildren(errorState(error.message, load));
    }
  }
}

function metric(label, value, hint, tone = "info") {
  return card([
    el("p", {className: "text-sm font-semibold text-on-surface-variant", text: label}),
    el("p", {className: `mt-3 text-4xl font-black ${tone === "warning" ? "text-amber-700" : tone === "success" ? "text-emerald-700" : "text-primary"}`, text: value}),
    el("p", {className: "mt-2 text-sm text-on-surface-variant", text: hint})
  ]);
}

function countBy(items, keyFn) {
  return items.reduce((acc, item) => {
    const key = keyFn(item) ?? "SIN_ESTADO";
    acc[key] = (acc[key] ?? 0) + 1;
    return acc;
  }, {});
}

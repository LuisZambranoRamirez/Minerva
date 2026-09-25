import {request} from "../../core/api.js";
import {el} from "../../core/dom.js";
import {money, dateTime, statusLabel} from "../../core/format.js";
import {employeeShell} from "../../ui/shells.js";
import {badge, button, card, errorState, heading, loadingState, table} from "../../ui/components.js";

const statuses = ["", "PENDIENTE", "CONFIRMADO", "EN_PREPARACION", "EN_REPARTO", "ENTREGADO", "CANCELADO"];
const tone = status => status === "ENTREGADO" ? "success" : status === "CANCELADO" ? "error" : status === "PENDIENTE" ? "warning" : "info";

export function renderEmployeeOrders({session, query}) {
  const selectedStatus = query.get("status") ?? "";
  const root = el("div", {className: "mx-auto grid max-w-7xl gap-6"}, [
    heading("Pedidos", "Listado operativo autorizado por rol, sin paginación ni acciones simuladas."),
    loadingState("Cargando pedidos…")
  ]);
  employeeShell(session, root);
  load(selectedStatus);

  async function load(status) {
    try {
      const orders = await request("/api/v1/orders", {token: session.accessToken, query: {status}});
      root.replaceChildren(
        heading("Pedidos", "Filtrá por estado y abrí el detalle para operar transiciones válidas.", filters(status)),
        card(table([
          {label: "Pedido", render: order => el("a", {className: "font-mono font-bold text-secondary hover:underline", text: order.orderId, attrs: {href: `#/employee/orders/${encodeURIComponent(order.orderId)}`}})},
          {label: "Cliente", render: order => el("span", {className: "font-mono", text: order.customerId})},
          {label: "Fecha", render: order => el("span", {text: dateTime(order.registrationDate)})},
          {label: "Estado", render: order => badge(statusLabel(order.status), tone(order.status))},
          {label: "Total", render: order => el("b", {text: money(order.total)})}
        ], orders, {empty: "No hay pedidos para el filtro seleccionado."}))
      );
    } catch (error) {
      root.replaceChildren(errorState(error.message, () => load(status)));
    }
  }

  function filters(status) {
    const select = el("select", {className: "field min-w-56", attrs: {"aria-label": "Filtrar por estado"}}, statuses.map(value => el("option", {text: value ? statusLabel(value) : "Todos los estados", attrs: {value, selected: value === status}})));
    const apply = button("Aplicar filtro", {tone: "ghost", onClick: () => {
      const next = select.value ? `/employee/orders?status=${encodeURIComponent(select.value)}` : "/employee/orders";
      location.hash = `#${next}`;
    }});
    return el("div", {className: "flex flex-col gap-3 sm:flex-row sm:items-center"}, [select, apply]);
  }
}

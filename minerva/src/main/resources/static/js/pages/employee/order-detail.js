import {request} from "../../core/api.js";
import {el, busy} from "../../core/dom.js";
import {money, dateTime, statusLabel} from "../../core/format.js";
import {employeeShell} from "../../ui/shells.js";
import {badge, button, card, errorState, heading, loadingState, table} from "../../ui/components.js";
import {toast, confirmDialog} from "../../ui/feedback.js";

const statusTone = status => status === "ENTREGADO" ? "success" : status === "CANCELADO" ? "error" : status === "PENDIENTE" ? "warning" : "info";
const transitions = [
  {label: "Confirmar", path: "confirm", from: "PENDIENTE", roles: ["ADMIN", "VENDEDOR"]},
  {label: "Preparar", path: "prepare", from: "CONFIRMADO", roles: ["ADMIN", "ALMACENISTA"]},
  {label: "Despachar", path: "dispatch", from: "EN_PREPARACION", roles: ["ADMIN", "ALMACENISTA"]},
  {label: "Entregar", path: "deliver", from: "EN_REPARTO", roles: ["ADMIN", "ALMACENISTA"]},
  {label: "Cancelar", path: "cancel", from: ["PENDIENTE", "CONFIRMADO", "EN_PREPARACION"], roles: ["ADMIN", "VENDEDOR"], danger: true}
];

export function renderEmployeeOrderDetail({session, params}) {
  const root = el("div", {className: "mx-auto grid max-w-7xl gap-6"}, [loadingState("Cargando pedido…")]);
  employeeShell(session, root);
  load();

  async function load() {
    try {
      const [order, orderTransitions, products] = await Promise.all([
        request(`/api/v1/orders/${encodeURIComponent(params.orderId)}`, {token: session.accessToken}),
        request(`/api/v1/orders/${encodeURIComponent(params.orderId)}/transitions`, {token: session.accessToken}),
        request("/api/v1/products", {token: session.accessToken})
      ]);
      const names = new Map(products.map(product => [product.productId, product.productName]));
      const actions = allowedTransitions(session.role, order.status).map(action => actionButton(action, order.orderId));
      root.replaceChildren(
        heading(`Pedido ${order.orderId}`, `Cliente ${order.customerId}`, actions.length ? el("div", {className: "flex flex-wrap gap-3"}, actions) : null),
        el("div", {className: "grid gap-5 md:grid-cols-4"}, [
          summary("Estado", badge(statusLabel(order.status), statusTone(order.status))),
          summary("Total", money(order.total)),
          summary("Creado", dateTime(order.registrationDate)),
          summary("Actualizado", dateTime(order.updatedDate))
        ]),
        card([
          el("h2", {className: "mb-4 text-xl font-bold text-primary", text: "Productos"}),
          table([
            {label: "Producto", render: item => el("div", {}, [el("b", {text: names.get(item.productId) ?? "Producto"}), el("p", {className: "font-mono text-xs text-on-surface-variant", text: item.productId})])},
            {label: "Cantidad", key: "quantity"},
            {label: "Precio unitario", render: item => el("span", {text: money(item.unitPrice)})},
            {label: "Subtotal", render: item => el("b", {text: money(Number(item.quantity) * Number(item.unitPrice))})}
          ], order.details)
        ]),
        card([
          el("h2", {className: "mb-4 text-xl font-bold text-primary", text: "Transiciones"}),
          table([
            {label: "Fecha", render: item => el("span", {text: dateTime(item.registrationDate)})},
            {label: "Cambio", render: item => el("span", {text: `${statusLabel(item.previousStatus)} → ${statusLabel(item.newStatus)}`})},
            {label: "Actor", render: item => el("span", {className: "font-mono", text: item.actorId})}
          ], orderTransitions, {empty: "No hay transiciones registradas."})
        ])
      );
    } catch (error) {
      root.replaceChildren(errorState(error.message, load));
    }
  }

  function actionButton(action, orderId) {
    const trigger = button(action.label, {tone: action.danger ? "danger" : "primary"});
    trigger.addEventListener("click", async () => {
      if (action.danger) {
        const accepted = await confirmDialog({title: "Cancelar pedido", message: "El backend permite cancelar solo pedidos pendientes, confirmados o en preparación.", confirmLabel: "Cancelar pedido", danger: true});
        if (!accepted) return;
      }
      busy(trigger, true);
      try {
        await request(`/api/v1/orders/${encodeURIComponent(orderId)}/${action.path}`, {method: "POST", token: session.accessToken});
        toast("Pedido actualizado.");
        await load();
      } catch (error) {
        toast(error.message, {tone: "error"});
      } finally {
        busy(trigger, false);
      }
    });
    return trigger;
  }
}

function allowedTransitions(role, status) {
  return transitions.filter(action => action.roles.includes(role) && (Array.isArray(action.from) ? action.from.includes(status) : action.from === status));
}

function summary(label, value) {
  return card([
    el("p", {className: "text-sm text-on-surface-variant", text: label}),
    value instanceof Node ? value : el("p", {className: "mt-2 text-lg font-bold text-primary", text: value})
  ]);
}

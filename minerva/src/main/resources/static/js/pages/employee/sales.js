import {request} from "../../core/api.js";
import {el, busy} from "../../core/dom.js";
import {money, dateTime} from "../../core/format.js";
import {employeeShell} from "../../ui/shells.js";
import {badge, button, card, errorState, field, heading, loadingState, table} from "../../ui/components.js";
import {toast, fieldErrors} from "../../ui/feedback.js";

const paymentMethods = ["EFECTIVO", "DIGITAL"];
const returnReasons = ["DAÑADO", "VENCIDO", "EQUIVOCACION", "OTROS"];

export function renderSales({session, query}) {
  const saleId = query.get("saleId") ?? "";
  const root = el("div", {className: "mx-auto grid max-w-7xl gap-6"}, [
    heading("Ventas, pagos y devoluciones", "Operaciones soportadas por /api/v1/sales; sin cancelaciones, notas de crédito ni reembolsos simulados."),
    loadingState("Cargando ventas…")
  ]);
  employeeShell(session, root);
  load(saleId);

  async function load(selectedSaleId = "") {
    try {
      const [sales, customers, products, returns] = await Promise.all([
        request("/api/v1/sales", {token: session.accessToken}),
        request("/api/v1/customers", {token: session.accessToken}),
        request("/api/v1/products", {token: session.accessToken}),
        request("/api/v1/sales/returns", {token: session.accessToken})
      ]);
      const customersById = new Map(customers.map(customer => [customer.customerId, customer]));
      const productsById = new Map(products.map(product => [product.productId, product]));
      const selectedSale = selectedSaleId
        ? await request(`/api/v1/sales/${encodeURIComponent(selectedSaleId)}`, {token: session.accessToken})
        : null;
      root.replaceChildren(
        heading("Ventas, pagos y devoluciones", "Registrar venta directa, agregar pagos y registrar devoluciones reales por detalle."),
        el("div", {className: "grid gap-5 xl:grid-cols-[minmax(0,1.2fr)_minmax(380px,.8fr)]"}, [
          card([el("h2", {className: "mb-4 text-xl font-bold text-primary", text: "Registrar venta"}), saleForm(customers, products)]),
          card([el("h2", {className: "mb-4 text-xl font-bold text-primary", text: "Resumen"}), summaryMetrics(sales, returns)])
        ]),
        card([el("h2", {className: "mb-4 text-xl font-bold text-primary", text: "Ventas"}), salesTable(sales, customersById)]),
        selectedSale ? saleDetail(selectedSale, productsById) : null,
        card([el("h2", {className: "mb-4 text-xl font-bold text-primary", text: "Devoluciones registradas"}), returnsTable(returns)])
      );
    } catch (error) {
      root.replaceChildren(errorState(error.message, () => load(selectedSaleId)));
    }
  }

  function saleForm(customers, products) {
    const form = el("form", {className: "grid gap-4", attrs: {novalidate: true}});
    const customer = select("customerId", "Cliente", customers.map(item => [item.customerId, item.fullName]));
    const lines = el("div", {className: "grid gap-3"});
    const addLine = button("Agregar producto", {tone: "ghost", onClick: () => lines.append(line(products))});
    const initialPayment = field({name: "initialPayment", label: "Pago inicial opcional", type: "number", required: false});
    initialPayment.querySelector("input").setAttribute("step", "0.10");
    initialPayment.querySelector("input").setAttribute("min", "0.10");
    const paymentMethod = select("paymentMethod", "Método del pago inicial", paymentMethods.map(value => [value, label(value)]), false);
    const msg = el("p", {className: "hidden rounded-lg p-3", attrs: {role: "status"}});
    const submit = button("Registrar venta", {type: "submit"});
    lines.append(line(products));
    form.append(customer, lines, addLine, el("div", {className: "grid gap-4 md:grid-cols-2"}, [initialPayment, paymentMethod]), msg, submit);
    form.addEventListener("submit", async event => {
      event.preventDefault();
      msg.classList.add("hidden");
      fieldErrors(form);
      if (!form.checkValidity()) return form.reportValidity();
      const items = [...lines.querySelectorAll("[data-sale-line]")].map(node => ({
        productId: node.querySelector("[name='productId']").value,
        quantity: Number(node.querySelector("[name='quantity']").value),
        unitPrice: node.querySelector("[name='unitPrice']").value === "" ? null : Number(node.querySelector("[name='unitPrice']").value)
      })).filter(item => item.productId && item.quantity > 0);
      if (!items.length) return showMsg(msg, "Agregá al menos un producto válido.", true);
      const amount = Number(form.elements.namedItem("initialPayment").value);
      const payments = amount > 0 ? [{amount, paymentMethod: form.elements.namedItem("paymentMethod").value}] : [];
      busy(submit, true);
      try {
        await request("/api/v1/sales", {method: "POST", token: session.accessToken, body: {customerId: form.elements.namedItem("customerId").value, items, payments}});
        toast("Venta registrada.");
        await load();
      } catch (error) {
        fieldErrors(form, error.fieldErrors);
        showMsg(msg, error.message, true);
      } finally {
        busy(submit, false);
      }
    });
    return form;
  }

  function line(products) {
    const product = select("productId", "Producto", products.map(item => [item.productId, `${item.sku} · ${item.productName}`]));
    const quantity = field({name: "quantity", label: "Cantidad", type: "number"});
    quantity.querySelector("input").setAttribute("step", "0.01");
    quantity.querySelector("input").setAttribute("min", "0.01");
    const unitPrice = field({name: "unitPrice", label: "Precio pactado opcional", type: "number", required: false});
    unitPrice.querySelector("input").setAttribute("step", "0.01");
    unitPrice.querySelector("input").setAttribute("min", "0.01");
    const remove = button("Quitar", {tone: "ghost", onClick: event => event.currentTarget.closest("[data-sale-line]").remove()});
    return el("div", {className: "grid gap-3 rounded-xl border border-surface-container p-4 md:grid-cols-[1fr_130px_160px_auto] md:items-end", attrs: {"data-sale-line": "true"}}, [product, quantity, unitPrice, remove]);
  }

  function salesTable(sales, customersById) {
    return table([
      {label: "Venta", render: sale => el("a", {className: "font-mono font-bold text-secondary hover:underline", text: sale.saleId, attrs: {href: `#/employee/sales?saleId=${encodeURIComponent(sale.saleId)}`}})},
      {label: "Cliente", render: sale => el("span", {text: customersById.get(sale.customerId)?.fullName ?? sale.customerId})},
      {label: "Fecha", render: sale => el("span", {text: dateTime(sale.registrationDate)})},
      {label: "Total", render: sale => el("b", {text: money(sale.total)})},
      {label: "Pagado", render: sale => el("span", {text: money(sale.totalPaid)})},
      {label: "Saldo", render: sale => badge(money(sale.amountDue), Number(sale.amountDue) > 0 ? "warning" : "success")}
    ], sales, {empty: "No hay ventas registradas."});
  }

  function saleDetail(sale, productsById) {
    return card([
      heading(`Venta ${sale.saleId}`, `Cliente ${sale.customerId}`),
      el("div", {className: "mb-5 grid gap-4 md:grid-cols-3"}, [
        metric("Total", money(sale.total)), metric("Pagado", money(sale.totalPaid)), metric("Saldo", money(sale.amountDue))
      ]),
      el("div", {className: "grid gap-5 xl:grid-cols-2"}, [
        el("section", {}, [el("h3", {className: "mb-3 font-bold text-primary", text: "Detalles"}), detailsTable(sale.details, productsById)]),
        el("section", {}, [el("h3", {className: "mb-3 font-bold text-primary", text: "Pagos"}), table([
          {label: "Fecha", render: payment => el("span", {text: dateTime(payment.registrationDate)})},
          {label: "Método", render: payment => badge(label(payment.paymentMethod), "info")},
          {label: "Monto", render: payment => el("b", {text: money(payment.amount)})}
        ], sale.payments, {empty: "No hay pagos registrados."}), addPaymentForm(sale.saleId)])
      ])
    ]);
  }

  function detailsTable(details, productsById) {
    return table([
      {label: "Producto", render: item => productCell(productsById.get(item.productId), item.productId)},
      {label: "Cantidad", key: "quantity"},
      {label: "Unitario", render: item => el("span", {text: money(item.unitPrice)})},
      {label: "Subtotal", render: item => el("b", {text: money(Number(item.quantity) * Number(item.unitPrice))})},
      {label: "Devolver", render: item => returnForm(item.saleDetailId)}
    ], details, {empty: "La venta no tiene detalles."});
  }

  function addPaymentForm(saleId) {
    const form = el("form", {className: "mt-4 grid gap-3 rounded-xl bg-surface-container p-4 md:grid-cols-[1fr_1fr_auto] md:items-end"});
    const amount = field({name: "amount", label: "Agregar pago", type: "number"});
    amount.querySelector("input").setAttribute("step", "0.10");
    amount.querySelector("input").setAttribute("min", "0.10");
    const method = select("paymentMethod", "Método", paymentMethods.map(value => [value, label(value)]));
    const submit = button("Agregar", {type: "submit"});
    form.append(amount, method, submit);
    form.addEventListener("submit", async event => {
      event.preventDefault();
      if (!form.checkValidity()) return form.reportValidity();
      busy(submit, true);
      try {
        await request(`/api/v1/sales/${encodeURIComponent(saleId)}/payments`, {method: "POST", token: session.accessToken, body: {payments: [{amount: Number(form.elements.namedItem("amount").value), paymentMethod: form.elements.namedItem("paymentMethod").value}]}});
        toast("Pago agregado.");
        await load(saleId);
      } catch (error) { toast(error.message, {tone: "error"}); } finally { busy(submit, false); }
    });
    return form;
  }

  function returnForm(saleDetailId) {
    const form = el("form", {className: "grid gap-2"});
    const quantity = el("input", {className: "field w-28", attrs: {name: "quantity", type: "number", min: "0.01", step: "0.01", required: true, "aria-label": "Cantidad devuelta"}});
    const reason = el("select", {className: "field", attrs: {name: "reason", "aria-label": "Motivo de devolución"}}, returnReasons.map(value => el("option", {text: label(value), attrs: {value}})));
    const submit = button("Registrar devolución", {type: "submit", tone: "ghost"});
    form.append(quantity, reason, submit);
    form.addEventListener("submit", async event => {
      event.preventDefault();
      if (!form.checkValidity()) return form.reportValidity();
      busy(submit, true);
      try {
        await request(`/api/v1/sales/details/${encodeURIComponent(saleDetailId)}/returns`, {method: "POST", token: session.accessToken, body: {quantity: Number(quantity.value), reason: reason.value}});
        toast("Devolución registrada.");
        await load(query.get("saleId") ?? "");
      } catch (error) { toast(error.message, {tone: "error"}); } finally { busy(submit, false); }
    });
    return form;
  }

  function returnsTable(returns) {
    return table([
      {label: "Devolución", key: "productReturnId"},
      {label: "Detalle", key: "saleDetailId"},
      {label: "Cantidad", key: "quantity"},
      {label: "Motivo", render: item => badge(label(item.reason), "warning")},
      {label: "Fecha", render: item => el("span", {text: dateTime(item.registrationDate)})}
    ], returns, {empty: "No hay devoluciones registradas."});
  }
}

function select(name, text, options, required = true) {
  return el("label", {className: "grid gap-1.5 text-sm font-semibold"}, [
    el("span", {text}),
    el("select", {className: "field", attrs: {name, required}}, options.map(([value, optionText]) => el("option", {text: optionText, attrs: {value}})))
  ]);
}

function metric(labelText, value, tone = "info") { return card([el("p", {className: "text-sm text-on-surface-variant", text: labelText}), el("p", {className: `mt-2 text-2xl font-bold ${tone === "warning" ? "text-amber-700" : "text-primary"}`, text: value})]); }
function summaryMetrics(sales, returns) { return el("div", {className: "grid gap-4 sm:grid-cols-3 xl:grid-cols-1"}, [metric("Ventas", sales.length), metric("Devoluciones", returns.length), metric("Saldo pendiente total", money(sales.reduce((sum, sale) => sum + Number(sale.amountDue ?? 0), 0)), "warning")]); }
function productCell(product, productId) { return product ? el("div", {}, [el("b", {text: product.productName}), el("p", {className: "font-mono text-xs text-on-surface-variant", text: product.sku})]) : el("span", {className: "font-mono", text: productId}); }
function label(value) { return String(value ?? "—").replaceAll("_", " ").toLowerCase().replace(/^./, char => char.toUpperCase()); }
function showMsg(node, message, error) { node.className = `rounded-lg p-3 ${error ? "bg-error-container text-error" : "bg-secondary-container text-secondary"}`; node.textContent = message; }
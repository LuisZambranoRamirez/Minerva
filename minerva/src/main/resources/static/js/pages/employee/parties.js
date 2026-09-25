import {request} from "../../core/api.js";
import {el, busy} from "../../core/dom.js";
import {dateTime} from "../../core/format.js";
import {employeeShell} from "../../ui/shells.js";
import {button, card, errorState, field, heading, loadingState, table} from "../../ui/components.js";
import {toast, fieldErrors} from "../../ui/feedback.js";

export function renderParties({session}) {
  const root = el("div", {className: "mx-auto grid max-w-7xl gap-6"}, [heading("Clientes y proveedores", "Gestión operativa según permisos reales del backend."), loadingState("Cargando terceros…")]);
  employeeShell(session, root);
  load();

  async function load() {
    try {
      const canCustomers = ["ADMIN", "VENDEDOR"].includes(session.role);
      const canSuppliers = ["ADMIN", "ALMACENISTA"].includes(session.role);
      const [customers, suppliers] = await Promise.all([
        canCustomers ? request("/api/v1/customers", {token: session.accessToken}) : Promise.resolve([]),
        canSuppliers ? request("/api/v1/suppliers", {token: session.accessToken}) : Promise.resolve([])
      ]);
      root.replaceChildren(
        heading("Clientes y proveedores", "Altas y teléfonos/RUC se envían a endpoints reales; no hay exportación ni paginación simulada."),
        canCustomers ? customerSection(customers) : null,
        canSuppliers ? supplierSection(suppliers) : null
      );
    } catch (error) { root.replaceChildren(errorState(error.message, load)); }
  }

  function customerSection(customers) {
    return el("div", {className: "grid gap-5 xl:grid-cols-[420px_1fr]"}, [
      card([el("h2", {className: "mb-4 text-xl font-bold text-primary", text: "Registrar cliente"}), customerForm()]),
      card([el("h2", {className: "mb-4 text-xl font-bold text-primary", text: "Clientes"}), table([
        {label: "Cliente", render: customer => el("div", {}, [el("b", {text: customer.fullName}), el("p", {className: "font-mono text-xs text-on-surface-variant", text: customer.customerId})])},
        {label: "Teléfono", render: customer => phoneForm("customer", customer.customerId, customer.phoneNumber)},
        {label: "Registro", render: customer => el("span", {text: dateTime(customer.registrationDate)})}
      ], customers, {empty: "No hay clientes registrados."})])
    ]);
  }

  function supplierSection(suppliers) {
    return el("div", {className: "grid gap-5 xl:grid-cols-[420px_1fr]"}, [
      card([el("h2", {className: "mb-4 text-xl font-bold text-primary", text: "Registrar proveedor"}), supplierForm()]),
      card([el("h2", {className: "mb-4 text-xl font-bold text-primary", text: "Proveedores"}), table([
        {label: "Proveedor", render: supplier => el("div", {}, [el("b", {text: supplier.supplierName}), el("p", {className: "font-mono text-xs text-on-surface-variant", text: supplier.supplierId})])},
        {label: "RUC", render: supplier => rucForm(supplier.supplierId, supplier.ruc)},
        {label: "Teléfono", render: supplier => phoneForm("supplier", supplier.supplierId, supplier.phoneNumber)}
      ], suppliers, {empty: "No hay proveedores registrados."})])
    ]);
  }

  function customerForm() {
    const form = el("form", {className: "grid gap-4", attrs: {novalidate: true}});
    const msg = el("p", {className: "hidden rounded-lg p-3", attrs: {role: "status"}});
    const submit = button("Registrar cliente", {type: "submit"});
    form.append(field({name: "fullName", label: "Nombre completo"}), field({name: "phoneNumber", label: "Teléfono", required: false}), msg, submit);
    form.addEventListener("submit", async event => {
      event.preventDefault(); await submitJson(form, submit, msg, "/api/v1/customers", {fullName: value(form, "fullName"), phoneNumber: value(form, "phoneNumber") || null}, "Cliente registrado.");
    });
    return form;
  }

  function supplierForm() {
    const form = el("form", {className: "grid gap-4", attrs: {novalidate: true}});
    const msg = el("p", {className: "hidden rounded-lg p-3", attrs: {role: "status"}});
    const submit = button("Registrar proveedor", {type: "submit"});
    form.append(field({name: "supplierName", label: "Nombre del proveedor"}), field({name: "ruc", label: "RUC", required: false}), field({name: "phoneNumber", label: "Teléfono", required: false}), msg, submit);
    form.addEventListener("submit", async event => {
      event.preventDefault(); await submitJson(form, submit, msg, "/api/v1/suppliers", {supplierName: value(form, "supplierName"), ruc: value(form, "ruc") || null, phoneNumber: value(form, "phoneNumber") || null}, "Proveedor registrado.");
    });
    return form;
  }

  function phoneForm(type, id, current) {
    const form = inlineForm(current, "Teléfono", async input => {
      const path = type === "customer" ? `/api/v1/customers/${encodeURIComponent(id)}/phone-number` : `/api/v1/suppliers/${encodeURIComponent(id)}/phone-number`;
      const body = type === "customer" ? {newPhoneNumber: input.value.trim()} : {phoneNumber: input.value.trim()};
      await request(path, {method: "PATCH", token: session.accessToken, body});
    });
    return form;
  }

  function rucForm(id, current) {
    return inlineForm(current, "RUC", async input => {
      await request(`/api/v1/suppliers/${encodeURIComponent(id)}/ruc`, {method: "PATCH", token: session.accessToken, body: {ruc: input.value.trim()}});
    });
  }

  function inlineForm(current, label, action) {
    const input = el("input", {className: "field min-w-36", attrs: {value: current ?? "", required: true, "aria-label": label}});
    const submit = button("Guardar", {type: "submit", tone: "ghost"});
    const form = el("form", {className: "flex flex-col gap-2 sm:flex-row"}, [input, submit]);
    form.addEventListener("submit", async event => {
      event.preventDefault(); if (!form.checkValidity()) return form.reportValidity(); busy(submit, true);
      try { await action(input); toast("Dato actualizado."); await load(); } catch (error) { toast(error.message, {tone: "error"}); } finally { busy(submit, false); }
    });
    return form;
  }

  async function submitJson(form, submit, msg, path, body, success) {
    msg.classList.add("hidden"); fieldErrors(form); if (!form.checkValidity()) return form.reportValidity(); busy(submit, true);
    try { await request(path, {method: "POST", token: session.accessToken, body}); toast(success); await load(); }
    catch (error) { fieldErrors(form, error.fieldErrors); msg.className = "rounded-lg bg-error-container p-3 text-error"; msg.textContent = error.message; }
    finally { busy(submit, false); }
  }
}

function value(form, name) { return form.elements.namedItem(name).value.trim(); }
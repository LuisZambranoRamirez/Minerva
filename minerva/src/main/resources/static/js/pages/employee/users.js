import {request} from "../../core/api.js";
import {el, busy} from "../../core/dom.js";
import {dateTime} from "../../core/format.js";
import {employeeShell} from "../../ui/shells.js";
import {badge, button, card, errorState, field, heading, loadingState, table} from "../../ui/components.js";
import {fieldErrors, toast, confirmDialog} from "../../ui/feedback.js";

const roles = ["ADMIN", "VENDEDOR", "ALMACENISTA"];
const statuses = ["PENDING_APPROVAL", "APPROVED", "REJECTED"];

export function renderUsers({session, query}) {
  const status = query.get("status") ?? "PENDING_APPROVAL";
  const root = el("div", {className: "mx-auto grid max-w-7xl gap-6"}, [heading("Usuarios y aprobaciones", "Alta de usuarios internos y revisión de cuentas CLIENTE."), loadingState("Cargando cuentas…")]);
  employeeShell(session, root);
  load(status);

  async function load(selectedStatus) {
    try {
      const accounts = await request("/api/v1/admin/customer-accounts", {token: session.accessToken, query: {status: selectedStatus}});
      root.replaceChildren(
        heading("Usuarios y aprobaciones", "Solo ADMIN. Los usuarios CLIENTE se habilitan por aprobación; no se crean manualmente desde este formulario.", statusFilter(selectedStatus)),
        el("div", {className: "grid gap-5 xl:grid-cols-[420px_1fr]"}, [
          card([el("h2", {className: "mb-4 text-xl font-bold text-primary", text: "Registrar usuario interno"}), userForm()]),
          card([el("h2", {className: "mb-4 text-xl font-bold text-primary", text: "Cuentas CLIENTE"}), accountsTable(accounts, selectedStatus)])
        ])
      );
    } catch (error) { root.replaceChildren(errorState(error.message, () => load(selectedStatus))); }
  }

  function userForm() {
    const form = el("form", {className: "grid gap-4", attrs: {novalidate: true}});
    const role = el("label", {className: "grid gap-1.5 text-sm font-semibold"}, [el("span", {text: "Rol"}), el("select", {className: "field", attrs: {name: "role", required: true}}, roles.map(value => el("option", {text: value, attrs: {value}})))]);
    const msg = el("p", {className: "hidden rounded-lg p-3", attrs: {role: "status"}});
    const submit = button("Registrar usuario", {type: "submit"});
    [
      field({name: "dni", label: "DNI", pattern: "[0-9]{8}", maxLength: 8}),
      field({name: "names", label: "Nombres"}),
      field({name: "lastNames", label: "Apellidos"}),
      field({name: "phoneNumber", label: "Teléfono"}),
      field({name: "email", label: "Correo", type: "email"}),
      field({name: "username", label: "Usuario"}),
      field({name: "password", label: "Contraseña", type: "password", minLength: 8}),
      role,
      msg,
      submit
    ].forEach(node => form.append(node));
    form.addEventListener("submit", async event => {
      event.preventDefault(); msg.classList.add("hidden"); fieldErrors(form); if (!form.checkValidity()) return form.reportValidity(); busy(submit, true);
      try {
        const body = Object.fromEntries(["dni", "names", "lastNames", "phoneNumber", "email", "username", "password", "role"].map(name => [name, form.elements.namedItem(name).value.trim()]));
        await request("/api/v1/users", {method: "POST", token: session.accessToken, body});
        form.reset(); toast("Usuario registrado.");
      } catch (error) {
        fieldErrors(form, error.fieldErrors); msg.className = "rounded-lg bg-error-container p-3 text-error"; msg.textContent = error.message;
      } finally { busy(submit, false); }
    });
    return form;
  }

  function statusFilter(selectedStatus) {
    const select = el("select", {className: "field min-w-56", attrs: {"aria-label": "Filtrar cuentas por estado"}}, statuses.map(value => el("option", {text: label(value), attrs: {value, selected: value === selectedStatus}})));
    const apply = button("Aplicar", {tone: "ghost", onClick: () => { location.hash = `#/employee/users?status=${encodeURIComponent(select.value)}`; }});
    return el("div", {className: "flex flex-col gap-3 sm:flex-row"}, [select, apply]);
  }

  function accountsTable(accounts, selectedStatus) {
    return table([
      {label: "Cuenta", render: account => el("div", {}, [el("b", {text: account.businessName ?? account.legalName ?? account.username}), el("p", {className: "font-mono text-xs text-on-surface-variant", text: account.username})])},
      {label: "RUC", key: "ruc"},
      {label: "Contacto", render: account => el("div", {}, [el("span", {text: account.contactName ?? "—"}), el("p", {className: "text-xs text-on-surface-variant", text: account.contactEmail ?? account.contactPhone ?? "—"})])},
      {label: "Estado", render: account => badge(label(account.approvalStatus), account.approvalStatus === "APPROVED" ? "success" : account.approvalStatus === "REJECTED" ? "error" : "warning")},
      {label: "Registro", render: account => el("span", {text: dateTime(account.registrationDate)})},
      {label: "Acciones", render: account => selectedStatus === "PENDING_APPROVAL" ? actions(account.username) : auditCell(account)}
    ], accounts, {empty: "No hay cuentas para el estado seleccionado."});
  }

  function actions(username) {
    return el("div", {className: "flex flex-wrap gap-2"}, [
      actionButton("Aprobar", async () => request(`/api/v1/admin/customer-accounts/${encodeURIComponent(username)}/approve`, {method: "POST", token: session.accessToken}), false),
      actionButton("Rechazar", async () => reject(username), true)
    ]);
  }

  function actionButton(text, action, danger) {
    const trigger = button(text, {tone: danger ? "danger" : "primary"});
    trigger.addEventListener("click", async () => { busy(trigger, true); try { const changed = await action(); if (changed === false) return; toast("Cuenta actualizada."); await load("PENDING_APPROVAL"); } catch (error) { toast(error.message, {tone: "error"}); } finally { busy(trigger, false); } });
    return trigger;
  }

  async function reject(username) {
    const reason = el("textarea", {className: "field", attrs: {rows: 4, maxlength: 255, required: true, placeholder: "Motivo visible para auditoría"}});
    const accepted = await confirmDialog({title: "Rechazar cuenta CLIENTE", message: "El motivo es obligatorio y se enviará al backend.", confirmLabel: "Rechazar", danger: true, content: reason});
    if (!accepted) return false;
    if (!reason.value.trim()) { toast("Ingresá un motivo de rechazo.", {tone: "error"}); return false; }
    await request(`/api/v1/admin/customer-accounts/${encodeURIComponent(username)}/reject`, {method: "POST", token: session.accessToken, body: {reason: reason.value.trim()}});
  }

  function auditCell(account) {
    const who = account.approvedBy ?? account.rejectedBy ?? "—";
    const when = account.approvedDate ?? account.rejectedDate;
    return el("div", {}, [el("span", {text: who}), el("p", {className: "text-xs text-on-surface-variant", text: dateTime(when)}), account.rejectionReason ? el("p", {className: "text-xs text-error", text: account.rejectionReason}) : null]);
  }
}

function label(value) { return String(value ?? "—").replaceAll("_", " ").toLowerCase().replace(/^./, char => char.toUpperCase()); }
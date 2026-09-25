import {el, icon, mount} from "../core/dom.js";
import {logout} from "../core/auth.js";

const employee = [
  ["#/employee/dashboard", "dashboard", "Dashboard", ["ADMIN", "VENDEDOR", "ALMACENISTA"]],
  ["#/employee/inventory", "inventory_2", "Productos e inventario", ["ADMIN", "VENDEDOR", "ALMACENISTA"]],
  ["#/employee/orders", "shopping_cart_checkout", "Pedidos", ["ADMIN", "VENDEDOR", "ALMACENISTA"]],
  ["#/employee/sales", "point_of_sale", "Ventas", ["ADMIN", "VENDEDOR"]],
  ["#/employee/parties", "groups", "Clientes y proveedores", ["ADMIN", "VENDEDOR", "ALMACENISTA"]],
  ["#/employee/users", "admin_panel_settings", "Usuarios", ["ADMIN"]]
];
const customer = [
  ["#/customer/catalog", "storefront", "Catálogo"],
  ["#/customer/cart", "shopping_cart", "Carrito"],
  ["#/customer/orders", "package_2", "Mis pedidos"]
];

export const publicShell = mount;

function shell(session, content, links) {
  const backdrop = el("button", {className: "drawer-bg hidden md:hidden", attrs: {type: "button", "aria-label": "Cerrar menú"}});
  const aside = el("aside", {className: "drawer fixed inset-y-0 left-0 z-50 flex w-64 -translate-x-full flex-col bg-surface-low shadow-xl md:translate-x-0", attrs: {id: "app-drawer", "aria-label": "Navegación principal"}}, [
    el("div", {className: "flex h-16 items-center gap-2 bg-surface-container px-5"}, [icon("local_shipping"), el("b", {text: "GRUPO JIMÉNEZ"})]),
    el("nav", {className: "grid gap-1 p-3"}, links.filter(item => !item[3] || item[3].includes(session.role)).map(item => el("a", {className: `flex items-center gap-3 rounded-lg px-4 py-3 ${location.hash.startsWith(item[0]) ? "bg-primary text-white" : "hover:bg-surface-container"}`, attrs: {href: item[0]}}, [icon(item[1]), el("span", {text: item[2]})]))),
    el("div", {className: "mt-auto border-t border-surface-container p-4"}, [el("b", {text: session.username}), el("p", {className: "text-xs text-on-surface-variant", text: session.role})])
  ]);
  let menu;
  const setDrawer = open => {
    aside.classList.toggle("-translate-x-full", !open);
    backdrop.classList.toggle("hidden", !open);
    menu.setAttribute("aria-expanded", String(open));
    if (open) aside.querySelector("a")?.focus();
  };
  menu = el("button", {className: "rounded-lg p-2 md:hidden", attrs: {type: "button", "aria-label": "Abrir menú", "aria-controls": "app-drawer", "aria-expanded": "false"}, on: {click: () => setDrawer(true)}}, icon("menu"));
  backdrop.addEventListener("click", () => setDrawer(false));
  aside.addEventListener("click", event => { if (event.target.closest("a")) setDrawer(false); });
  const header = el("header", {className: "fixed left-0 right-0 top-0 z-40 flex h-16 items-center justify-between bg-surface px-4 shadow md:left-64 md:px-6"}, [
    el("div", {className: "flex items-center gap-3"}, [menu, el("b", {text: "MINERVA"})]),
    el("button", {className: "rounded-lg px-3 py-2 font-semibold text-error hover:bg-error-container", text: "Cerrar sesión", attrs: {type: "button"}, on: {click: logout}})
  ]);
  document.addEventListener("keydown", event => { if (event.key === "Escape") setDrawer(false); }, {once: true});
  mount(el("div", {}, [backdrop, aside, header, el("main", {className: "min-h-screen px-4 pb-10 pt-24 md:ml-64 md:px-8", attrs: {id: "main", tabindex: "-1"}}, content)]));
}

export const employeeShell = (session, content) => shell(session, content, employee);
export const customerShell = (session, content) => shell(session, content, customer);

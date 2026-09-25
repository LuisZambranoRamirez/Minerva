import {el} from "./core/dom.js";
import {getSession, homeForRole} from "./core/auth.js";
import {register, setNotFound, start, navigate} from "./core/router.js";
import {renderLogin} from "./pages/public/login.js";
import {renderRegister} from "./pages/public/register.js";
import {renderCatalog} from "./pages/customer/catalog.js";
import {renderCart} from "./pages/customer/cart.js";
import {renderOrders} from "./pages/customer/orders.js";
import {renderOrderDetail} from "./pages/customer/order-detail.js";
import {renderDashboard} from "./pages/employee/dashboard.js";
import {renderInventory} from "./pages/employee/inventory.js";
import {renderEmployeeOrders} from "./pages/employee/orders.js";
import {renderEmployeeOrderDetail} from "./pages/employee/order-detail.js";
import {renderSales} from "./pages/employee/sales.js";
import {renderParties} from "./pages/employee/parties.js";
import {renderUsers} from "./pages/employee/users.js";
import {publicShell} from "./ui/shells.js";

register({path: "/", title: "Inicio", render: () => {
  const session = getSession();
  navigate(session ? homeForRole(session.role) : "/login", true);
}});
register({path: "/login", title: "Ingresar", publicOnly: true, render: renderLogin});
register({path: "/register", title: "Registro B2B", publicOnly: true, render: renderRegister});
register({path: "/customer/catalog", title: "Catálogo", auth: true, roles: ["CLIENTE"], render: renderCatalog});
register({path: "/customer/cart", title: "Carrito", auth: true, roles: ["CLIENTE"], render: renderCart});
register({path: "/customer/orders", title: "Mis pedidos", auth: true, roles: ["CLIENTE"], render: renderOrders});
register({path: "/customer/orders/:orderId", title: "Detalle del pedido", auth: true, roles: ["CLIENTE"], render: renderOrderDetail});
register({path: "/employee/dashboard", title: "Dashboard", auth: true, roles: ["ADMIN", "VENDEDOR", "ALMACENISTA"], render: renderDashboard});
register({path: "/employee/inventory", title: "Productos e inventario", auth: true, roles: ["ADMIN", "VENDEDOR", "ALMACENISTA"], render: renderInventory});
register({path: "/employee/orders", title: "Pedidos", auth: true, roles: ["ADMIN", "VENDEDOR", "ALMACENISTA"], render: renderEmployeeOrders});
register({path: "/employee/orders/:orderId", title: "Detalle de pedido", auth: true, roles: ["ADMIN", "VENDEDOR", "ALMACENISTA"], render: renderEmployeeOrderDetail});
register({path: "/employee/sales", title: "Ventas", auth: true, roles: ["ADMIN", "VENDEDOR"], render: renderSales});
register({path: "/employee/parties", title: "Clientes y proveedores", auth: true, roles: ["ADMIN", "VENDEDOR", "ALMACENISTA"], render: renderParties});
register({path: "/employee/users", title: "Usuarios", auth: true, roles: ["ADMIN"], render: renderUsers});

setNotFound(() => publicShell(el("main", {className: "grid min-h-screen place-items-center", attrs: {id: "main"}},
  el("a", {className: "rounded-lg bg-primary px-5 py-3 text-white", text: "Volver a Minerva", attrs: {href: "#/"}})
)));

start();

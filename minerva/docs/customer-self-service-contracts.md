# Customer Self-Service API Contracts

Backend contract for integrating the supplied customer HTML. Treat browser state and `localStorage` cart data as hostile input: the backend derives customer ownership from the JWT and prices from the product domain.

## Authentication and lifecycle

### Register B2B customer
`POST /api/v1/auth/customer-register` is public.

Request body:
```json
{
  "businessName": "Acme Distribuciones",
  "legalName": "Acme Distribuciones SAC",
  "ruc": "20601234567",
  "address": "Av. Principal 123",
  "defaultDeliveryAddress": "Almacén Norte 456",
  "defaultDeliveryContact": "Ana Compras",
  "defaultDeliveryPhone": "987654321",
  "contactDni": "12345678",
  "contactNames": "Ana",
  "contactLastNames": "Compras",
  "contactPhone": "987654321",
  "contactEmail": "ana.compras@example.com",
  "username": "cliente01",
  "password": "password123"
}
```

Response `201`:
```json
{
  "username": "cliente01",
  "customerId": "uuid",
  "approvalStatus": "PENDING_APPROVAL",
  "message": "Solicitud registrada. Tu cuenta queda pendiente de aprobación administrativa."
}
```

Duplicate username/DNI/email/phone/RUC/customer ownership returns a generic `409`/conflict-style error from the API layer; the UI must not rely on knowing which field already exists. The request MUST NOT include role, customerId, approval status, unit prices, stock or costs.

### Login
`POST /api/v1/auth/login` accepts `username` and `password`. Pending or rejected CLIENTE accounts receive the same safe `401` response as bad credentials. Approved CLIENTE accounts receive a JWT whose role is `CLIENTE`.

## Admin customer account approval
All endpoints require ADMIN.

- `GET /api/v1/admin/customer-accounts?status=PENDING_APPROVAL|APPROVED|REJECTED`
- `POST /api/v1/admin/customer-accounts/{username}/approve`
- `POST /api/v1/admin/customer-accounts/{username}/reject` with `{ "reason": "..." }`

Approval/rejection stores audit metadata (`approvedBy`, `approvedDate`, `rejectedBy`, `rejectedDate`, `rejectionReason`). Rejected/approved accounts cannot be approved/rejected again unless a future product decision introduces a re-open flow.

## Customer catalog
All endpoints under `/api/v1/customer/**` require an approved CLIENTE JWT.

`GET /api/v1/customer/catalog`

Item shape:
```json
{
  "productId": "uuid",
  "sku": "SKU-0001",
  "productName": "Detergente",
  "category": "LIMPIEZA",
  "saleType": "UNIDAD",
  "price": 12.50,
  "availability": "DISPONIBLE"
}
```

The catalog intentionally excludes exact stock, purchase cost, supplier, reorder level and inventory internals. `price` is server-calculated.

## Checkout
`POST /api/v1/customer/orders`

Request body:
```json
{
  "items": [
    { "productId": "uuid", "quantity": 2 }
  ],
  "deliveryAddress": "Almacén Norte 456",
  "deliveryContact": "Ana Compras",
  "deliveryPhone": "987654321"
}
```

Only `productId`, `quantity`, and delivery data are accepted by the DTO. The HTML may keep a cart in `localStorage`, but must send it as untrusted suggestions only. The backend ignores/rejects client-side owner/price concepts because `customerId` and `unitPrice` are absent from the checkout DTO; the service derives customer from the JWT-linked account and recalculates price from product data.

Delivery fields fallback to the Customer defaults. If neither request nor defaults provide final non-blank address/contact/phone, checkout returns validation error and creates no order.

Invalid unknown, unavailable/out-of-stock, insufficient stock, invalid decimal-for-unit, non-positive quantity, or invalid-price products reject the entire order and create no partial order.

## Own orders
- `GET /api/v1/customer/orders?status=&from=&to=` where dates use ISO-8601, e.g. `2026-09-23T10:30:00`.
- `GET /api/v1/customer/orders/{orderId}`
- `GET /api/v1/customer/orders/{orderId}/transitions`
- `POST /api/v1/customer/orders/{orderId}/cancel` with `{ "reason": "Cliente se equivocó de producto" }`

Order ownership is enforced below the controller with customer-owned repository predicates. Requests for another customer's order return not-found/forbidden style responses without ownership details. Cancellation requires a non-empty reason, uses the existing lifecycle rules, locks the owned order row for update, and persists the reason on the cancellation transition.

## Postponed product choices
The current backend does not implement scheduled delivery windows, payment terms/credit, or persistent shopping carts. Those need explicit product/architecture decisions before backend implementation.
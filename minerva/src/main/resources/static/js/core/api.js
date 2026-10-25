import {downloadFile} from "./download.mjs";

export class ApiError extends Error {
  constructor(message, {status = 0, fieldErrors = {}} = {}) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors ?? {};
  }
}

let unauthorized = () => {};
export const onUnauthorized = handler => unauthorized = handler;
const SESSION_KEY = "minerva.session";
const PUBLIC_API_PATHS = new Set(["/api/v1/auth/login", "/api/v1/auth/customer-register"]);

function storedSession() {
  try { return JSON.parse(sessionStorage.getItem(SESSION_KEY)); } catch { return null; }
}

export async function request(path, {method = "GET", body, query, token} = {}) {
  const url = new URL(path, location.origin);
  Object.entries(query ?? {}).forEach(([key, value]) => {
    if (value != null && value !== "") url.searchParams.set(key, value);
  });
  const headers = {Accept: "application/json"};
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (!PUBLIC_API_PATHS.has(url.pathname)) {
    const session = storedSession();
    const accessToken = token ?? session?.accessToken;
    if (accessToken) headers.Authorization = `${session?.tokenType || "Bearer"} ${accessToken}`;
  }
  let response;
  try {
    response = await fetch(url, {method, headers, body: body === undefined ? undefined : JSON.stringify(body)});
  } catch {
    throw new ApiError("No se pudo conectar con el servidor.");
  }
  const data = response.status === 204 ? null : await response.json().catch(() => null);
  if (response.ok) return data;
  const error = new ApiError(data?.message || (response.status === 403
    ? "No tenés permiso para esta acción."
    : response.status === 409
      ? "La operación entra en conflicto con el estado actual."
      : "No se pudo completar la solicitud."), {status: response.status, fieldErrors: data?.fieldErrors});
  if (response.status === 401) unauthorized(error);
  throw error;
}

export function download(path, {query, token, fallbackFilename} = {}) {
  return downloadFile(path, {
    query,
    token,
    session: storedSession(),
    fallbackFilename,
    origin: location.origin,
    fetchImpl: fetch,
    documentRef: document,
    urlApi: URL,
    onUnauthorized: unauthorized,
    makeError: (message, options) => new ApiError(message, options)
  });
}

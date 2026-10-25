const SAFE_REPORT_FILENAME = /^movimientos-inventario(?:-\d{8}-\d{6})?\.(?:xlsx|csv)$/;

export function safeDownloadFilename(contentDisposition, fallbackFilename) {
  const candidates = [];
  if (contentDisposition) {
    const encoded = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
    const regular = contentDisposition.match(/filename\s*=\s*(?:"([^"]+)"|([^;]+))/i);
    if (encoded) {
      try { candidates.push(decodeURIComponent(encoded.trim())); } catch { /* use fallback */ }
    }
    if (regular) candidates.push((regular[1] ?? regular[2]).trim());
  }
  candidates.push(fallbackFilename);
  return candidates.find(name => SAFE_REPORT_FILENAME.test(name ?? "")) ?? "movimientos-inventario.csv";
}

export function reportErrorMessage(status, serverMessage) {
  if (serverMessage) return serverMessage;
  return ({
    400: "Revisá los filtros y el rango de fechas.",
    403: "No tenés permiso para exportar el inventario.",
    422: "El reporte es demasiado grande. Reducí el rango o aplicá más filtros.",
    500: "No se pudo generar el reporte. Intentá nuevamente más tarde."
  })[status] ?? "No se pudo descargar el reporte.";
}

export async function downloadFile(path, {
  query,
  token,
  session,
  fallbackFilename,
  origin,
  fetchImpl,
  documentRef,
  urlApi,
  onUnauthorized,
  makeError
}) {
  const url = new URL(path, origin);
  Object.entries(query ?? {}).forEach(([key, value]) => {
    if (value != null && value !== "") url.searchParams.set(key, value);
  });
  const headers = {Accept: "application/octet-stream"};
  const accessToken = token ?? session?.accessToken;
  if (accessToken) headers.Authorization = `${session?.tokenType || "Bearer"} ${accessToken}`;

  let response;
  try {
    response = await fetchImpl(url, {method: "GET", headers});
  } catch {
    throw makeError("No se pudo conectar con el servidor.", {status: 0});
  }

  if (!response.ok) {
    const payload = await response.json().catch(() => null);
    const error = makeError(reportErrorMessage(response.status, payload?.message), {
      status: response.status,
      fieldErrors: payload?.fieldErrors
    });
    if (response.status === 401) onUnauthorized(error);
    throw error;
  }

  const blob = await response.blob();
  const filename = safeDownloadFilename(response.headers.get("Content-Disposition"), fallbackFilename);
  const objectUrl = urlApi.createObjectURL(blob);
  const anchor = documentRef.createElement("a");
  try {
    anchor.href = objectUrl;
    anchor.download = filename;
    anchor.hidden = true;
    documentRef.body.append(anchor);
    anchor.click();
  } finally {
    anchor.remove();
    urlApi.revokeObjectURL(objectUrl);
  }
  return {filename, blob};
}

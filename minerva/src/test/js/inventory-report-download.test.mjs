import test from "node:test";
import assert from "node:assert/strict";
import {downloadFile, reportErrorMessage, safeDownloadFilename} from "../../main/resources/static/js/core/download.mjs";

class TestError extends Error {
  constructor(message, options = {}) {
    super(message);
    Object.assign(this, options);
  }
}

function browserHarness() {
  const state = {clicked: 0, appended: 0, removed: 0, revoked: [], requests: []};
  const anchor = {
    click() { state.clicked++; },
    remove() { state.removed++; }
  };
  return {
    state,
    documentRef: {
      body: {append(node) { assert.equal(node, anchor); state.appended++; }},
      createElement(tag) { assert.equal(tag, "a"); return anchor; }
    },
    urlApi: {
      createObjectURL(blob) { assert.ok(blob instanceof Blob); return "blob:minerva-report"; },
      revokeObjectURL(url) { state.revoked.push(url); }
    },
    anchor
  };
}

test("downloads an authenticated Blob, preserves filters, uses safe filename and revokes URL", async () => {
  const browser = browserHarness();
  const blob = new Blob(["csv"]);
  const fetchImpl = async (url, options) => {
    browser.state.requests.push({url, options});
    return {
      ok: true,
      status: 200,
      headers: {get: name => name === "Content-Disposition"
        ? 'attachment; filename="movimientos-inventario-20261024-223000.csv"' : null},
      blob: async () => blob
    };
  };

  const result = await downloadFile("/api/v1/products/inventory-movements/export", {
    query: {format: "csv", productId: "product-1", type: "", from: "2026-10-24T00:00"},
    session: {accessToken: "secret-token", tokenType: "Bearer"},
    fallbackFilename: "movimientos-inventario.csv",
    origin: "http://localhost:8080",
    fetchImpl,
    documentRef: browser.documentRef,
    urlApi: browser.urlApi,
    onUnauthorized: () => assert.fail("401 callback must not run"),
    makeError: (message, options) => new TestError(message, options)
  });

  const request = browser.state.requests[0];
  assert.equal(request.url.searchParams.get("format"), "csv");
  assert.equal(request.url.searchParams.get("productId"), "product-1");
  assert.equal(request.url.searchParams.get("from"), "2026-10-24T00:00");
  assert.equal(request.url.searchParams.has("type"), false);
  assert.equal(request.url.searchParams.has("token"), false);
  assert.equal(request.options.headers.Authorization, "Bearer secret-token");
  assert.equal(result.filename, "movimientos-inventario-20261024-223000.csv");
  assert.equal(browser.anchor.download, result.filename);
  assert.equal(browser.state.clicked, 1);
  assert.equal(browser.state.removed, 1);
  assert.deepEqual(browser.state.revoked, ["blob:minerva-report"]);
});

test("rejects unsafe Content-Disposition names and uses the format fallback", () => {
  assert.equal(safeDownloadFilename('attachment; filename="../../evil.csv"', "movimientos-inventario.xlsx"),
    "movimientos-inventario.xlsx");
  assert.equal(safeDownloadFilename("attachment; filename*=UTF-8''movimientos-inventario-20261024-223000.xlsx",
    "movimientos-inventario.xlsx"), "movimientos-inventario-20261024-223000.xlsx");
});

test("parses JSON errors and triggers global logout only for 401", async () => {
  for (const status of [400, 401, 403, 422, 500]) {
    let unauthorized = 0;
    const browser = browserHarness();
    await assert.rejects(() => downloadFile("/report", {
      query: {format: "csv"},
      session: {accessToken: "token", tokenType: "Bearer"},
      fallbackFilename: "movimientos-inventario.csv",
      origin: "http://localhost:8080",
      fetchImpl: async () => ({
        ok: false,
        status,
        json: async () => ({message: `server-${status}`, fieldErrors: {format: "invalid"}})
      }),
      documentRef: browser.documentRef,
      urlApi: browser.urlApi,
      onUnauthorized: () => unauthorized++,
      makeError: (message, options) => new TestError(message, options)
    }), error => {
      assert.equal(error.status, status);
      assert.equal(error.message, `server-${status}`);
      assert.deepEqual(error.fieldErrors, {format: "invalid"});
      return true;
    });
    assert.equal(unauthorized, status === 401 ? 1 : 0);
    assert.equal(browser.state.clicked, 0);
  }
});

test("provides understandable fallback messages for report failures", () => {
  assert.match(reportErrorMessage(400), /filtros/i);
  assert.match(reportErrorMessage(403), /permiso/i);
  assert.match(reportErrorMessage(422), /grande/i);
  assert.match(reportErrorMessage(500), /generar/i);
});

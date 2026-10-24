import test from "node:test";
import assert from "node:assert/strict";
import {
  PRODUCT_PLACEHOLDER,
  installProductImageFallback,
  productImageAttributes,
  productQuantityStep
} from "../../main/resources/static/js/core/product-image.mjs";

test("uses the product name as alt and a local placeholder for a missing image", () => {
  assert.deepEqual(productImageAttributes({productName: "Arroz", imageUrl: null}), {
    src: PRODUCT_PLACEHOLDER,
    alt: "Arroz"
  });
  assert.deepEqual(productImageAttributes({productName: "Arroz", imageUrl: "/assets/products/arroz.webp"}), {
    src: "/assets/products/arroz.webp",
    alt: "Arroz"
  });
});

test("accepts only expected same-origin product asset paths", () => {
  assert.equal(productImageAttributes({productName: "Arroz", imageUrl: "/assets/products/arroz-extra.webp"}).src,
    "/assets/products/arroz-extra.webp");

  for (const imageUrl of [
    "https://evil.example/assets/products/arroz.webp",
    "//evil.example/arroz.webp",
    "/assets/products/../secret.webp",
    "/assets/products/arroz.png",
    "not a url"
  ]) {
    assert.equal(productImageAttributes({productName: "Arroz", imageUrl}).src, PRODUCT_PLACEHOLDER);
  }
});

test("customer quantity controls use the sale type precision", () => {
  assert.equal(productQuantityStep("UNIDAD"), "1");
  assert.equal(productQuantityStep("GRANEL"), "0.001");
});

test("fallback replaces a broken image once and cannot loop", () => {
  const listeners = [];
  const image = {
    src: "/assets/products/broken.webp",
    onerror: () => {},
    addEventListener(type, listener, options) {
      listeners.push({type, listener, options});
    }
  };

  assert.equal(installProductImageFallback(image), image);
  assert.equal(listeners.length, 1);
  assert.deepEqual({type: listeners[0].type, options: listeners[0].options}, {type: "error", options: {once: true}});
  listeners[0].listener();
  assert.equal(image.src, PRODUCT_PLACEHOLDER);
  assert.equal(image.onerror, null);
});

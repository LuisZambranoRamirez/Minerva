export const PRODUCT_PLACEHOLDER = "/assets/products/placeholder.webp";
const PRODUCT_IMAGE_URL = /^\/assets\/products\/[a-z0-9]+(?:-[a-z0-9]+)*\.webp$/;

export function productImageAttributes(product) {
  return {
    src: typeof product.imageUrl === "string" && PRODUCT_IMAGE_URL.test(product.imageUrl)
      ? product.imageUrl
      : PRODUCT_PLACEHOLDER,
    alt: product.productName
  };
}

export function installProductImageFallback(image) {
  image.addEventListener("error", () => {
    image.onerror = null;
    image.src = PRODUCT_PLACEHOLDER;
  }, {once: true});
  return image;
}

export function productQuantityStep(saleType) {
  return saleType === "GRANEL" ? "0.001" : "1";
}

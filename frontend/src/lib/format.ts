const priceFormat = new Intl.NumberFormat("ru-RU", { maximumFractionDigits: 0 });

/** 8900 → "8 900 ₸". Only KZT is used by the store. */
export function formatPrice(value: number): string {
  return `${priceFormat.format(value)} ₸`;
}

/** Whole-percent discount, or null when there is no real discount. */
export function discountPercent(price: number | null, promoPrice: number | null): number | null {
  if (price == null || promoPrice == null || promoPrice >= price || price <= 0) return null;
  return Math.max(1, Math.round((1 - promoPrice / price) * 100));
}

export function pluralizeProducts(count: number): string {
  const mod10 = count % 10;
  const mod100 = count % 100;
  if (mod10 === 1 && mod100 !== 11) return `${count} товар`;
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return `${count} товара`;
  return `${count} товаров`;
}

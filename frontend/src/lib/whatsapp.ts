import { formatPrice } from "@/lib/format";

// International format without "+", as wa.me expects (8 706 … → 7 706 …).
const WHATSAPP_PHONE = process.env.NEXT_PUBLIC_WHATSAPP_PHONE ?? "77066059713";
const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";

export function whatsappLink(text?: string): string {
  const base = `https://wa.me/${WHATSAPP_PHONE}`;
  return text ? `${base}?text=${encodeURIComponent(text)}` : base;
}

/** Opening WhatsApp is a lead, not an order: the manager confirms availability and delivery. */
export function productOrderMessage(product: {
  name: string;
  slug: string;
  price: number | null;
  promoPrice: number | null;
}): string {
  const price = product.promoPrice ?? product.price;
  return [
    "Здравствуйте! Хочу заказать:",
    `${product.name} — 1 шт.`,
    price != null ? `Цена: ${formatPrice(price)}` : null,
    `${SITE_URL}/product/${product.slug}`,
  ]
    .filter(Boolean)
    .join("\n");
}

export function productAvailabilityMessage(product: { name: string; slug: string }): string {
  return [
    "Здравствуйте! Подскажите, когда появится в наличии:",
    product.name,
    `${SITE_URL}/product/${product.slug}`,
  ].join("\n");
}

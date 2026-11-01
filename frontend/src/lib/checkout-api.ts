// Called from the browser: the backend allows the storefront origin via CORS.
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export type DeliveryMethod = "COURIER" | "PICKUP";

export type CheckoutItem = { slug: string; quantity: number };

export type QuoteProblem = {
  slug: string;
  name: string | null;
  reason: "NOT_AVAILABLE" | "INSUFFICIENT_STOCK";
  available: number;
};

export type Quote = {
  lines: { slug: string; name: string; imageUrl: string | null; quantity: number; unitPrice: number; lineTotal: number }[];
  problems: QuoteProblem[];
  itemsTotal: number;
  deliveryFee: number;
  total: number;
  currency: string;
};

export type PlacedOrder = {
  number: string;
  total: number;
  deliveryMethod: DeliveryMethod;
};

export type PlaceOrderInput = {
  idempotencyKey: string;
  name: string;
  phone: string;
  deliveryMethod: DeliveryMethod;
  address?: string;
  comment?: string;
  consent: boolean;
  items: CheckoutItem[];
};

export type PlaceOrderResult =
  | { ok: true; order: PlacedOrder }
  | { ok: false; kind: "validation"; message: string; fields: Record<string, string> }
  | { ok: false; kind: "stock"; problems: QuoteProblem[] }
  | { ok: false; kind: "rate-limit" | "network" | "server"; message: string };

async function postJson(path: string, body: unknown): Promise<Response> {
  return fetch(`${API_BASE_URL}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify(body),
  });
}

export async function fetchQuote(items: CheckoutItem[], deliveryMethod: DeliveryMethod): Promise<Quote> {
  const res = await postJson("/api/checkout/quote", { items, deliveryMethod });
  if (!res.ok) throw new Error(`Quote failed: ${res.status}`);
  return res.json();
}

export async function placeOrder(input: PlaceOrderInput): Promise<PlaceOrderResult> {
  let res: Response;
  try {
    res = await postJson("/api/orders", input);
  } catch {
    return { ok: false, kind: "network", message: "Нет соединения с сервером. Проверьте интернет и попробуйте снова." };
  }
  const body = await res.json().catch(() => ({}));
  if (res.ok) return { ok: true, order: body as PlacedOrder };
  if (res.status === 400) {
    const fields: Record<string, string> = {};
    for (const e of (body.errors ?? []) as { field: string; message: string }[]) fields[e.field] = e.message;
    return { ok: false, kind: "validation", message: body.detail ?? "Проверьте введённые данные", fields };
  }
  if (res.status === 409) return { ok: false, kind: "stock", problems: body.problems ?? [] };
  if (res.status === 429) {
    return { ok: false, kind: "rate-limit", message: "Слишком много попыток. Подождите несколько минут." };
  }
  return { ok: false, kind: "server", message: "Не удалось оформить заказ. Попробуйте ещё раз или напишите нам в WhatsApp." };
}

/** Mirrors the backend rule: Kazakhstan mobile numbers only. */
export function isValidKzPhone(input: string): boolean {
  let digits = input.replace(/\D/g, "");
  if (digits.length === 11 && (digits.startsWith("7") || digits.startsWith("8"))) digits = digits.slice(1);
  return digits.length === 10 && digits.startsWith("7");
}

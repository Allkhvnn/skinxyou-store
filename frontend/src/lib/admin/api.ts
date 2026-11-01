import "server-only";

import { notFound, redirect } from "next/navigation";

import type { Page } from "@/lib/catalog-api";
import type { AdminSession } from "@/lib/admin/session";

const API_BASE_URL = process.env.API_BASE_URL ?? "http://localhost:8080";

export const ORDER_STATUSES = ["NEW", "CONFIRMED", "SHIPPED", "READY_FOR_PICKUP", "COMPLETED", "CANCELLED"] as const;
export const PAYMENT_STATUSES = ["PENDING", "PAID", "CANCELLED"] as const;
export type OrderStatus = (typeof ORDER_STATUSES)[number];
export type PaymentStatus = (typeof PAYMENT_STATUSES)[number];

export type AdminOrderSummary = {
  number: string;
  createdAt: string;
  customerName: string;
  customerPhone: string;
  deliveryMethod: "COURIER" | "PICKUP";
  itemCount: number;
  total: number;
  currency: string;
  status: OrderStatus;
  paymentStatus: PaymentStatus;
};

export type AdminOrderDetails = Omit<AdminOrderSummary, "itemCount"> & {
  paymentMethod: string;
  deliveryAddress: string | null;
  comment: string | null;
  items: { slug: string; name: string; sku: string | null; quantity: number; unitPrice: number; lineTotal: number }[];
  itemsTotal: number;
  deliveryFee: number;
  allowedStatuses: OrderStatus[];
  allowedPaymentStatuses: PaymentStatus[];
  history: {
    type: "ORDER" | "PAYMENT";
    fromStatus: string | null;
    toStatus: string;
    changedBy: string;
    note: string | null;
    createdAt: string;
  }[];
  version: number;
};

export type MutationResult = { ok: true } | { ok: false; message: string };

/** Personal data: never cached, always fetched with the staff member's own token. */
async function call(session: AdminSession, path: string, init?: RequestInit): Promise<Response> {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    cache: "no-store",
    headers: { Accept: "application/json", "Content-Type": "application/json", Authorization: `Bearer ${session.token}` },
  });
  if (res.status === 401 || res.status === 403) redirect("/admin/login");
  return res;
}

export async function listOrders(
  session: AdminSession,
  filter: { status?: OrderStatus; paymentStatus?: PaymentStatus; q?: string; page: number },
): Promise<Page<AdminOrderSummary>> {
  const params = new URLSearchParams({ page: String(filter.page - 1), size: "20" });
  if (filter.status) params.set("status", filter.status);
  if (filter.paymentStatus) params.set("paymentStatus", filter.paymentStatus);
  if (filter.q) params.set("q", filter.q);
  const res = await call(session, `/api/admin/orders?${params}`);
  if (!res.ok) throw new Error(`Admin orders API responded ${res.status}`);
  return res.json();
}

export async function getOrder(session: AdminSession, number: string): Promise<AdminOrderDetails> {
  const res = await call(session, `/api/admin/orders/${encodeURIComponent(number)}`);
  if (res.status === 404) notFound();
  if (!res.ok) throw new Error(`Admin order API responded ${res.status}`);
  return res.json();
}

export async function changeOrderStatus(
  session: AdminSession,
  number: string,
  body: { status?: OrderStatus; paymentStatus?: PaymentStatus; note?: string; version: number },
): Promise<MutationResult> {
  const endpoint = body.paymentStatus ? "payment-status" : "status";
  const res = await call(session, `/api/admin/orders/${encodeURIComponent(number)}/${endpoint}`, {
    method: "POST",
    body: JSON.stringify(body),
  });
  if (res.ok) return { ok: true };
  const problem = await res.json().catch(() => ({}));
  return { ok: false, message: problem.detail ?? "Не удалось изменить статус. Попробуйте ещё раз." };
}

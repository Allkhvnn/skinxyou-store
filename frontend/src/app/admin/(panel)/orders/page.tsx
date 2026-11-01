import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";

import { OrderStatusBadge, PaymentStatusBadge } from "@/components/admin/status-badge";
import { Pagination } from "@/components/pagination";
import { listOrders, ORDER_STATUSES, PAYMENT_STATUSES, type OrderStatus, type PaymentStatus } from "@/lib/admin/api";
import { DELIVERY_LABELS, formatDateTime, formatPhone, ORDER_STATUS_LABELS } from "@/lib/admin/labels";
import { requireSession } from "@/lib/admin/session";
import { formatPrice } from "@/lib/format";
import { cn } from "@/lib/utils";

export const metadata: Metadata = { title: "Заказы" };

type Filter = { status?: OrderStatus; paymentStatus?: PaymentStatus; q?: string; page: number };

const TABS: { label: string; filter: Partial<Filter> }[] = [
  { label: "Все", filter: {} },
  { label: "Новые", filter: { status: "NEW" } },
  { label: "Ожидают оплаты", filter: { paymentStatus: "PENDING" } },
  ...(["CONFIRMED", "SHIPPED", "READY_FOR_PICKUP", "COMPLETED", "CANCELLED"] as const).map((status) => ({
    label: ORDER_STATUS_LABELS[status],
    filter: { status },
  })),
];

function href(filter: Partial<Filter>) {
  const params = new URLSearchParams();
  if (filter.status) params.set("status", filter.status);
  if (filter.paymentStatus) params.set("paymentStatus", filter.paymentStatus);
  if (filter.q) params.set("q", filter.q);
  if (filter.page && filter.page > 1) params.set("page", String(filter.page));
  const qs = params.toString();
  return qs ? `/admin/orders?${qs}` : "/admin/orders";
}

export default function OrdersPage(props: PageProps<"/admin/orders">) {
  return (
    <div className="flex flex-col gap-6">
      <h1 className="page-title">Заказы</h1>
      <Suspense fallback={<div className="h-96 animate-pulse bg-image" aria-hidden />}>
        <Orders searchParams={props.searchParams} />
      </Suspense>
    </div>
  );
}

async function Orders({ searchParams }: Pick<PageProps<"/admin/orders">, "searchParams">) {
  const session = await requireSession();
  const raw = await searchParams;
  const one = (v: string | string[] | undefined) => (Array.isArray(v) ? v[0] : v);
  const status = one(raw.status);
  const paymentStatus = one(raw.paymentStatus);
  const filter: Filter = {
    status: (ORDER_STATUSES as readonly string[]).includes(status ?? "") ? (status as OrderStatus) : undefined,
    paymentStatus: (PAYMENT_STATUSES as readonly string[]).includes(paymentStatus ?? "")
      ? (paymentStatus as PaymentStatus)
      : undefined,
    q: one(raw.q)?.trim().slice(0, 100) || undefined,
    page: Math.max(1, Math.floor(Number(one(raw.page)) || 1)),
  };
  const page = await listOrders(session, filter);

  return (
    <>
      <div className="flex flex-col gap-4">
        <nav aria-label="Фильтр" className="flex max-w-full gap-2 overflow-x-auto pb-1">
          {TABS.map((tab) => {
            const active = tab.filter.status === filter.status && tab.filter.paymentStatus === filter.paymentStatus;
            return (
              <Link key={tab.label} href={href({ ...tab.filter, q: filter.q })} className="category-pill"
                aria-current={active ? "page" : undefined}>
                {tab.label}
              </Link>
            );
          })}
        </nav>
        <form action="/admin/orders" method="get" className="flex max-w-xl gap-2">
          {filter.status && <input type="hidden" name="status" value={filter.status} />}
          {filter.paymentStatus && <input type="hidden" name="paymentStatus" value={filter.paymentStatus} />}
          <input name="q" type="search" defaultValue={filter.q} maxLength={100} className="field"
            placeholder="Номер заказа, телефон или имя" aria-label="Поиск заказов" />
          <button type="submit" className="outline-button min-h-11">Найти</button>
        </form>
      </div>

      {page.items.length === 0 ? (
        <p className="border border-primary/15 px-5 py-16 text-center text-muted-foreground">Заказов не найдено.</p>
      ) : (
        <div className="overflow-x-auto border border-primary/15">
          <table className="w-full min-w-[760px] text-sm">
            <thead className="bg-sand/60 text-left text-xs uppercase tracking-wider text-muted-foreground">
              <tr>
                <th className="px-4 py-3 font-semibold">Заказ</th>
                <th className="px-4 py-3 font-semibold">Клиент</th>
                <th className="px-4 py-3 font-semibold">Доставка</th>
                <th className="px-4 py-3 text-right font-semibold">Сумма</th>
                <th className="px-4 py-3 font-semibold">Статус</th>
                <th className="px-4 py-3 font-semibold">Оплата</th>
              </tr>
            </thead>
            <tbody>
              {page.items.map((order) => (
                <tr key={order.number} className={cn("border-t border-primary/10 hover:bg-sand/30",
                  order.status === "NEW" && "font-medium")}>
                  <td className="px-4 py-3">
                    <Link href={`/admin/orders/${order.number}`} className="underline-offset-4 hover:underline">
                      {order.number}
                    </Link>
                    <div className="text-xs text-muted-foreground">{formatDateTime(order.createdAt)}</div>
                  </td>
                  <td className="px-4 py-3">
                    {order.customerName}
                    <div className="text-xs text-muted-foreground">{formatPhone(order.customerPhone)}</div>
                  </td>
                  <td className="px-4 py-3">{DELIVERY_LABELS[order.deliveryMethod]}</td>
                  <td className="px-4 py-3 text-right whitespace-nowrap">
                    {formatPrice(order.total)}
                    <div className="text-xs text-muted-foreground">{order.itemCount} шт.</div>
                  </td>
                  <td className="px-4 py-3"><OrderStatusBadge status={order.status} /></td>
                  <td className="px-4 py-3"><PaymentStatusBadge status={order.paymentStatus} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Pagination page={page.page + 1} totalPages={page.totalPages} hrefFor={(p) => href({ ...filter, page: p })} />
    </>
  );
}

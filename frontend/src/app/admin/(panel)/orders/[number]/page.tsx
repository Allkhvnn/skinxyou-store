import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Suspense } from "react";

import { StatusActions } from "@/components/admin/status-actions";
import { OrderStatusBadge, PaymentStatusBadge } from "@/components/admin/status-badge";
import { getOrder } from "@/lib/admin/api";
import {
  DELIVERY_LABELS,
  formatDateTime,
  formatPhone,
  ORDER_STATUS_ACTIONS,
  ORDER_STATUS_LABELS,
  PAYMENT_STATUS_ACTIONS,
  PAYMENT_STATUS_LABELS,
} from "@/lib/admin/labels";
import { requireSession } from "@/lib/admin/session";
import { formatPrice } from "@/lib/format";

export const metadata: Metadata = { title: "Заказ" };

export default function OrderPage(props: PageProps<"/admin/orders/[number]">) {
  return (
    <Suspense fallback={<div className="h-96 animate-pulse bg-image" aria-hidden />}>
      <Order params={props.params} />
    </Suspense>
  );
}

async function Order({ params }: Pick<PageProps<"/admin/orders/[number]">, "params">) {
  const session = await requireSession();
  const { number } = await params;
  if (!/^SX-\d{1,10}$/.test(number)) notFound();
  const order = await getOrder(session, number);
  const phoneDigits = order.customerPhone.replace(/\D/g, "");
  const customerMessage = `Здравствуйте, ${order.customerName}! Это SkinxYou, пишем по вашему заказу ${order.number}.`;

  return (
    <div className="flex flex-col gap-8">
      <div>
        <Link href="/admin/orders" className="eyebrow text-muted-foreground hover:text-foreground">← Все заказы</Link>
        <div className="mt-3 flex flex-wrap items-center gap-3">
          <h1 className="page-title">{order.number}</h1>
          <OrderStatusBadge status={order.status} />
          <PaymentStatusBadge status={order.paymentStatus} />
        </div>
        <p className="mt-2 text-sm text-muted-foreground">Оформлен {formatDateTime(order.createdAt)}</p>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_380px]">
        <div className="flex flex-col gap-6">
          <section className="border border-primary/15 p-5">
            <h2 className="eyebrow mb-4">Состав заказа</h2>
            <table className="w-full text-sm">
              <tbody>
                {order.items.map((item) => (
                  <tr key={item.slug} className="border-b border-primary/10 last:border-0">
                    <td className="py-2 pr-4">
                      <Link href={`/product/${item.slug}`} target="_blank" className="hover:underline">{item.name}</Link>
                      {item.sku && <div className="text-xs text-muted-foreground">Артикул {item.sku}</div>}
                    </td>
                    <td className="py-2 pr-4 whitespace-nowrap text-muted-foreground">
                      {item.quantity} × {formatPrice(item.unitPrice)}
                    </td>
                    <td className="py-2 text-right whitespace-nowrap">{formatPrice(item.lineTotal)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <dl className="mt-4 flex flex-col gap-1 border-t border-primary/15 pt-4 text-sm">
              <div className="flex justify-between"><dt>Товары</dt><dd>{formatPrice(order.itemsTotal)}</dd></div>
              <div className="flex justify-between"><dt>Доставка</dt><dd>{formatPrice(order.deliveryFee)}</dd></div>
              <div className="mt-1 flex justify-between font-serif text-2xl"><dt>Итого</dt><dd>{formatPrice(order.total)}</dd></div>
            </dl>
          </section>

          <section className="border border-primary/15 p-5">
            <h2 className="eyebrow mb-4">Статус заказа</h2>
            <StatusActions number={order.number} version={order.version} kind="ORDER"
              targets={order.allowedStatuses} labels={ORDER_STATUS_ACTIONS} />
          </section>

          <section className="border border-primary/15 p-5">
            <h2 className="eyebrow mb-1">Оплата · Kaspi Pay</h2>
            <p className="mb-4 text-xs text-muted-foreground">
              Отправьте клиенту счёт в Kaspi на сумму {formatPrice(order.total)}, после оплаты отметьте заказ оплаченным.
            </p>
            <StatusActions number={order.number} version={order.version} kind="PAYMENT"
              targets={order.allowedPaymentStatuses} labels={PAYMENT_STATUS_ACTIONS} />
          </section>
        </div>

        <aside className="flex flex-col gap-6">
          <section className="border border-primary/15 p-5 text-sm">
            <h2 className="eyebrow mb-4">Клиент</h2>
            <p className="font-medium">{order.customerName}</p>
            <p className="mt-1">{formatPhone(order.customerPhone)}</p>
            <div className="mt-4 flex flex-wrap gap-2">
              <a href={`tel:${order.customerPhone}`} className="outline-button min-h-10 px-4">Позвонить</a>
              <a href={`https://wa.me/${phoneDigits}?text=${encodeURIComponent(customerMessage)}`} target="_blank"
                rel="noopener noreferrer" className="outline-button min-h-10 px-4">WhatsApp</a>
            </div>
          </section>

          <section className="border border-primary/15 p-5 text-sm">
            <h2 className="eyebrow mb-4">Доставка</h2>
            <p className="font-medium">{DELIVERY_LABELS[order.deliveryMethod]}</p>
            {order.deliveryAddress && <p className="mt-1">{order.deliveryAddress}</p>}
            {order.comment && (
              <>
                <h3 className="eyebrow mb-1 mt-4 text-muted-foreground">Комментарий клиента</h3>
                <p className="whitespace-pre-line">{order.comment}</p>
              </>
            )}
          </section>

          <section className="border border-primary/15 p-5 text-sm">
            <h2 className="eyebrow mb-4">История</h2>
            <ol className="flex flex-col gap-3">
              {order.history.toReversed().map((entry, i) => {
                const labels = entry.type === "PAYMENT" ? PAYMENT_STATUS_LABELS : ORDER_STATUS_LABELS;
                return (
                  <li key={i} className="border-l-2 border-primary/20 pl-3">
                    <p>
                      {entry.type === "PAYMENT" ? "Оплата: " : "Заказ: "}
                      {entry.fromStatus ? `${labels[entry.fromStatus] ?? entry.fromStatus} → ` : ""}
                      <span className="font-medium">{labels[entry.toStatus] ?? entry.toStatus}</span>
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {formatDateTime(entry.createdAt)} · {entry.changedBy === "system" ? "сайт" : entry.changedBy}
                    </p>
                    {entry.note && <p className="mt-1 text-xs">{entry.note}</p>}
                  </li>
                );
              })}
            </ol>
          </section>
        </aside>
      </div>
    </div>
  );
}

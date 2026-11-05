import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";

import { getAnalytics } from "@/lib/admin/api";
import { requireSession } from "@/lib/admin/session";
import { formatPrice } from "@/lib/format";

export const metadata: Metadata = { title: "Аналитика" };

const PERIODS = [
  { days: 7, label: "7 дней" },
  { days: 30, label: "30 дней" },
  { days: 90, label: "90 дней" },
  { days: 365, label: "Год" },
];

const dayFormat = new Intl.DateTimeFormat("ru-RU", { day: "2-digit", month: "2-digit", timeZone: "UTC" });

export default function AnalyticsPage(props: PageProps<"/admin/analytics">) {
  return (
    <div className="flex flex-col gap-8">
      <div>
        <h1 className="page-title">Аналитика</h1>
        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
          Выручка — только оплаченные заказы. Отменённые заказы не учитываются. Время — Алматы.
        </p>
      </div>
      <Suspense fallback={<div className="h-96 animate-pulse bg-image" aria-hidden />}>
        <Report searchParams={props.searchParams} />
      </Suspense>
    </div>
  );
}

async function Report({ searchParams }: Pick<PageProps<"/admin/analytics">, "searchParams">) {
  const session = await requireSession();
  const raw = (await searchParams).days;
  const requested = Number(Array.isArray(raw) ? raw[0] : raw);
  const days = PERIODS.some((p) => p.days === requested) ? requested : 30;
  const report = await getAnalytics(session, days);
  const s = report.summary;
  const maxRevenue = Math.max(1, ...report.days.map((d) => d.revenue));
  const maxOrders = Math.max(1, ...report.days.map((d) => d.orders));
  const totalDelivery = s.courierOrders + s.pickupOrders;

  return (
    <>
      <nav aria-label="Период" className="flex gap-2">
        {PERIODS.map((p) => (
          <Link key={p.days} href={`/admin/analytics?days=${p.days}`} className="category-pill"
            aria-current={p.days === days ? "page" : undefined}>
            {p.label}
          </Link>
        ))}
      </nav>

      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <Stat label="Выручка" value={formatPrice(s.revenue)} hint={`оплачено заказов: ${s.paidOrders}`} />
        <Stat label="Ожидает оплаты" value={formatPrice(s.awaitingPayment)} hint="неоплаченные заказы" />
        <Stat label="Заказов" value={String(s.orders)} hint={`отменено: ${s.cancelledOrders}`} />
        <Stat label="Средний чек" value={s.paidOrders ? formatPrice(s.averageCheck) : "—"} hint="по оплаченным" />
      </div>

      <section className="border border-primary/15 p-5">
        <h2 className="eyebrow mb-4">Выручка и заказы по дням</h2>
        <div className="flex h-48 items-end gap-px overflow-x-auto" role="img"
          aria-label={`Выручка за период: ${formatPrice(s.revenue)}`}>
          {report.days.map((d) => (
            <div key={d.date} className="group relative flex h-full min-w-1.5 flex-1 flex-col justify-end"
              title={`${dayFormat.format(new Date(d.date))}: ${formatPrice(d.revenue)}, заказов ${d.orders}`}>
              <div className="bg-sand" style={{ height: `${(d.orders / maxOrders) * 100}%` }}>
                <div className="w-full bg-forest" style={{ height: `${d.orders ? (d.revenue / maxRevenue) * 100 : 0}%` }} />
              </div>
            </div>
          ))}
        </div>
        <div className="mt-2 flex justify-between text-xs text-muted-foreground">
          <span>{dayFormat.format(new Date(report.from))}</span>
          <span className="flex gap-4">
            <span><span className="mr-1 inline-block size-2 bg-sand" />заказы</span>
            <span><span className="mr-1 inline-block size-2 bg-forest" />выручка</span>
          </span>
          <span>{dayFormat.format(new Date(report.to))}</span>
        </div>
      </section>

      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        <section className="border border-primary/15 p-5">
          <h2 className="eyebrow mb-4">Популярные товары</h2>
          {report.topProducts.length === 0 ? (
            <p className="text-sm text-muted-foreground">За этот период заказов нет.</p>
          ) : (
            <table className="w-full text-sm">
              <tbody>
                {report.topProducts.map((p, i) => (
                  <tr key={p.slug} className="border-b border-primary/10 last:border-0">
                    <td className="w-6 py-2 text-muted-foreground">{i + 1}</td>
                    <td className="py-2 pr-4">
                      <Link href={`/product/${p.slug}`} target="_blank" className="hover:underline">{p.name}</Link>
                    </td>
                    <td className="py-2 pr-4 text-right whitespace-nowrap text-muted-foreground">{p.quantity} шт.</td>
                    <td className="py-2 text-right whitespace-nowrap">{formatPrice(p.revenue)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>

        <section className="h-fit border border-primary/15 p-5 text-sm">
          <h2 className="eyebrow mb-4">Способ получения</h2>
          {totalDelivery === 0 ? (
            <p className="text-muted-foreground">Нет данных.</p>
          ) : (
            <dl className="flex flex-col gap-3">
              <Share label="Курьер" value={s.courierOrders} total={totalDelivery} />
              <Share label="Самовывоз" value={s.pickupOrders} total={totalDelivery} />
            </dl>
          )}
        </section>
      </div>
    </>
  );
}

function Stat({ label, value, hint }: { label: string; value: string; hint: string }) {
  return (
    <div className="border border-primary/15 p-4">
      <p className="eyebrow text-muted-foreground">{label}</p>
      <p className="mt-2 font-serif text-3xl">{value}</p>
      <p className="mt-1 text-xs text-muted-foreground">{hint}</p>
    </div>
  );
}

function Share({ label, value, total }: { label: string; value: number; total: number }) {
  const percent = Math.round((value / total) * 100);
  return (
    <div>
      <div className="flex justify-between"><dt>{label}</dt><dd>{value} · {percent}%</dd></div>
      <div className="mt-1 h-1.5 bg-sand"><div className="h-full bg-forest" style={{ width: `${percent}%` }} /></div>
    </div>
  );
}

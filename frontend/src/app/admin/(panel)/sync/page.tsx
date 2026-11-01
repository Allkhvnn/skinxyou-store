import type { Metadata } from "next";
import { Suspense } from "react";

import { RefreshWhileRunning, SyncControls } from "@/components/admin/sync-controls";
import { getSyncRuns, getSyncStatus, type SyncRun } from "@/lib/admin/api";
import { formatDateTime } from "@/lib/admin/labels";
import { requireSession } from "@/lib/admin/session";
import { cn } from "@/lib/utils";

export const metadata: Metadata = { title: "Синхронизация" };

const TYPE_LABELS = { DELTA: "Изменения", FULL: "Полная сверка" } as const;
const STATUS_LABELS = { RUNNING: "Идёт", SUCCESS: "Успешно", FAILED: "Ошибка" } as const;

export default function SyncPage() {
  return (
    <div className="flex flex-col gap-8">
      <div>
        <h1 className="page-title">Синхронизация с BILLZ</h1>
        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
          Товары, цены и остатки загружаются из BILLZ автоматически каждые 15 минут, полная сверка — каждую ночь в 04:00.
        </p>
      </div>
      <Suspense fallback={<div className="h-96 animate-pulse bg-image" aria-hidden />}>
        <SyncDashboard />
      </Suspense>
    </div>
  );
}

async function SyncDashboard() {
  const session = await requireSession();
  const [status, runs] = await Promise.all([getSyncStatus(session), getSyncRuns(session)]);
  const stale = status.stale;
  const lastFailed = status.lastRun?.status === "FAILED";

  return (
    <>
      <RefreshWhileRunning running={status.running} />

      {status.running ? (
        <Banner tone="info">Синхронизация идёт… Страница обновится сама.</Banner>
      ) : lastFailed ? (
        <Banner tone="error">
          Последняя синхронизация завершилась ошибкой: {status.lastRun?.errorMessage ?? "неизвестная ошибка"}
        </Banner>
      ) : stale ? (
        <Banner tone="error">
          Данные не обновлялись больше часа. Цены и остатки на сайте могут быть неактуальны.
        </Banner>
      ) : (
        <Banner tone="ok">Всё в порядке: цены и остатки актуальны.</Banner>
      )}

      <div className="grid grid-cols-2 gap-3 lg:grid-cols-5">
        <Stat label="Последнее обновление" value={status.lastSuccessAt ? formatDateTime(status.lastSuccessAt) : "—"} />
        <Stat label="Товаров на сайте" value={status.catalog.onSite} />
        <Stat label="В наличии" value={status.catalog.inStock} />
        <Stat label="Скрыто вручную" value={status.catalog.hidden} />
        <Stat label="Удалены в BILLZ" value={status.catalog.removedInBillz} />
      </div>

      <section className="border border-primary/15 p-5">
        <h2 className="eyebrow mb-4">Запустить вручную</h2>
        <SyncControls running={status.running} />
      </section>

      <section>
        <h2 className="eyebrow mb-4">Последние запуски</h2>
        {runs.items.length === 0 ? (
          <p className="text-sm text-muted-foreground">Запусков пока не было.</p>
        ) : (
          <div className="overflow-x-auto border border-primary/15">
            <table className="w-full min-w-[760px] text-sm">
              <thead className="bg-sand/60 text-left text-xs uppercase tracking-wider text-muted-foreground">
                <tr>
                  <th className="px-4 py-3 font-semibold">Начало</th>
                  <th className="px-4 py-3 font-semibold">Тип</th>
                  <th className="px-4 py-3 font-semibold">Результат</th>
                  <th className="px-4 py-3 text-right font-semibold">Получено</th>
                  <th className="px-4 py-3 text-right font-semibold">Новых</th>
                  <th className="px-4 py-3 text-right font-semibold">Обновлено</th>
                  <th className="px-4 py-3 text-right font-semibold">Снято</th>
                  <th className="px-4 py-3 text-right font-semibold">Время</th>
                </tr>
              </thead>
              <tbody>
                {runs.items.map((run) => <RunRow key={run.id} run={run} />)}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  );
}

function RunRow({ run }: { run: SyncRun }) {
  return (
    <>
      <tr className="border-t border-primary/10">
        <td className="px-4 py-3 whitespace-nowrap">{formatDateTime(run.startedAt)}</td>
        <td className="px-4 py-3">{TYPE_LABELS[run.type]}</td>
        <td className={cn("px-4 py-3 font-medium", run.status === "FAILED" && "text-destructive",
          run.status === "SUCCESS" && "text-forest")}>
          {STATUS_LABELS[run.status]}
        </td>
        <td className="px-4 py-3 text-right tabular-nums">{run.productsFetched}</td>
        <td className="px-4 py-3 text-right tabular-nums">{run.productsCreated}</td>
        <td className="px-4 py-3 text-right tabular-nums">{run.productsUpdated}</td>
        <td className="px-4 py-3 text-right tabular-nums">{run.productsDeactivated}</td>
        <td className="px-4 py-3 text-right tabular-nums text-muted-foreground">
          {run.durationMs == null ? "—" : `${(run.durationMs / 1000).toFixed(1)} с`}
        </td>
      </tr>
      {run.errorMessage && (
        <tr>
          <td colSpan={8} className="px-4 pb-3 text-xs text-destructive">{run.errorMessage}</td>
        </tr>
      )}
    </>
  );
}

function Stat({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="border border-primary/15 p-4">
      <p className="eyebrow text-muted-foreground">{label}</p>
      <p className="mt-2 font-serif text-2xl">{value}</p>
    </div>
  );
}

function Banner({ tone, children }: { tone: "ok" | "info" | "error"; children: React.ReactNode }) {
  return (
    <div role={tone === "error" ? "alert" : "status"} className={cn("border px-4 py-3 text-sm",
      tone === "ok" && "border-forest/30 bg-forest/5 text-forest",
      tone === "info" && "border-gold/40 bg-gold/10",
      tone === "error" && "border-destructive/40 bg-destructive/5 text-destructive")}>
      {children}
    </div>
  );
}

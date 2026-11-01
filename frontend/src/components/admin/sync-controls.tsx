"use client";

import { useRouter } from "next/navigation";
import { useActionState, useEffect } from "react";

import { runSync, type FormState } from "@/app/admin/actions";

export function SyncControls({ running }: { running: boolean }) {
  const [state, action, pending] = useActionState<FormState, FormData>(runSync, {});
  const disabled = running || pending;
  return (
    <form action={action} className="flex flex-col gap-3">
      <div className="flex flex-wrap gap-2">
        <button type="submit" name="type" value="DELTA" disabled={disabled} className="primary-button min-h-11 disabled:opacity-50">
          Загрузить изменения
        </button>
        <button type="submit" name="type" value="FULL" disabled={disabled} className="outline-button min-h-11 disabled:opacity-50"
          onClick={(e) => {
            if (!confirm("Полная сверка загрузит весь каталог и скроет товары, которых больше нет в BILLZ. Продолжить?")) {
              e.preventDefault();
            }
          }}>
          Полная сверка
        </button>
      </div>
      <p className="text-xs text-muted-foreground">
        «Загрузить изменения» — только товары, изменённые с прошлой синхронизации (секунды). «Полная сверка» — весь
        каталог; она также запускается автоматически каждую ночь.
      </p>
      {state.error && <p role="alert" className="text-sm text-destructive">{state.error}</p>}
    </form>
  );
}

/** Re-renders the page every few seconds while a sync is running, then stops. */
export function RefreshWhileRunning({ running }: { running: boolean }) {
  const router = useRouter();
  useEffect(() => {
    if (!running) return;
    const timer = setInterval(() => router.refresh(), 3000);
    return () => clearInterval(timer);
  }, [running, router]);
  return null;
}

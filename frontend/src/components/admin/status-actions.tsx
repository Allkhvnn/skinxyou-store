"use client";

import { useActionState } from "react";

import { changeStatus, type FormState } from "@/app/admin/actions";
import { cn } from "@/lib/utils";

/**
 * Buttons for the statuses the backend allows next. The clicked button's value is the target status;
 * the order version guards against two staff members editing the same order at once.
 */
export function StatusActions({
  number,
  version,
  kind,
  targets,
  labels,
}: {
  number: string;
  version: number;
  kind: "ORDER" | "PAYMENT";
  targets: string[];
  labels: Record<string, string>;
}) {
  const [state, action, pending] = useActionState<FormState, FormData>(changeStatus, {});

  if (targets.length === 0) {
    return <p className="text-sm text-muted-foreground">Статус окончательный, изменить нельзя.</p>;
  }

  return (
    <form action={action} className="flex flex-col gap-3">
      <input type="hidden" name="number" value={number} />
      <input type="hidden" name="kind" value={kind} />
      <input type="hidden" name="version" value={version} />
      <input name="note" maxLength={500} className="field" aria-label="Комментарий к изменению"
        placeholder={kind === "PAYMENT" ? "Комментарий, например номер чека Kaspi" : "Комментарий (необязательно)"} />
      <div className="flex flex-wrap gap-2">
        {targets.map((target) => {
          const destructive = target === "CANCELLED";
          return (
            <button key={target} type="submit" name="target" value={target} disabled={pending}
              className={cn(destructive ? "outline-button border-destructive text-destructive hover:bg-destructive"
                : "primary-button", "min-h-11 disabled:opacity-60")}
              onClick={(event) => {
                if (destructive && !confirm(`${labels[target]}? Это действие нельзя отменить.`)) event.preventDefault();
              }}>
              {labels[target] ?? target}
            </button>
          );
        })}
      </div>
      {state.error && <p role="alert" className="text-sm text-destructive">{state.error}</p>}
    </form>
  );
}

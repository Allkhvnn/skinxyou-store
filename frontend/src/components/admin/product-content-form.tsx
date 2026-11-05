"use client";

import { useActionState } from "react";

import { saveProduct, type FormState } from "@/app/admin/actions";
import type { AdminProductDetails } from "@/lib/admin/api";

export function ProductContentForm({ product }: { product: AdminProductDetails }) {
  const [state, action, pending] = useActionState<FormState & { saved?: boolean }, FormData>(saveProduct, {});
  return (
    // key on the saved values so the fields show the stored text after React's post-action form reset
    <form action={action} className="flex flex-col gap-5">
      <input type="hidden" name="id" value={product.id} />
      <label className="flex flex-col gap-1.5 text-sm">
        Описание для покупателей
        <textarea name="description" rows={10} maxLength={10000} defaultValue={product.description ?? ""}
          className="field h-auto py-2 leading-6" placeholder="Для кого, как действует, как применять, объём…" />
        <span className="text-xs text-muted-foreground">Абзацы разделяйте пустой строкой. До 10 000 символов.</span>
      </label>
      <label className="flex flex-col gap-1.5 text-sm">
        SEO-заголовок (title)
        <input name="seoTitle" maxLength={255} defaultValue={product.seoTitle ?? ""} className="field"
          placeholder={`По умолчанию: ${product.name}${product.brand ? ` — ${product.brand}` : ""}`} />
        <span className="text-xs text-muted-foreground">Показывается в Google. Оптимально до 60 символов.</span>
      </label>
      <label className="flex flex-col gap-1.5 text-sm">
        SEO-описание (description)
        <textarea name="seoDescription" rows={3} maxLength={500} defaultValue={product.seoDescription ?? ""}
          className="field h-auto py-2" placeholder="Короткий текст для поисковой выдачи, до 160 символов" />
      </label>
      <label className="flex items-center gap-3 text-sm">
        <input type="checkbox" name="hidden" defaultChecked={product.hidden} className="size-4 accent-primary" />
        Скрыть товар с сайта
      </label>
      <div className="flex items-center gap-4">
        <button type="submit" disabled={pending} className="primary-button min-h-11 disabled:opacity-60">
          {pending ? "Сохраняем…" : "Сохранить"}
        </button>
        {state.saved && <span role="status" className="text-sm text-forest">Сохранено. На сайте обновится в течение нескольких минут.</span>}
        {state.error && <span role="alert" className="text-sm text-destructive">{state.error}</span>}
      </div>
    </form>
  );
}

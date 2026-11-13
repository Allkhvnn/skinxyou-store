"use client";

import Image from "next/image";
import { useActionState } from "react";

import { manageBanner, type FormState } from "@/app/admin/actions";
import type { AdminBanner } from "@/lib/admin/api";

const FILE_INPUT = "text-sm file:mr-3 file:border file:border-primary file:bg-transparent file:px-3 file:py-2 file:text-xs file:font-semibold file:uppercase file:tracking-widest";

export function NewBannerForm() {
  const [state, action, pending] = useActionState<FormState, FormData>(manageBanner, {});
  return (
    <form action={action} className="flex flex-wrap items-end gap-3 border border-primary/15 p-5">
      <input type="hidden" name="op" value="create" />
      <label className="flex min-w-64 flex-1 flex-col gap-1 text-sm">
        <span className="eyebrow text-muted-foreground">Новый баннер</span>
        <input name="title" required maxLength={120} placeholder="Название, например «Скидки на Medicube»" className="field" />
      </label>
      <button type="submit" disabled={pending} className="primary-button min-h-11 disabled:opacity-60">
        {pending ? "Создаём…" : "Создать"}
      </button>
      {state.error && <p role="alert" className="w-full text-sm text-destructive">{state.error}</p>}
    </form>
  );
}

export function BannerEditor({ banner, first, last }: { banner: AdminBanner; first: boolean; last: boolean }) {
  const [state, action, pending] = useActionState<FormState, FormData>(manageBanner, {});

  return (
    <article className="flex flex-col gap-5 border border-primary/15 p-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <h2 className="font-serif text-2xl">{banner.title}</h2>
          <span className={`product-badge static ${banner.shownNow ? "bg-forest text-cream" : "bg-sand"}`}>
            {banner.shownNow ? "Показывается" : "Не показывается"}
          </span>
        </div>
        <form action={action} className="flex gap-4 text-xs">
          <input type="hidden" name="id" value={banner.id} />
          <button type="submit" name="op" value="up" disabled={pending || first} className="underline disabled:opacity-30">↑ Выше</button>
          <button type="submit" name="op" value="down" disabled={pending || last} className="underline disabled:opacity-30">↓ Ниже</button>
          <button type="submit" name="op" value="delete" disabled={pending} className="text-destructive underline"
            onClick={(e) => { if (!confirm("Удалить баннер вместе с картинками?")) e.preventDefault(); }}>
            Удалить
          </button>
        </form>
      </div>

      {!banner.shownNow && <p className="text-xs text-muted-foreground">{whyHidden(banner)}</p>}

      <div className="grid gap-5 md:grid-cols-[2.857fr_1fr]">
        <ImageSlot label="Компьютер · 1600×560" url={banner.desktopImageUrl} aspect="aspect-[1600/560]">
          <form action={action} className="flex flex-wrap items-center gap-3">
            <input type="hidden" name="id" value={banner.id} />
            <input type="file" name="desktopFile" accept="image/jpeg,image/png,image/webp" required className={FILE_INPUT} />
            <button type="submit" name="op" value="desktop" disabled={pending} className="outline-button min-h-10 disabled:opacity-60">
              Загрузить
            </button>
          </form>
        </ImageSlot>
        <ImageSlot label="Телефон · 800×800" url={banner.mobileImageUrl} aspect="aspect-square">
          <form action={action} className="flex flex-col items-start gap-2">
            <input type="hidden" name="id" value={banner.id} />
            <input type="file" name="mobileFile" accept="image/jpeg,image/png,image/webp" required className={FILE_INPUT} />
            <div className="flex gap-3">
              <button type="submit" name="op" value="mobile" disabled={pending} className="outline-button min-h-10 disabled:opacity-60">
                Загрузить
              </button>
            </div>
          </form>
          {banner.mobileImageUrl && (
            <form action={action}>
              <input type="hidden" name="id" value={banner.id} />
              <button type="submit" name="op" value="removeMobile" disabled={pending} className="text-xs text-destructive underline">
                Убрать картинку для телефона
              </button>
            </form>
          )}
        </ImageSlot>
      </div>

      <form action={action} className="grid gap-4 border-t border-primary/10 pt-5 sm:grid-cols-2 lg:grid-cols-4">
        <input type="hidden" name="id" value={banner.id} />
        <label className="flex flex-col gap-1 text-sm sm:col-span-2">
          <span className="eyebrow text-muted-foreground">Название (для незрячих покупателей)</span>
          <input name="title" required maxLength={120} defaultValue={banner.title} className="field" />
        </label>
        <label className="flex flex-col gap-1 text-sm sm:col-span-2">
          <span className="eyebrow text-muted-foreground">Ссылка при нажатии</span>
          <input name="linkUrl" maxLength={500} defaultValue={banner.linkUrl ?? ""} placeholder="/catalog?brand=medicube или https://…"
            className="field" />
        </label>
        <label className="flex flex-col gap-1 text-sm">
          <span className="eyebrow text-muted-foreground">Показывать с</span>
          <input type="date" name="startsOn" defaultValue={banner.startsOn ?? ""} className="field" />
        </label>
        <label className="flex flex-col gap-1 text-sm">
          <span className="eyebrow text-muted-foreground">по (включительно)</span>
          <input type="date" name="endsOn" defaultValue={banner.endsOn ?? ""} className="field" />
        </label>
        <label className="flex items-center gap-2 self-end pb-3 text-sm">
          <input type="checkbox" name="active" defaultChecked={banner.active} className="size-4 accent-primary" />
          Показывать на сайте
        </label>
        <div className="self-end">
          <button type="submit" name="op" value="save" disabled={pending} className="primary-button min-h-11 w-full disabled:opacity-60">
            {pending ? "Сохраняем…" : "Сохранить"}
          </button>
        </div>
      </form>

      {state.error && <p role="alert" className="text-sm text-destructive">{state.error}</p>}
    </article>
  );
}

function ImageSlot({ label, url, aspect, children }: {
  label: string; url: string | null; aspect: string; children: React.ReactNode;
}) {
  return (
    <div className="flex flex-col gap-3">
      <p className="eyebrow text-muted-foreground">{label}</p>
      <div className={`relative ${aspect} overflow-hidden bg-image`}>
        {url ? (
          <Image src={url} alt="" fill sizes="(min-width: 768px) 60vw, 100vw" className="object-cover" />
        ) : (
          <div className="flex h-full items-center justify-center text-xs text-muted-foreground">Нет картинки</div>
        )}
      </div>
      {children}
    </div>
  );
}

function whyHidden(b: AdminBanner): string {
  if (!b.desktopImageUrl) return "Загрузите картинку для компьютера — без неё баннер не показывается.";
  if (!b.active) return "Включите «Показывать на сайте» и сохраните.";
  return "Сегодня вне выбранных дат показа.";
}

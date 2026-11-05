import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { Suspense } from "react";

import { Pagination } from "@/components/pagination";
import { listProducts, PRODUCT_FILTERS, type ProductFilter } from "@/lib/admin/api";
import { requireSession } from "@/lib/admin/session";
import { formatPrice } from "@/lib/format";

export const metadata: Metadata = { title: "Товары" };

const FILTER_LABELS: Record<ProductFilter, string> = {
  NO_DESCRIPTION: "Без описания",
  NO_PHOTO: "Без фото",
  HIDDEN: "Скрытые",
  REMOVED_IN_BILLZ: "Удалены в BILLZ",
};

function href(f: { q?: string; filter?: ProductFilter; page?: number }) {
  const params = new URLSearchParams();
  if (f.q) params.set("q", f.q);
  if (f.filter) params.set("filter", f.filter);
  if (f.page && f.page > 1) params.set("page", String(f.page));
  const qs = params.toString();
  return qs ? `/admin/products?${qs}` : "/admin/products";
}

export default function ProductsPage(props: PageProps<"/admin/products">) {
  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="page-title">Товары</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Название, цена и остаток приходят из BILLZ. Здесь — описание, SEO и видимость на сайте.
        </p>
      </div>
      <Suspense fallback={<div className="h-96 animate-pulse bg-image" aria-hidden />}>
        <Products searchParams={props.searchParams} />
      </Suspense>
    </div>
  );
}

async function Products({ searchParams }: Pick<PageProps<"/admin/products">, "searchParams">) {
  const session = await requireSession();
  const raw = await searchParams;
  const one = (v: string | string[] | undefined) => (Array.isArray(v) ? v[0] : v);
  const filterParam = one(raw.filter);
  const filter = (PRODUCT_FILTERS as readonly string[]).includes(filterParam ?? "") ? (filterParam as ProductFilter) : undefined;
  const q = one(raw.q)?.trim().slice(0, 100) || undefined;
  const pageNumber = Math.max(1, Math.floor(Number(one(raw.page)) || 1));
  const page = await listProducts(session, { q, filter, page: pageNumber });

  return (
    <>
      <nav aria-label="Фильтр" className="flex max-w-full gap-2 overflow-x-auto pb-1">
        <Link href={href({ q })} className="category-pill" aria-current={!filter ? "page" : undefined}>Все</Link>
        {PRODUCT_FILTERS.map((f) => (
          <Link key={f} href={href({ q, filter: f })} className="category-pill" aria-current={filter === f ? "page" : undefined}>
            {FILTER_LABELS[f]}
          </Link>
        ))}
      </nav>
      <form action="/admin/products" method="get" className="flex max-w-xl gap-2">
        {filter && <input type="hidden" name="filter" value={filter} />}
        <input name="q" type="search" defaultValue={q} maxLength={100} className="field"
          placeholder="Название, артикул, бренд или штрихкод" aria-label="Поиск товаров" />
        <button type="submit" className="outline-button min-h-11">Найти</button>
      </form>
      <p className="eyebrow text-muted-foreground">Найдено: {page.totalItems}</p>

      <ul className="flex flex-col border-t border-primary/15">
        {page.items.map((p) => (
          <li key={p.id} className="border-b border-primary/15">
            <Link href={`/admin/products/${p.id}`} className="flex items-center gap-4 py-3 hover:bg-sand/30">
              <div className="relative size-14 shrink-0 overflow-hidden bg-image">
                {p.imageUrl && <Image src={p.imageUrl} alt="" fill sizes="56px" className="object-cover" />}
              </div>
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium">{p.name}</p>
                <p className="text-xs text-muted-foreground">
                  {[p.brand, p.sku, p.price != null ? formatPrice(p.price) : null, `остаток ${Number(p.stock)}`]
                    .filter(Boolean).join(" · ")}
                </p>
              </div>
              <div className="hidden shrink-0 gap-2 text-[0.65rem] font-semibold uppercase tracking-wider sm:flex">
                {!p.hasDescription && <span className="border border-destructive px-2 py-1 text-destructive">нет описания</span>}
                {p.imageCount === 0 && <span className="border border-destructive px-2 py-1 text-destructive">нет фото</span>}
                {p.hidden && <span className="bg-muted px-2 py-1 text-muted-foreground">скрыт</span>}
                {!p.availableInBillz && <span className="bg-muted px-2 py-1 text-muted-foreground">удалён в BILLZ</span>}
              </div>
            </Link>
          </li>
        ))}
      </ul>
      <Pagination page={page.page + 1} totalPages={page.totalPages} hrefFor={(n) => href({ q, filter, page: n })} />
    </>
  );
}

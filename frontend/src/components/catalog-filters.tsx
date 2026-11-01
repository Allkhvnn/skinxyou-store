import Link from "next/link";

import { AutoSubmitForm } from "@/components/auto-submit-form";
import { SORTS, type Brand, type Category, type ProductQuery } from "@/lib/catalog-api";
import { catalogHref } from "@/lib/catalog-query";

/**
 * Category pills + sort/stock controls + collapsible filters, submitting to the same URL.
 * When `fixedBrand` is set (brand page), the brand comes from the path instead of a selector.
 */
export function CatalogFilters({
  action,
  query,
  brands,
  categories,
  fixedBrand,
}: {
  action: string;
  query: ProductQuery;
  brands: Brand[];
  categories: Category[];
  fixedBrand?: boolean;
}) {
  const extraFilters = [query.brand && !fixedBrand, query.minPrice != null, query.maxPrice != null].filter(Boolean).length;
  const topCategories = categories.toSorted((a, b) => b.productCount - a.productCount).slice(0, 8);

  return (
    <div className="flex flex-col gap-5">
      {topCategories.length > 0 && (
        <nav aria-label="Категории" className="flex max-w-full gap-2 overflow-x-auto pb-1">
          <Link href={catalogHref(action, query, { category: undefined, page: 1 })} className="category-pill"
            aria-current={!query.category ? "page" : undefined}>
            Все
          </Link>
          {topCategories.map((c) => (
            <Link key={c.slug} href={catalogHref(action, query, { category: c.slug, page: 1 })} className="category-pill"
              aria-current={query.category === c.slug ? "page" : undefined}>
              {c.name}
            </Link>
          ))}
        </nav>
      )}

      <AutoSubmitForm action={action} className="flex flex-col gap-3">
        {query.q && <input type="hidden" name="q" value={query.q} />}
        {query.category && <input type="hidden" name="category" value={query.category} />}

        <div className="flex flex-wrap items-center gap-2">
          <label className="sr-only" htmlFor="sort">Сортировка</label>
          <select id="sort" name="sort" defaultValue={query.sort ?? "newest"} className="field w-auto min-w-48">
            {Object.entries(SORTS).map(([value, label]) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </select>
          <label className="flex h-11 items-center gap-2 border border-input px-3 text-sm">
            <input type="checkbox" name="inStock" value="true" defaultChecked={query.inStock} className="size-4 accent-primary" />
            Только в наличии
          </label>

          <details className="group relative w-full sm:w-auto" open={extraFilters > 0}>
            <summary className="flex h-11 cursor-pointer select-none items-center border border-input px-3 text-sm marker:hidden">
              Бренд и цена{extraFilters > 0 ? ` (${extraFilters})` : ""}
            </summary>
            <div className="mt-2 grid gap-3 border border-input p-3 sm:absolute sm:z-20 sm:w-[min(36rem,90vw)] sm:grid-cols-2 sm:bg-background">
              {!fixedBrand && (
                <div className="flex flex-col gap-1 sm:col-span-2">
                  <label htmlFor="brand" className="eyebrow text-muted-foreground">Бренд</label>
                  <select id="brand" name="brand" defaultValue={query.brand ?? ""} className="field">
                    <option value="">Все бренды</option>
                    {brands.map((b) => (
                      <option key={b.slug} value={b.slug}>{b.name}</option>
                    ))}
                  </select>
                </div>
              )}
              <div className="flex flex-col gap-1 sm:col-span-2">
                <span className="eyebrow text-muted-foreground">Цена, ₸</span>
                <div className="flex gap-2">
                  <input name="minPrice" type="number" min={0} inputMode="numeric" placeholder="от"
                    defaultValue={query.minPrice} aria-label="Цена от" className="field" />
                  <input name="maxPrice" type="number" min={0} inputMode="numeric" placeholder="до"
                    defaultValue={query.maxPrice} aria-label="Цена до" className="field" />
                </div>
              </div>
              <button type="submit" className="primary-button min-h-11">Показать</button>
              <Link href={action} className="outline-button min-h-11">Сбросить</Link>
            </div>
          </details>
        </div>
      </AutoSubmitForm>
    </div>
  );
}

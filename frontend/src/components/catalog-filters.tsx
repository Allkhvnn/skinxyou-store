import Link from "next/link";

import { AutoSubmitForm } from "@/components/auto-submit-form";
import { buttonVariants } from "@/components/ui/button";
import { SORTS, type Brand, type Category, type ProductQuery } from "@/lib/catalog-api";
import { cn } from "@/lib/utils";

const fieldClass =
  "h-10 w-full rounded-lg border border-input bg-background px-3 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50";

/**
 * Server-rendered filter form submitting to the same URL. When `fixedBrand` is set (brand page),
 * the brand selector is hidden and the brand comes from the path instead.
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
  const activeCount = [query.brand && !fixedBrand, query.category, query.minPrice != null, query.maxPrice != null, query.inStock]
    .filter(Boolean).length;

  return (
    <AutoSubmitForm action={action} className="flex flex-col gap-3">
      {query.q && <input type="hidden" name="q" value={query.q} />}

      <div className="flex items-center gap-2">
        <label className="sr-only" htmlFor="sort">Сортировка</label>
        <select id="sort" name="sort" defaultValue={query.sort ?? "newest"} className={cn(fieldClass, "sm:w-56")}>
          {Object.entries(SORTS).map(([value, label]) => (
            <option key={value} value={value}>{label}</option>
          ))}
        </select>
        <label className="flex h-10 shrink-0 items-center gap-2 rounded-lg border border-input px-3 text-sm">
          <input type="checkbox" name="inStock" value="true" defaultChecked={query.inStock} className="size-4" />
          В наличии
        </label>
      </div>

      <details className="rounded-lg border border-input" open={activeCount > 0}>
        <summary className="cursor-pointer select-none px-3 py-2.5 text-sm font-medium">
          Фильтры{activeCount > 0 ? ` (${activeCount})` : ""}
        </summary>
        <div className="grid gap-3 border-t border-input p-3 sm:grid-cols-2 lg:grid-cols-4">
          {!fixedBrand && (
            <div className="flex flex-col gap-1">
              <label htmlFor="brand" className="text-xs text-muted-foreground">Бренд</label>
              <select id="brand" name="brand" defaultValue={query.brand ?? ""} className={fieldClass}>
                <option value="">Все бренды</option>
                {brands.map((b) => (
                  <option key={b.slug} value={b.slug}>{b.name}</option>
                ))}
              </select>
            </div>
          )}
          <div className="flex flex-col gap-1">
            <label htmlFor="category" className="text-xs text-muted-foreground">Категория</label>
            <select id="category" name="category" defaultValue={query.category ?? ""} className={fieldClass}>
              <option value="">Все категории</option>
              {categories.map((c) => (
                <option key={c.slug} value={c.slug}>{c.name}</option>
              ))}
            </select>
          </div>
          <div className="flex flex-col gap-1">
            <span className="text-xs text-muted-foreground">Цена, ₸</span>
            <div className="flex gap-2">
              <input name="minPrice" type="number" min={0} inputMode="numeric" placeholder="от"
                defaultValue={query.minPrice} aria-label="Цена от" className={fieldClass} />
              <input name="maxPrice" type="number" min={0} inputMode="numeric" placeholder="до"
                defaultValue={query.maxPrice} aria-label="Цена до" className={fieldClass} />
            </div>
          </div>
          <div className="flex items-end gap-2">
            <button type="submit" className={cn(buttonVariants(), "h-10 flex-1")}>Показать</button>
            <Link href={action} className={cn(buttonVariants({ variant: "outline" }), "h-10")}>Сбросить</Link>
          </div>
        </div>
      </details>
    </AutoSubmitForm>
  );
}

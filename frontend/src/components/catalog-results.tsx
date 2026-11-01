import Link from "next/link";

import { CatalogFilters } from "@/components/catalog-filters";
import { Pagination } from "@/components/pagination";
import { ProductGrid } from "@/components/product-card";
import { getBrands, getCategories, getProducts, type ProductQuery } from "@/lib/catalog-api";
import { catalogHref } from "@/lib/catalog-query";
import { pluralizeProducts } from "@/lib/format";

/** Filters + product grid + pagination for /catalog and /brands/[slug]. */
export async function CatalogResults({
  basePath,
  query,
  fixedBrand,
}: {
  basePath: string;
  query: ProductQuery;
  fixedBrand?: boolean;
}) {
  const [page, brands, categories] = await Promise.all([getProducts(query), getBrands(), getCategories()]);
  // Filters in the URL are relative to the base path; the fixed brand is part of the path.
  const urlQuery = fixedBrand ? { ...query, brand: undefined } : query;

  return (
    <div className="flex flex-col gap-8">
      <CatalogFilters action={basePath} query={urlQuery} brands={brands} categories={categories} fixedBrand={fixedBrand} />

      <p className="eyebrow text-muted-foreground">
        {query.q ? `По запросу «${query.q}»: ` : "Найдено: "}
        {pluralizeProducts(page.totalItems)}
      </p>

      {page.items.length === 0 ? (
        <div className="flex flex-col items-center gap-3 border border-primary/15 px-5 py-20 text-center">
          <p className="font-serif text-3xl">Ничего не найдено.</p>
          <p className="max-w-sm text-sm text-muted-foreground">Попробуйте изменить запрос или сбросить фильтры.</p>
          <Link href={basePath} className="outline-button mt-3">Сбросить фильтры</Link>
        </div>
      ) : (
        <ProductGrid products={page.items} />
      )}

      <Pagination
        page={page.page + 1}
        totalPages={page.totalPages}
        hrefFor={(p) => catalogHref(basePath, urlQuery, { page: p })}
      />
    </div>
  );
}

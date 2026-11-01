import { SORTS, type ProductQuery, type Sort } from "@/lib/catalog-api";

type RawParams = Record<string, string | string[] | undefined>;

function first(value: string | string[] | undefined): string | undefined {
  const v = Array.isArray(value) ? value[0] : value;
  const trimmed = v?.trim();
  return trimmed ? trimmed : undefined;
}

function positiveNumber(value: string | undefined): number | undefined {
  if (value == null) return undefined;
  const n = Number(value);
  return Number.isFinite(n) && n >= 0 ? n : undefined;
}

/** Parses untrusted URL params; anything invalid is dropped instead of failing the page. */
export function parseCatalogQuery(params: RawParams): ProductQuery {
  const sort = first(params.sort);
  const page = Math.floor(positiveNumber(first(params.page)) ?? 1);
  return {
    q: first(params.q)?.slice(0, 100),
    brand: first(params.brand),
    category: first(params.category),
    minPrice: positiveNumber(first(params.minPrice)),
    maxPrice: positiveNumber(first(params.maxPrice)),
    inStock: first(params.inStock) === "true",
    sort: sort && sort in SORTS ? (sort as Sort) : undefined,
    page: Math.min(Math.max(page, 1), 10_000),
  };
}

/** Builds a storefront URL for the catalog, omitting defaults to keep URLs short and canonical. */
export function catalogHref(basePath: string, query: ProductQuery, overrides: Partial<ProductQuery> = {}) {
  const merged = { ...query, ...overrides };
  const params = new URLSearchParams();
  if (merged.q) params.set("q", merged.q);
  if (merged.brand) params.set("brand", merged.brand);
  if (merged.category) params.set("category", merged.category);
  if (merged.minPrice != null) params.set("minPrice", String(merged.minPrice));
  if (merged.maxPrice != null) params.set("maxPrice", String(merged.maxPrice));
  if (merged.inStock) params.set("inStock", "true");
  if (merged.sort && merged.sort !== "newest") params.set("sort", merged.sort);
  if (merged.page && merged.page > 1) params.set("page", String(merged.page));
  const qs = params.toString();
  return qs ? `${basePath}?${qs}` : basePath;
}

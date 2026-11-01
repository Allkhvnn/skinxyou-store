import { cacheLife } from "next/cache";

// Server-only: not prefixed with NEXT_PUBLIC_, so it never reaches the browser bundle.
const API_BASE_URL = process.env.API_BASE_URL ?? "http://localhost:8080";

export type BrandRef = { slug: string; name: string };

export type ProductSummary = {
  slug: string;
  name: string;
  brand: BrandRef | null;
  price: number | null;
  promoPrice: number | null;
  currency: string | null;
  inStock: boolean;
  imageUrl: string | null;
};

export type ProductDetails = Omit<ProductSummary, "imageUrl"> & {
  sku: string | null;
  categories: { slug: string; name: string }[];
  images: { url: string }[];
  description: string | null;
  seoTitle: string | null;
  seoDescription: string | null;
  related: ProductSummary[];
};

export type Page<T> = {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
};

export type Brand = { slug: string; name: string; productCount: number };

export type Category = {
  slug: string;
  name: string;
  parentSlug: string | null;
  productCount: number;
};

export const SORTS = {
  newest: "Сначала новые",
  price_asc: "Сначала дешевле",
  price_desc: "Сначала дороже",
  name: "По названию",
} as const;

export type Sort = keyof typeof SORTS;

/** Catalog query as used by the storefront; `page` is 1-based here and 0-based in the API. */
export type ProductQuery = {
  q?: string;
  brand?: string;
  category?: string;
  minPrice?: number;
  maxPrice?: number;
  inStock?: boolean;
  sort?: Sort;
  page?: number;
  size?: number;
};

async function getJson<T>(path: string): Promise<T | null> {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    headers: { Accept: "application/json" },
  });
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`Catalog API responded ${res.status} for ${path}`);
  return (await res.json()) as T;
}

function toApiParams(query: ProductQuery): string {
  const params = new URLSearchParams();
  if (query.q) params.set("q", query.q);
  if (query.brand) params.set("brand", query.brand);
  if (query.category) params.set("category", query.category);
  if (query.minPrice != null) params.set("minPrice", String(query.minPrice));
  if (query.maxPrice != null) params.set("maxPrice", String(query.maxPrice));
  if (query.inStock) params.set("inStock", "true");
  if (query.sort) params.set("sort", query.sort);
  params.set("page", String(Math.max(0, (query.page ?? 1) - 1)));
  params.set("size", String(query.size ?? 24));
  return params.toString();
}

// Prices and stock sync from BILLZ every 15 minutes, so a few minutes of caching is safe.
// Checkout re-validates price and stock on the backend anyway.

export async function getProducts(query: ProductQuery): Promise<Page<ProductSummary>> {
  "use cache";
  cacheLife("minutes");
  return (await getJson<Page<ProductSummary>>(`/api/catalog/products?${toApiParams(query)}`))!;
}

export async function getProduct(slug: string): Promise<ProductDetails | null> {
  "use cache";
  cacheLife("minutes");
  return getJson<ProductDetails>(`/api/catalog/products/${encodeURIComponent(slug)}`);
}

export async function getBrands(): Promise<Brand[]> {
  "use cache";
  cacheLife("hours");
  return (await getJson<Brand[]>("/api/catalog/brands")) ?? [];
}

export async function getBrand(slug: string): Promise<Brand | null> {
  "use cache";
  cacheLife("hours");
  return getJson<Brand>(`/api/catalog/brands/${encodeURIComponent(slug)}`);
}

export async function getCategories(): Promise<Category[]> {
  "use cache";
  cacheLife("hours");
  return (await getJson<Category[]>("/api/catalog/categories")) ?? [];
}

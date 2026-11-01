import type { Metadata } from "next";
import Link from "next/link";
import { connection } from "next/server";
import { Suspense } from "react";

import { getBrands } from "@/lib/catalog-api";
import { pluralizeProducts } from "@/lib/format";

export const metadata: Metadata = {
  title: "Бренды",
  description: "Бренды уходовой и декоративной косметики в интернет-магазине SkinxYou, Алматы.",
  alternates: { canonical: "/brands" },
};

export default function BrandsPage() {
  return (
    <div className="flex flex-col gap-5">
      <h1 className="text-2xl font-semibold tracking-tight">Бренды</h1>
      <Suspense fallback={<div className="h-64 animate-pulse rounded-xl bg-muted" aria-hidden />}>
        <BrandList />
      </Suspense>
    </div>
  );
}

async function BrandList() {
  // Rendered per request (from cached data) so the build does not depend on the backend being up.
  await connection();
  const brands = await getBrands();
  if (brands.length === 0) {
    return <p className="text-sm text-muted-foreground">Бренды скоро появятся.</p>;
  }
  return (
    <ul className="grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-4">
      {brands.map((brand) => (
        <li key={brand.slug}>
          <Link href={`/brands/${brand.slug}`}
            className="flex flex-col rounded-xl border p-4 transition-colors hover:bg-muted">
            <span className="font-medium">{brand.name}</span>
            <span className="text-xs text-muted-foreground">{pluralizeProducts(brand.productCount)}</span>
          </Link>
        </li>
      ))}
    </ul>
  );
}

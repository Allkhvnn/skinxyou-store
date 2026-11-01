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
    <div className="container-page flex flex-col gap-10 py-12 lg:py-16">
      <header>
        <p className="eyebrow mb-3">Магазин</p>
        <h1 className="page-title">Бренды</h1>
      </header>
      <Suspense fallback={<div className="h-64 animate-pulse bg-image" aria-hidden />}>
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
    <ul className="grid grid-cols-1 border-t border-primary/15 sm:grid-cols-2 lg:grid-cols-3">
      {brands.map((brand) => (
        <li key={brand.slug} className="border-b border-primary/15">
          <Link href={`/brands/${brand.slug}`} className="group flex items-baseline justify-between gap-4 py-5 pr-4">
            <span className="font-serif text-2xl transition group-hover:translate-x-1">{brand.name}</span>
            <span className="eyebrow shrink-0 text-muted-foreground">{pluralizeProducts(brand.productCount)}</span>
          </Link>
        </li>
      ))}
    </ul>
  );
}

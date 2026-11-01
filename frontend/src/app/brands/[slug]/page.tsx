import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { Suspense } from "react";

import { CatalogResults } from "@/components/catalog-results";
import { ProductGridSkeleton } from "@/components/product-card";
import { getBrand } from "@/lib/catalog-api";
import { parseCatalogQuery } from "@/lib/catalog-query";

export async function generateMetadata(props: PageProps<"/brands/[slug]">): Promise<Metadata> {
  const { slug } = await props.params;
  const brand = await getBrand(slug);
  if (!brand) return { title: "Бренд не найден" };
  return {
    title: `${brand.name} — косметика в Алматы`,
    description: `Косметика ${brand.name} в интернет-магазине SkinxYou: цены, наличие, доставка по Алматы.`,
    alternates: { canonical: `/brands/${brand.slug}` },
  };
}

export default function BrandPage(props: PageProps<"/brands/[slug]">) {
  return (
    <Suspense fallback={<ProductGridSkeleton />}>
      <BrandContent params={props.params} searchParams={props.searchParams} />
    </Suspense>
  );
}

async function BrandContent({ params, searchParams }: Pick<PageProps<"/brands/[slug]">, "params" | "searchParams">) {
  const { slug } = await params;
  const brand = await getBrand(slug);
  if (!brand) notFound();
  const query = { ...parseCatalogQuery(await searchParams), brand: brand.slug };

  return (
    <div className="flex flex-col gap-5">
      <h1 className="text-2xl font-semibold tracking-tight">{brand.name}</h1>
      <CatalogResults basePath={`/brands/${brand.slug}`} query={query} fixedBrand />
    </div>
  );
}

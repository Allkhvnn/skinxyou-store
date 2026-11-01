import type { Metadata } from "next";
import Link from "next/link";
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
    <div className="container-page py-12 lg:py-16">
      <Suspense fallback={<ProductGridSkeleton />}>
        <BrandContent params={props.params} searchParams={props.searchParams} />
      </Suspense>
    </div>
  );
}

async function BrandContent({ params, searchParams }: Pick<PageProps<"/brands/[slug]">, "params" | "searchParams">) {
  const { slug } = await params;
  const brand = await getBrand(slug);
  if (!brand) notFound();
  const query = { ...parseCatalogQuery(await searchParams), brand: brand.slug };

  return (
    <div className="flex flex-col gap-8">
      <header>
        <Link href="/brands" className="eyebrow mb-3 inline-block hover:underline">Бренды</Link>
        <h1 className="page-title">{brand.name}</h1>
      </header>
      <CatalogResults basePath={`/brands/${brand.slug}`} query={query} fixedBrand />
    </div>
  );
}

import type { Metadata } from "next";
import { Suspense } from "react";

import { CatalogResults } from "@/components/catalog-results";
import { ProductGridSkeleton } from "@/components/product-card";
import { parseCatalogQuery } from "@/lib/catalog-query";

export const metadata: Metadata = {
  title: "Каталог косметики",
  description: "Уходовая и декоративная косметика с доставкой по Алматы: цены, наличие, бренды.",
  // Filtered and paginated variants point to the main catalog to avoid duplicate pages in search.
  alternates: { canonical: "/catalog" },
};

export default function CatalogPage(props: PageProps<"/catalog">) {
  return (
    <div className="flex flex-col gap-5">
      <h1 className="text-2xl font-semibold tracking-tight">Каталог</h1>
      <Suspense fallback={<ProductGridSkeleton />}>
        <Results searchParams={props.searchParams} />
      </Suspense>
    </div>
  );
}

async function Results({ searchParams }: Pick<PageProps<"/catalog">, "searchParams">) {
  const query = parseCatalogQuery(await searchParams);
  return <CatalogResults basePath="/catalog" query={query} />;
}

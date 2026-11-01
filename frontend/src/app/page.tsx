import Link from "next/link";
import { connection } from "next/server";
import { Suspense } from "react";

import { ProductGrid, ProductGridSkeleton } from "@/components/product-card";
import { buttonVariants } from "@/components/ui/button";
import { WhatsAppButton } from "@/components/whatsapp-button";
import { getBrands, getProducts } from "@/lib/catalog-api";

export default function Home() {
  return (
    <div className="flex flex-col gap-12">
      <section className="flex flex-col items-start gap-4 rounded-2xl bg-muted px-6 py-10 sm:px-10 sm:py-14">
        <h1 className="max-w-xl text-3xl font-semibold leading-tight tracking-tight sm:text-4xl">
          Уходовая и декоративная косметика в Алматы
        </h1>
        <p className="max-w-lg text-muted-foreground">
          Корейский уход и проверенные бренды. Актуальные цены и наличие, заказ на сайте или в WhatsApp.
        </p>
        <div className="flex flex-wrap gap-2">
          <Link href="/catalog" className={buttonVariants({ size: "lg", className: "h-11 px-5 text-base" })}>
            Перейти в каталог
          </Link>
          <WhatsAppButton variant="outline" message="Здравствуйте! Хочу проконсультироваться по уходу.">
            Консультация в WhatsApp
          </WhatsAppButton>
        </div>
      </section>

      <section className="flex flex-col gap-4">
        <div className="flex items-baseline justify-between">
          <h2 className="text-xl font-semibold tracking-tight">Новинки в наличии</h2>
          <Link href="/catalog?inStock=true" className="text-sm hover:underline">Все товары →</Link>
        </div>
        <Suspense fallback={<ProductGridSkeleton />}>
          <NewArrivals />
        </Suspense>
      </section>

      <section className="flex flex-col gap-4">
        <div className="flex items-baseline justify-between">
          <h2 className="text-xl font-semibold tracking-tight">Бренды</h2>
          <Link href="/brands" className="text-sm hover:underline">Все бренды →</Link>
        </div>
        <Suspense fallback={<div className="h-24 animate-pulse rounded-xl bg-muted" aria-hidden />}>
          <TopBrands />
        </Suspense>
      </section>
    </div>
  );
}

async function NewArrivals() {
  // Rendered per request (from cached data) so the build does not depend on the backend being up.
  await connection();
  const page = await getProducts({ inStock: true, sort: "newest", size: 8 });
  return <ProductGrid products={page.items} />;
}

async function TopBrands() {
  await connection();
  const brands = (await getBrands()).toSorted((a, b) => b.productCount - a.productCount).slice(0, 12);
  return (
    <ul className="flex flex-wrap gap-2">
      {brands.map((brand) => (
        <li key={brand.slug}>
          <Link href={`/brands/${brand.slug}`} className="block rounded-full border px-4 py-2 text-sm hover:bg-muted">
            {brand.name}
          </Link>
        </li>
      ))}
    </ul>
  );
}

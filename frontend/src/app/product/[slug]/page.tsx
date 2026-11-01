import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Suspense } from "react";

import { Price } from "@/components/price";
import { ProductGrid } from "@/components/product-card";
import { ProductGallery } from "@/components/product-gallery";
import { WhatsAppButton } from "@/components/whatsapp-button";
import { getProduct } from "@/lib/catalog-api";
import { productAvailabilityMessage, productOrderMessage } from "@/lib/whatsapp";

export async function generateMetadata(props: PageProps<"/product/[slug]">): Promise<Metadata> {
  const { slug } = await props.params;
  const product = await getProduct(slug);
  if (!product) return { title: "Товар не найден" };

  const title = product.seoTitle ?? (product.brand ? `${product.name} — ${product.brand.name}` : product.name);
  const description =
    product.seoDescription ?? `${product.name}: купить в Алматы в интернет-магазине SkinxYou. Цены, наличие, доставка.`;
  return {
    title,
    description,
    alternates: { canonical: `/product/${product.slug}` },
    openGraph: {
      title,
      description,
      type: "website",
      images: product.images[0] ? [{ url: product.images[0].url }] : undefined,
    },
  };
}

export default function ProductPage(props: PageProps<"/product/[slug]">) {
  return (
    <Suspense fallback={<ProductSkeleton />}>
      <ProductContent params={props.params} />
    </Suspense>
  );
}

async function ProductContent({ params }: Pick<PageProps<"/product/[slug]">, "params">) {
  const { slug } = await params;
  const product = await getProduct(slug);
  if (!product) notFound();

  return (
    <div className="flex flex-col gap-12">
      <nav aria-label="Навигация" className="text-sm text-muted-foreground">
        <Link href="/catalog" className="hover:underline">Каталог</Link>
        {product.brand && (
          <>
            {" / "}
            <Link href={`/brands/${product.brand.slug}`} className="hover:underline">{product.brand.name}</Link>
          </>
        )}
      </nav>

      <div className="grid gap-8 md:grid-cols-2">
        <ProductGallery images={product.images} name={product.name} />

        <div className="flex flex-col gap-4">
          {product.brand && (
            <Link href={`/brands/${product.brand.slug}`}
              className="text-sm uppercase tracking-wide text-muted-foreground hover:underline">
              {product.brand.name}
            </Link>
          )}
          <h1 className="text-2xl font-semibold leading-tight tracking-tight">{product.name}</h1>
          <Price price={product.price} promoPrice={product.promoPrice} className="text-2xl" />

          <p className={product.inStock ? "text-sm text-green-700 dark:text-green-500" : "text-sm text-muted-foreground"}>
            {product.inStock ? "В наличии" : "Нет в наличии"}
          </p>

          {product.inStock ? (
            <WhatsAppButton message={productOrderMessage(product)}>Заказать в WhatsApp</WhatsAppButton>
          ) : (
            <WhatsAppButton variant="outline" message={productAvailabilityMessage(product)}>
              Спросить о поступлении
            </WhatsAppButton>
          )}

          <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-sm">
            {product.sku && (
              <>
                <dt className="text-muted-foreground">Артикул</dt>
                <dd>{product.sku}</dd>
              </>
            )}
            {product.categories.length > 0 && (
              <>
                <dt className="text-muted-foreground">Категория</dt>
                <dd>
                  {product.categories.map((c, i) => (
                    <span key={c.slug}>
                      {i > 0 && ", "}
                      <Link href={`/catalog?category=${c.slug}`} className="hover:underline">{c.name}</Link>
                    </span>
                  ))}
                </dd>
              </>
            )}
          </dl>

          {product.description && (
            <div className="whitespace-pre-line text-sm leading-relaxed">{product.description}</div>
          )}
        </div>
      </div>

      {product.related.length > 0 && (
        <section className="flex flex-col gap-4">
          <h2 className="text-xl font-semibold tracking-tight">Похожие товары</h2>
          <ProductGrid products={product.related.slice(0, 4)} />
        </section>
      )}
    </div>
  );
}

function ProductSkeleton() {
  return (
    <div className="grid gap-8 md:grid-cols-2" aria-hidden>
      <div className="aspect-square animate-pulse rounded-xl bg-muted" />
      <div className="flex flex-col gap-4">
        <div className="h-4 w-24 animate-pulse rounded bg-muted" />
        <div className="h-8 w-4/5 animate-pulse rounded bg-muted" />
        <div className="h-7 w-32 animate-pulse rounded bg-muted" />
        <div className="h-11 w-full animate-pulse rounded-lg bg-muted" />
      </div>
    </div>
  );
}

import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Suspense } from "react";

import { AddToCartButton } from "@/components/cart/add-to-cart-button";
import { Price } from "@/components/price";
import { ProductGrid } from "@/components/product-card";
import { ProductGallery } from "@/components/product-gallery";
import { WhatsAppButton } from "@/components/whatsapp-button";
import { getCheckoutOptions, getProduct } from "@/lib/catalog-api";
import { formatPrice } from "@/lib/format";
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
    <div className="container-page py-8 lg:py-12">
      <Suspense fallback={<ProductSkeleton />}>
        <ProductContent params={props.params} />
      </Suspense>
    </div>
  );
}

async function ProductContent({ params }: Pick<PageProps<"/product/[slug]">, "params">) {
  const { slug } = await params;
  const [product, options] = await Promise.all([getProduct(slug), getCheckoutOptions()]);
  if (!product) notFound();

  return (
    <div className="flex flex-col gap-20">
      <div className="grid gap-8 lg:grid-cols-[1.1fr_0.9fr] lg:gap-16">
        <ProductGallery images={product.images} name={product.name} />

        <div className="flex flex-col gap-6 lg:pt-6">
          <nav aria-label="Навигация" className="eyebrow flex flex-wrap gap-2 text-muted-foreground">
            <Link href="/catalog" className="hover:text-foreground">Каталог</Link>
            {product.brand && (
              <>
                <span aria-hidden>/</span>
                <Link href={`/brands/${product.brand.slug}`} className="hover:text-foreground">{product.brand.name}</Link>
              </>
            )}
          </nav>

          <h1 className="font-serif text-4xl leading-[1.05] tracking-[-0.03em] sm:text-5xl">{product.name}</h1>

          <div className="flex flex-wrap items-center gap-x-5 gap-y-2">
            <Price price={product.price} promoPrice={product.promoPrice} className="text-2xl" />
            <span className={product.inStock ? "eyebrow text-forest" : "eyebrow text-muted-foreground"}>
              {product.inStock ? "● В наличии" : "Нет в наличии"}
            </span>
          </div>

          <div className="flex flex-col gap-3 border-y border-primary/15 py-6">
            {product.inStock ? (
              <>
                <AddToCartButton className="w-full"
                  product={{ slug: product.slug, name: product.name, imageUrl: product.images[0]?.url ?? null,
                    price: product.price, promoPrice: product.promoPrice }} />
                <WhatsAppButton variant="outline" message={productOrderMessage(product)} className="w-full">
                  Заказать в WhatsApp
                </WhatsAppButton>
                <p className="text-xs leading-5 text-muted-foreground">
                  Доставка по Алматы — {formatPrice(options.courierFee)}, бесплатно от{" "}
                  {formatPrice(options.freeCourierFrom)}. Самовывоз — бесплатно.
                </p>
              </>
            ) : (
              <>
                <WhatsAppButton variant="outline" message={productAvailabilityMessage(product)} className="w-full">
                  Спросить о поступлении
                </WhatsAppButton>
                <p className="text-xs leading-5 text-muted-foreground">
                  Напишите нам — сообщим, когда товар появится.
                </p>
              </>
            )}
          </div>

          <dl className="grid grid-cols-[auto_1fr] gap-x-6 gap-y-2 text-sm">
            {product.brand && (
              <>
                <dt className="text-muted-foreground">Бренд</dt>
                <dd><Link href={`/brands/${product.brand.slug}`} className="hover:underline">{product.brand.name}</Link></dd>
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
            {product.sku && (
              <>
                <dt className="text-muted-foreground">Артикул</dt>
                <dd>{product.sku}</dd>
              </>
            )}
          </dl>

          {product.description && (
            <div className="whitespace-pre-line text-sm leading-7 text-foreground/80">{product.description}</div>
          )}
        </div>
      </div>

      {product.related.length > 0 && (
        <section className="flex flex-col gap-8">
          <div>
            <p className="eyebrow mb-3">Вам может понравиться</p>
            <h2 className="section-title">Похожие товары.</h2>
          </div>
          <ProductGrid products={product.related.slice(0, 4)} />
        </section>
      )}
    </div>
  );
}

function ProductSkeleton() {
  return (
    <div className="grid gap-8 lg:grid-cols-[1.1fr_0.9fr] lg:gap-16" aria-hidden>
      <div className="aspect-[4/5] animate-pulse bg-image" />
      <div className="flex flex-col gap-5 lg:pt-6">
        <div className="h-3 w-32 animate-pulse bg-image" />
        <div className="h-12 w-4/5 animate-pulse bg-image" />
        <div className="h-7 w-32 animate-pulse bg-image" />
        <div className="h-13 w-full animate-pulse bg-image" />
      </div>
    </div>
  );
}

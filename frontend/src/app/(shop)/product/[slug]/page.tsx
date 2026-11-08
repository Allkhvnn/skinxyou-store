import { ChevronRight, CreditCard, Store, Truck, type LucideIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Suspense } from "react";

import { AddToCartButton } from "@/components/cart/add-to-cart-button";
import { Price } from "@/components/price";
import { ProductGrid } from "@/components/product-card";
import { ProductGallery } from "@/components/product-gallery";
import { ExpandableText, ProductTabs } from "@/components/product-tabs";
import { WhatsAppButton } from "@/components/whatsapp-button";
import { getCheckoutOptions, getProduct } from "@/lib/catalog-api";
import { categoryLabel, discountPercent, formatPrice } from "@/lib/format";
import { STORE } from "@/lib/store-info";
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
          <nav aria-label="Хлебные крошки" className="flex flex-wrap items-center gap-1.5 text-[0.8125rem] text-muted-foreground">
            <Link href="/catalog" className="hover:text-foreground">Каталог</Link>
            {product.categories[0] && (
              <>
                <ChevronRight className="size-3.5" aria-hidden />
                <Link href={`/catalog?category=${product.categories[0].slug}`} className="hover:text-foreground">{categoryLabel(product.categories[0].name)}</Link>
              </>
            )}
            {product.brand && (
              <>
                <ChevronRight className="size-3.5" aria-hidden />
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
          {discountPercent(product.price, product.promoPrice) != null && (
            <p className="-mt-3 text-sm text-destructive">
              Скидка {discountPercent(product.price, product.promoPrice)}% · вы экономите{" "}
              {formatPrice(product.price! - product.promoPrice!)}
            </p>
          )}

          <div className="flex flex-col gap-3 border-y border-primary/15 py-6">
            {product.inStock ? (
              <>
                <AddToCartButton className="w-full"
                  product={{ slug: product.slug, name: product.name, imageUrl: product.images[0]?.url ?? null,
                    price: product.price, promoPrice: product.promoPrice }} />
                <WhatsAppButton variant="outline" message={productOrderMessage(product)} className="w-full">
                  Заказать в WhatsApp
                </WhatsAppButton>
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

          <ProductTabs
            panels={[
              {
                label: "Описание",
                content: product.description ? (
                  <ExpandableText text={product.description} />
                ) : (
                  <div className="flex flex-col items-start gap-3 text-sm leading-6 text-muted-foreground">
                    <p>Подскажем состав, способ применения и подойдёт ли средство вашей коже.</p>
                    <WhatsAppButton variant="text" message={productAvailabilityMessage(product)}>
                      Спросить о товаре в WhatsApp
                    </WhatsAppButton>
                  </div>
                ),
              },
              {
                label: "Характеристики",
                content: (
                  <dl className="grid grid-cols-[auto_1fr] gap-x-6 gap-y-2 text-sm">
                    {product.brand && (
                      <>
                        <dt className="text-muted-foreground">Бренд</dt>
                        <dd><Link href={`/brands/${product.brand.slug}`} className="underline-offset-4 hover:underline">{product.brand.name}</Link></dd>
                      </>
                    )}
                    {product.categories.length > 0 && (
                      <>
                        <dt className="text-muted-foreground">Категория</dt>
                        <dd>
                          {product.categories.map((c, i) => (
                            <span key={c.slug}>
                              {i > 0 && ", "}
                              <Link href={`/catalog?category=${c.slug}`} className="underline-offset-4 hover:underline">{categoryLabel(c.name)}</Link>
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
                ),
              },
              {
                label: "Оплата и доставка",
                content: (
                  <ul className="flex flex-col gap-4 text-sm leading-6">
                    <InfoRow icon={Truck} title="Курьер по Алматы">
                      {formatPrice(options.courierFee)}, бесплатно от {formatPrice(options.freeCourierFrom)}
                    </InfoRow>
                    <InfoRow icon={Store} title="Самовывоз — бесплатно">
                      {STORE.pickupAddress}. {STORE.hours}
                    </InfoRow>
                    <InfoRow icon={CreditCard} title="Оплата через Kaspi">
                      После оформления менеджер отправит счёт в Kaspi
                    </InfoRow>
                  </ul>
                ),
              },
            ]}
          />
        </div>
      </div>

      {product.related.length > 0 && (
        <section className="flex flex-col gap-8">
          <div>
            <p className="eyebrow mb-3">Вам может понравиться</p>
            <h2 className="section-title">Рекомендуем также.</h2>
          </div>
          <ProductGrid products={product.related.slice(0, 4)} />
        </section>
      )}
    </div>
  );
}

function InfoRow({ icon: Icon, title, children }: { icon: LucideIcon; title: string; children: React.ReactNode }) {
  return (
    <li className="flex gap-3">
      <Icon className="mt-0.5 size-5 shrink-0 text-forest" strokeWidth={1.6} aria-hidden />
      <div>
        <p className="font-medium">{title}</p>
        <p className="text-muted-foreground">{children}</p>
      </div>
    </li>
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

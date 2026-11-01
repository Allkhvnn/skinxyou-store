import Image from "next/image";
import Link from "next/link";

import { Price } from "@/components/price";
import type { ProductSummary } from "@/lib/catalog-api";

export function ProductCard({ product }: { product: ProductSummary }) {
  return (
    <Link href={`/product/${product.slug}`} className="group block outline-none focus-visible:ring-2 focus-visible:ring-ring">
      <div className="relative aspect-[4/5] overflow-hidden bg-image">
        {product.imageUrl ? (
          <Image
            src={product.imageUrl}
            alt={product.name}
            fill
            sizes="(min-width: 1024px) 25vw, (min-width: 640px) 50vw, 50vw"
            className="object-cover transition duration-700 group-hover:scale-[1.04]"
          />
        ) : (
          <div className="flex h-full items-center justify-center text-xs uppercase tracking-widest text-muted-foreground">
            Нет фото
          </div>
        )}
        {!product.inStock ? (
          <span className="product-badge">Нет в наличии</span>
        ) : product.promoPrice != null ? (
          <span className="product-badge text-destructive">Скидка</span>
        ) : null}
      </div>
      <div className="pt-4">
        {product.brand && <p className="eyebrow mb-1.5 text-muted-foreground">{product.brand.name}</p>}
        <h3 className="line-clamp-2 font-serif text-lg leading-snug sm:text-xl">{product.name}</h3>
        <Price price={product.price} promoPrice={product.promoPrice} className="mt-1.5 block text-sm" />
      </div>
    </Link>
  );
}

export function ProductGrid({ products }: { products: ProductSummary[] }) {
  return (
    <ul className="grid grid-cols-2 gap-x-3 gap-y-10 sm:gap-x-5 lg:grid-cols-4">
      {products.map((product) => (
        <li key={product.slug}>
          <ProductCard product={product} />
        </li>
      ))}
    </ul>
  );
}

export function ProductGridSkeleton({ count = 8 }: { count?: number }) {
  return (
    <ul className="grid grid-cols-2 gap-x-3 gap-y-10 sm:gap-x-5 lg:grid-cols-4" aria-hidden>
      {Array.from({ length: count }, (_, i) => (
        <li key={i} className="flex flex-col gap-2">
          <div className="aspect-[4/5] animate-pulse bg-image" />
          <div className="mt-2 h-3 w-1/3 animate-pulse bg-image" />
          <div className="h-5 w-4/5 animate-pulse bg-image" />
          <div className="h-4 w-1/4 animate-pulse bg-image" />
        </li>
      ))}
    </ul>
  );
}

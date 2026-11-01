import Image from "next/image";
import Link from "next/link";

import { Price } from "@/components/price";
import type { ProductSummary } from "@/lib/catalog-api";

export function ProductCard({ product }: { product: ProductSummary }) {
  return (
    <Link
      href={`/product/${product.slug}`}
      className="group flex flex-col gap-2 rounded-xl outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
    >
      <div className="relative aspect-square overflow-hidden rounded-xl bg-muted">
        {product.imageUrl ? (
          <Image
            src={product.imageUrl}
            alt={product.name}
            fill
            sizes="(min-width: 1024px) 25vw, (min-width: 640px) 33vw, 50vw"
            className="object-cover transition-transform duration-300 group-hover:scale-105"
          />
        ) : (
          <div className="flex h-full items-center justify-center text-xs text-muted-foreground">Нет фото</div>
        )}
        {!product.inStock && (
          <span className="absolute left-2 top-2 rounded-md bg-background/90 px-2 py-0.5 text-xs font-medium">
            Нет в наличии
          </span>
        )}
        {product.inStock && product.promoPrice != null && (
          <span className="absolute left-2 top-2 rounded-md bg-destructive px-2 py-0.5 text-xs font-medium text-white">
            Скидка
          </span>
        )}
      </div>
      <div className="flex flex-col gap-1 px-0.5">
        {product.brand && (
          <span className="text-xs uppercase tracking-wide text-muted-foreground">{product.brand.name}</span>
        )}
        <span className="line-clamp-2 text-sm leading-snug">{product.name}</span>
        <Price price={product.price} promoPrice={product.promoPrice} />
      </div>
    </Link>
  );
}

export function ProductGrid({ products }: { products: ProductSummary[] }) {
  return (
    <ul className="grid grid-cols-2 gap-x-3 gap-y-6 sm:grid-cols-3 lg:grid-cols-4">
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
    <ul className="grid grid-cols-2 gap-x-3 gap-y-6 sm:grid-cols-3 lg:grid-cols-4" aria-hidden>
      {Array.from({ length: count }, (_, i) => (
        <li key={i} className="flex flex-col gap-2">
          <div className="aspect-square animate-pulse rounded-xl bg-muted" />
          <div className="h-3 w-1/3 animate-pulse rounded bg-muted" />
          <div className="h-4 w-4/5 animate-pulse rounded bg-muted" />
          <div className="h-4 w-1/4 animate-pulse rounded bg-muted" />
        </li>
      ))}
    </ul>
  );
}

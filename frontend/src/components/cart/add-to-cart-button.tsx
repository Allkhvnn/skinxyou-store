"use client";

import { Plus } from "lucide-react";

import { cart, cartDrawer } from "@/lib/cart-store";
import { cn } from "@/lib/utils";

type Product = {
  slug: string;
  name: string;
  imageUrl: string | null;
  price: number | null;
  promoPrice: number | null;
};

/** Adds one unit and opens the cart drawer, as in the design. */
export function AddToCartButton({ product, className }: { product: Product; className?: string }) {
  return (
    <button type="button" className={cn("primary-button", className)}
      onClick={() => {
        cart.add(product);
        cartDrawer.open();
      }}>
      В корзину <Plus className="size-4" strokeWidth={1.7} />
    </button>
  );
}

/** The design's hover "quick add" strip over a product card image. */
export function QuickAddButton({ product }: { product: Product }) {
  return (
    <button type="button" aria-label={`Добавить в корзину: ${product.name}`}
      className="absolute inset-x-3 bottom-3 flex min-h-12 items-center justify-center gap-2.5 bg-cream/94 text-[0.68rem] font-semibold uppercase tracking-[0.1em] backdrop-blur-sm transition-transform duration-250 sm:translate-y-[4.25rem] sm:group-hover:translate-y-0 sm:focus-visible:translate-y-0"
      onClick={() => {
        cart.add(product);
        cartDrawer.open();
      }}>
      В корзину <Plus className="size-4" strokeWidth={1.7} />
    </button>
  );
}

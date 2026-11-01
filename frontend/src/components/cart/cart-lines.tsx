"use client";

import { Minus, Plus } from "lucide-react";
import Image from "next/image";
import Link from "next/link";

import { cart, MAX_QUANTITY, unitPrice, type CartItem } from "@/lib/cart-store";
import { formatPrice } from "@/lib/format";

export function CartLines({ items, onNavigate }: { items: CartItem[]; onNavigate?: () => void }) {
  return (
    <ul>
      {items.map((item) => (
        <li key={item.slug} className="flex gap-4 border-b border-primary/10 py-4">
          <Link href={`/product/${item.slug}`} onClick={onNavigate}
            className="relative h-24 w-20 shrink-0 overflow-hidden bg-image">
            {item.imageUrl && <Image src={item.imageUrl} alt="" fill sizes="80px" className="object-cover" />}
          </Link>
          <div className="flex min-w-0 flex-1 flex-col justify-between gap-2 py-0.5">
            <Link href={`/product/${item.slug}`} onClick={onNavigate} className="line-clamp-2 font-serif text-lg leading-snug">
              {item.name}
            </Link>
            <div className="flex items-center justify-between gap-3">
              <QuantityStepper item={item} />
              <span className="text-sm">{formatPrice(unitPrice(item) * item.quantity)}</span>
            </div>
          </div>
          <button className="self-start text-xs text-muted-foreground underline" onClick={() => cart.remove(item.slug)}>
            Удалить
          </button>
        </li>
      ))}
    </ul>
  );
}

function QuantityStepper({ item }: { item: CartItem }) {
  return (
    <div className="flex h-9 items-center border border-input" role="group" aria-label={`Количество: ${item.name}`}>
      <button className="grid size-9 place-items-center disabled:opacity-30" aria-label="Уменьшить"
        disabled={item.quantity <= 1} onClick={() => cart.setQuantity(item.slug, item.quantity - 1)}>
        <Minus className="size-3.5" />
      </button>
      <span className="w-8 text-center text-sm tabular-nums" aria-live="polite">{item.quantity}</span>
      <button className="grid size-9 place-items-center disabled:opacity-30" aria-label="Увеличить"
        disabled={item.quantity >= MAX_QUANTITY} onClick={() => cart.setQuantity(item.slug, item.quantity + 1)}>
        <Plus className="size-3.5" />
      </button>
    </div>
  );
}

"use client";

import { ArrowRight } from "lucide-react";
import Link from "next/link";

import { CartLines } from "@/components/cart/cart-lines";
import { cartSubtotal, useCart } from "@/lib/cart-store";
import { formatPrice } from "@/lib/format";
import { cartOrderMessage, whatsappLink } from "@/lib/whatsapp";

export function CartPageContent() {
  const items = useCart();

  if (items.length === 0) {
    return (
      <div className="flex flex-col items-center gap-3 border border-primary/15 px-5 py-20 text-center">
        <p className="font-serif text-3xl">Корзина пока пуста.</p>
        <p className="text-sm text-muted-foreground">Добавьте товары из каталога.</p>
        <Link href="/catalog" className="primary-button mt-3">Перейти в каталог</Link>
      </div>
    );
  }

  return (
    <div className="grid gap-10 lg:grid-cols-[1fr_380px]">
      <CartLines items={items} />
      <aside className="h-fit border border-primary/15 p-6">
        <div className="flex justify-between font-serif text-2xl">
          <span>Сумма</span>
          <span>{formatPrice(cartSubtotal(items))}</span>
        </div>
        <p className="mt-2 text-xs text-muted-foreground">Доставка рассчитывается при оформлении заказа.</p>
        <Link href="/checkout" className="primary-button mt-6 w-full">
          Оформить заказ <ArrowRight className="size-4" strokeWidth={1.7} />
        </Link>
        <a href={whatsappLink(cartOrderMessage(items))} target="_blank" rel="noopener noreferrer"
          className="outline-button mt-3 w-full">
          Заказать в WhatsApp
        </a>
      </aside>
    </div>
  );
}

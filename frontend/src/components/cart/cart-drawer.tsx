"use client";

import { ArrowRight, ShoppingBag, X } from "lucide-react";
import Link from "next/link";
import { useEffect } from "react";

import { CartLines } from "@/components/cart/cart-lines";
import { cart, cartCount, cartDrawer, cartSubtotal, useCart, useCartDrawerOpen } from "@/lib/cart-store";
import { formatPrice } from "@/lib/format";
import { cartOrderMessage, whatsappLink } from "@/lib/whatsapp";

/** Header bag icon with item count; opens the drawer. */
export function CartButton() {
  const count = cartCount(useCart());
  return (
    <button className="icon-button relative" onClick={cartDrawer.open} aria-label={`Корзина, товаров: ${count}`}>
      <ShoppingBag className="size-5" strokeWidth={1.7} />
      {count > 0 && (
        <span className="absolute right-0.5 top-0.5 grid h-[1.1rem] min-w-[1.1rem] place-items-center rounded-full bg-destructive px-1 text-[0.6rem] text-white">
          {count}
        </span>
      )}
    </button>
  );
}

export function CartDrawer() {
  const open = useCartDrawerOpen();
  const items = useCart();

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && cartDrawer.close();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 bg-primary/40 backdrop-blur-[2px]" onClick={cartDrawer.close}>
      <aside role="dialog" aria-modal="true" aria-label="Корзина"
        className="absolute right-0 top-0 flex h-full w-full max-w-md flex-col bg-background p-6 shadow-2xl"
        onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between border-b border-primary/15 pb-5">
          <h2 className="font-serif text-3xl">
            Корзина <span className="text-lg">({cartCount(items)})</span>
          </h2>
          <button className="icon-button" onClick={cartDrawer.close} aria-label="Закрыть корзину">
            <X className="size-5" strokeWidth={1.7} />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto">
          {items.length === 0 ? (
            <div className="flex h-full flex-col items-center justify-center text-center">
              <ShoppingBag className="size-10 opacity-40" strokeWidth={1.4} />
              <p className="mt-4 font-serif text-2xl">Корзина пока пуста.</p>
              <p className="mt-2 text-sm text-muted-foreground">Добавьте в неё что-нибудь прекрасное.</p>
              <Link href="/catalog" onClick={cartDrawer.close} className="outline-button mt-6">Перейти в каталог</Link>
            </div>
          ) : (
            <CartLines items={items} onNavigate={cartDrawer.close} />
          )}
        </div>

        {items.length > 0 && (
          <div className="border-t border-primary/15 pt-5">
            <div className="flex justify-between font-serif text-xl">
              <span>Сумма</span>
              <span>{formatPrice(cartSubtotal(items))}</span>
            </div>
            <p className="mt-2 text-xs text-muted-foreground">Доставка рассчитывается при оформлении заказа.</p>
            <Link href="/checkout" onClick={cartDrawer.close} className="primary-button mt-5 w-full">
              Оформить заказ <ArrowRight className="size-4" strokeWidth={1.7} />
            </Link>
            <a href={whatsappLink(cartOrderMessage(items))} target="_blank" rel="noopener noreferrer"
              className="text-button mt-2 w-full justify-center border-b-0">
              Или заказать в WhatsApp
            </a>
            <button className="mt-1 w-full text-xs text-muted-foreground underline" onClick={() => cart.clear()}>
              Очистить корзину
            </button>
          </div>
        )}
      </aside>
    </div>
  );
}

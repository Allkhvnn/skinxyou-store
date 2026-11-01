import type { Metadata } from "next";

import { CartPageContent } from "@/components/cart/cart-page-content";

export const metadata: Metadata = {
  title: "Корзина",
  robots: { index: false, follow: true },
};

export default function CartPage() {
  return (
    <div className="container-page py-12 lg:py-16">
      <header className="mb-8">
        <p className="eyebrow mb-3">Магазин</p>
        <h1 className="page-title">Корзина</h1>
      </header>
      <CartPageContent />
    </div>
  );
}

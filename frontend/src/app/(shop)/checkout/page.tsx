import type { Metadata } from "next";
import { connection } from "next/server";
import { Suspense } from "react";

import { CheckoutForm } from "@/components/checkout/checkout-form";
import { getCheckoutOptions } from "@/lib/catalog-api";

export const metadata: Metadata = {
  title: "Оформление заказа",
  robots: { index: false, follow: false },
};

export default function CheckoutPage() {
  return (
    <div className="container-page py-12 lg:py-16">
      <header className="mb-8">
        <p className="eyebrow mb-3">Корзина</p>
        <h1 className="page-title">Оформление заказа</h1>
      </header>
      <Suspense fallback={<div className="h-96 animate-pulse bg-image" aria-hidden />}>
        <Checkout />
      </Suspense>
    </div>
  );
}

async function Checkout() {
  // Rendered per request (from cached data) so the build does not depend on the backend being up.
  await connection();
  return <CheckoutForm options={await getCheckoutOptions()} />;
}

import type { Metadata } from "next";
import { connection } from "next/server";
import { Suspense } from "react";

import { InfoPage } from "@/components/info-page";
import { getCheckoutOptions } from "@/lib/catalog-api";
import { formatPrice } from "@/lib/format";
import { STORE } from "@/lib/store-info";

export const metadata: Metadata = {
  title: "Доставка",
  description: "Доставка косметики курьером по Алматы и самовывоз в магазине SkinxYou.",
  alternates: { canonical: "/delivery" },
};

export default function DeliveryPage() {
  return (
    <InfoPage title="Доставка">
      <Suspense fallback={<div className="h-24 animate-pulse bg-image" aria-hidden />}>
        <Courier />
      </Suspense>
      <h2>Самовывоз</h2>
      <p>Бесплатно. Адрес: {STORE.pickupAddress}.</p>
      <p>{STORE.hours}. Менеджер сообщит, когда заказ будет готов к выдаче.</p>
    </InfoPage>
  );
}

async function Courier() {
  await connection();
  const options = await getCheckoutOptions();
  return (
    <>
      <h2>Курьер по Алматы</h2>
      <p>
        Стоимость — {formatPrice(options.courierFee)}. При заказе от {formatPrice(options.freeCourierFrom)} доставка
        бесплатная.
      </p>
      <p>Время доставки менеджер согласует с вами после оформления заказа.</p>
    </>
  );
}

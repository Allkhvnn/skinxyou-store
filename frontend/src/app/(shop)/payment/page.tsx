import type { Metadata } from "next";

import { InfoPage } from "@/components/info-page";

export const metadata: Metadata = {
  title: "Оплата",
  description: "Как оплатить заказ в интернет-магазине SkinxYou через Kaspi.",
  alternates: { canonical: "/payment" },
};

export default function PaymentPage() {
  return (
    <InfoPage title="Оплата">
      <p>Заказы оплачиваются через Kaspi.</p>
      <ol className="list-decimal space-y-2 pl-5">
        <li>Вы оформляете заказ на сайте или в WhatsApp.</li>
        <li>Менеджер проверяет наличие и отправляет счёт в Kaspi на номер, указанный в заказе.</li>
        <li>Вы оплачиваете счёт в приложении Kaspi.kz.</li>
        <li>Менеджер подтверждает оплату и передаёт заказ на доставку или выдачу.</li>
      </ol>
      <p>На сайте не нужно вводить данные карты.</p>
    </InfoPage>
  );
}

import type { Metadata } from "next";
import Link from "next/link";

import { InfoPage } from "@/components/info-page";
import { WhatsAppButton } from "@/components/whatsapp-button";

export const metadata: Metadata = {
  title: "Как сделать заказ",
  description: "Как заказать косметику в SkinxYou на сайте или через WhatsApp.",
  alternates: { canonical: "/how-to-order" },
};

export default function HowToOrderPage() {
  return (
    <InfoPage title="Как сделать заказ">
      <h2>На сайте</h2>
      <ol className="list-decimal space-y-2 pl-5">
        <li>Выберите товары в <Link href="/catalog" className="underline underline-offset-4">каталоге</Link> и добавьте их в корзину.</li>
        <li>Перейдите к оформлению, укажите имя, телефон и способ получения.</li>
        <li>Менеджер свяжется с вами и отправит счёт в Kaspi.</li>
        <li>После оплаты заказ доставит курьер, или вы заберёте его сами.</li>
      </ol>
      <h2>В WhatsApp</h2>
      <p>Напишите нам: поможем подобрать средства и оформим заказ в переписке.</p>
      <div><WhatsAppButton message="Здравствуйте! Хочу сделать заказ.">Написать в WhatsApp</WhatsAppButton></div>
    </InfoPage>
  );
}

import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";

import { WhatsAppButton } from "@/components/whatsapp-button";

export const metadata: Metadata = {
  title: "Заказ оформлен",
  robots: { index: false, follow: false },
};

export default function OrderSuccessPage(props: PageProps<"/order-success">) {
  return (
    <div className="container-page flex flex-col items-center py-20 text-center lg:py-28">
      <Suspense fallback={<h1 className="page-title">Спасибо за заказ!</h1>}>
        <Confirmation searchParams={props.searchParams} />
      </Suspense>
      <div className="mt-10 flex flex-wrap justify-center gap-3">
        <Link href="/catalog" className="primary-button">Продолжить покупки</Link>
      </div>
    </div>
  );
}

async function Confirmation({ searchParams }: Pick<PageProps<"/order-success">, "searchParams">) {
  const raw = (await searchParams).number;
  const number = typeof raw === "string" && /^SX-\d{1,10}$/.test(raw) ? raw : null;

  return (
    <>
      <p className="eyebrow mb-4">Заказ оформлен</p>
      <h1 className="page-title">Спасибо за заказ!</h1>
      {number && <p className="mt-6 font-serif text-3xl">№ {number}</p>}
      <p className="mt-6 max-w-lg leading-7 text-muted-foreground">
        Менеджер свяжется с вами, чтобы подтвердить заказ, и отправит счёт в Kaspi Pay. После оплаты передадим заказ
        в доставку или подготовим к самовывозу.
      </p>
      {number && (
        <WhatsAppButton variant="text" className="mt-6"
          message={`Здравствуйте! У меня вопрос по заказу ${number}.`}>
          Вопрос по заказу в WhatsApp
        </WhatsAppButton>
      )}
    </>
  );
}

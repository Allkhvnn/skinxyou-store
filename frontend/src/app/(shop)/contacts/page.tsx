import type { Metadata } from "next";

import { InfoPage } from "@/components/info-page";
import { WhatsAppButton } from "@/components/whatsapp-button";
import { STORE } from "@/lib/store-info";

export const metadata: Metadata = {
  title: "Контакты",
  description: "Телефон, WhatsApp, адрес и часы работы магазина косметики SkinxYou в Алматы.",
  alternates: { canonical: "/contacts" },
};

const MAP_URL = `https://2gis.kz/almaty/search/${encodeURIComponent("Курмангазы 111")}`;

export default function ContactsPage() {
  return (
    <InfoPage title="Контакты">
      <dl className="grid gap-x-8 gap-y-3 sm:grid-cols-[auto_1fr]">
        <dt className="text-muted-foreground">Телефон</dt>
        <dd><a href={STORE.phoneHref} className="underline underline-offset-4">{STORE.phone}</a></dd>
        <dt className="text-muted-foreground">Instagram</dt>
        <dd><a href={STORE.instagram} target="_blank" rel="noopener noreferrer" className="underline underline-offset-4">{STORE.instagramHandle}</a></dd>
        <dt className="text-muted-foreground">Часы работы</dt>
        <dd>{STORE.hours}</dd>
        <dt className="text-muted-foreground">Адрес</dt>
        <dd>
          {STORE.pickupAddress}{" "}
          <a href={MAP_URL} target="_blank" rel="noopener noreferrer" className="underline underline-offset-4">Открыть в 2ГИС</a>
        </dd>
      </dl>
      <div><WhatsAppButton message="Здравствуйте! У меня вопрос.">Написать в WhatsApp</WhatsAppButton></div>
    </InfoPage>
  );
}

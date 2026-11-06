import Link from "next/link";

import { STORE } from "@/lib/store-info";
import { whatsappLink } from "@/lib/whatsapp";

export function SiteFooter() {
  return (
    <footer className="bg-forest-deep px-5 py-12 text-cream lg:px-10">
      <div className="mx-auto grid max-w-[1380px] gap-10 md:grid-cols-[1.6fr_1fr_1fr]">
        <div>
          <p className="brand text-cream">skinxyou</p>
          <p className="mt-4 max-w-xs text-sm leading-6 text-cream/55">
            Уходовая и декоративная косметика в Алматы. Поможем подобрать уход под вашу кожу.
          </p>
        </div>
        <div>
          <p className="mb-4 text-xs font-semibold uppercase tracking-widest">Магазин</p>
          <div className="space-y-2 text-sm text-cream/55">
            <Link href="/catalog" className="block transition hover:text-cream">Каталог</Link>
            <Link href="/catalog?inStock=true" className="block transition hover:text-cream">В наличии</Link>
            <Link href="/brands" className="block transition hover:text-cream">Бренды</Link>
          </div>
        </div>
        <div>
          <p className="mb-4 text-xs font-semibold uppercase tracking-widest">Контакты</p>
          <div className="space-y-2 text-sm text-cream/55">
            <a href={STORE.phoneHref} className="block transition hover:text-cream">{STORE.phone}</a>
            <a href={whatsappLink()} target="_blank" rel="noopener noreferrer" className="block transition hover:text-cream">
              Написать в WhatsApp
            </a>
            <a href={STORE.instagram} target="_blank" rel="noopener noreferrer" className="block transition hover:text-cream">
              Instagram {STORE.instagramHandle}
            </a>
            <p>{STORE.hours}</p>
            <p>{STORE.pickupAddress}</p>
          </div>
        </div>
      </div>
      <div className="mx-auto mt-12 flex max-w-[1380px] flex-col justify-between gap-3 border-t border-white/10 pt-5 text-[11px] text-white/40 sm:flex-row">
        <p>© SkinxYou. Все права защищены.</p>
        <p>Оплата через Kaspi · Цены и наличие обновляются автоматически.</p>
      </div>
    </footer>
  );
}

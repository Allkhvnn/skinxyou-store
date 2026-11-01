import Link from "next/link";

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
            <a href={whatsappLink()} target="_blank" rel="noopener noreferrer" className="block transition hover:text-cream">
              WhatsApp: +7 706 605 97 13
            </a>
            <p>Алматы, Казахстан</p>
          </div>
        </div>
      </div>
      <div className="mx-auto mt-12 flex max-w-[1380px] flex-col justify-between gap-3 border-t border-white/10 pt-5 text-[11px] text-white/40 sm:flex-row">
        <p>© SkinxYou. Все права защищены.</p>
        <p>Цены и наличие обновляются автоматически.</p>
      </div>
    </footer>
  );
}

import Link from "next/link";

import { whatsappLink } from "@/lib/whatsapp";

export function SiteFooter() {
  return (
    <footer className="mt-16 border-t">
      <div className="mx-auto flex max-w-6xl flex-col gap-4 px-4 py-8 text-sm text-muted-foreground sm:flex-row sm:justify-between">
        <div className="flex flex-col gap-1">
          <span className="font-medium text-foreground">SkinxYou</span>
          <span>Уходовая и декоративная косметика, Алматы</span>
        </div>
        <nav className="flex flex-col gap-1">
          <Link href="/catalog" className="hover:underline">Каталог</Link>
          <Link href="/brands" className="hover:underline">Бренды</Link>
          <a href={whatsappLink()} target="_blank" rel="noopener noreferrer" className="hover:underline">
            WhatsApp: +7 706 605 97 13
          </a>
        </nav>
      </div>
    </footer>
  );
}

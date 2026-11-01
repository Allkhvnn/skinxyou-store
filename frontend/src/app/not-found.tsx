import Link from "next/link";

import { SiteFooter } from "@/components/site-footer";
import { SiteHeader } from "@/components/site-header";

// Unknown URLs render outside the (shop) layout, so the storefront chrome is added here.
export default function NotFound() {
  return (
    <>
      <SiteHeader />
      <main className="container-page flex flex-1 flex-col items-center gap-4 py-24 text-center">
        <p className="eyebrow">404</p>
        <h1 className="page-title">Страница не найдена.</h1>
        <Link href="/catalog" className="primary-button mt-4">Перейти в каталог</Link>
      </main>
      <SiteFooter />
    </>
  );
}

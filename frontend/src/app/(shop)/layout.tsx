import { SiteFooter } from "@/components/site-footer";
import { SiteHeader } from "@/components/site-header";

/** Storefront chrome: announcement bar, header with cart, footer. The admin panel has its own layout. */
export default function ShopLayout({ children }: LayoutProps<"/">) {
  return (
    <>
      <div className="bg-primary px-4 py-2.5 text-center text-[11px] font-medium uppercase tracking-[0.18em] text-primary-foreground">
        Доставка по Алматы · Заказ на сайте и в WhatsApp
      </div>
      <SiteHeader />
      <main className="flex-1">{children}</main>
      <SiteFooter />
    </>
  );
}

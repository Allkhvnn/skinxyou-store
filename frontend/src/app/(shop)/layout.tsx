import { Suspense } from "react";

import { AnnouncementBar } from "@/components/announcement-bar";
import { SiteFooter } from "@/components/site-footer";
import { SiteHeader } from "@/components/site-header";

/** Storefront chrome: announcement bar, header with cart, footer. The admin panel has its own layout. */
export default function ShopLayout({ children }: LayoutProps<"/">) {
  return (
    <>
      <Suspense fallback={<div className="h-9 bg-primary" aria-hidden />}>
        <AnnouncementBar />
      </Suspense>
      <SiteHeader />
      <main className="flex-1">{children}</main>
      <SiteFooter />
    </>
  );
}

import Link from "next/link";
import { Suspense } from "react";

import { logout } from "@/app/admin/actions";
import { requireSession } from "@/lib/admin/session";

export default function PanelLayout({ children }: LayoutProps<"/admin">) {
  return (
    <>
      <header className="border-b border-primary/10 bg-background">
        <div className="container-page flex h-16 items-center justify-between gap-4">
          <div className="flex items-baseline gap-6">
            <Link href="/admin/orders" className="brand text-2xl">skinxyou</Link>
            <nav className="flex gap-5">
              <Link href="/admin/orders" className="nav-link">Заказы</Link>
              <Link href="/admin/sync" className="nav-link">Синхронизация</Link>
            </nav>
          </div>
          <Suspense fallback={null}>
            <CurrentUser />
          </Suspense>
        </div>
      </header>
      <main className="container-page flex-1 py-8">{children}</main>
    </>
  );
}

async function CurrentUser() {
  const session = await requireSession();
  return (
    <form action={logout} className="flex items-center gap-4 text-sm">
      <span className="hidden text-muted-foreground sm:inline">{session.name}</span>
      <button type="submit" className="text-xs font-semibold uppercase tracking-widest underline">Выйти</button>
    </form>
  );
}

import Link from "next/link";

export function SiteHeader() {
  return (
    <header className="sticky top-0 z-40 border-b bg-background/95 backdrop-blur">
      <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-x-6 gap-y-2 px-4 py-3">
        <Link href="/" className="text-xl font-semibold tracking-tight">
          SkinxYou
        </Link>
        <nav className="flex gap-4 text-sm">
          <Link href="/catalog" className="hover:underline">Каталог</Link>
          <Link href="/brands" className="hover:underline">Бренды</Link>
        </nav>
        <form action="/catalog" method="get" role="search" className="order-last w-full sm:order-none sm:ml-auto sm:w-72">
          <label htmlFor="site-search" className="sr-only">Поиск товаров</label>
          <input
            id="site-search"
            name="q"
            type="search"
            maxLength={100}
            placeholder="Поиск: название, бренд, артикул"
            className="h-10 w-full rounded-lg border border-input bg-background px-3 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
          />
        </form>
      </div>
    </header>
  );
}

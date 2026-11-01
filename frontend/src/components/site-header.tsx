"use client";

import { Menu, Search, X } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";

import { CartButton, CartDrawer } from "@/components/cart/cart-drawer";

const NAV = [
  { href: "/catalog", label: "Каталог" },
  { href: "/catalog?inStock=true", label: "В наличии" },
  { href: "/brands", label: "Бренды" },
];

export function SiteHeader() {
  const [menuOpen, setMenuOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);

  useEffect(() => {
    if (!menuOpen && !searchOpen) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        setMenuOpen(false);
        setSearchOpen(false);
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [menuOpen, searchOpen]);

  return (
    <>
      <header className="sticky top-0 z-30 border-b border-primary/10 bg-background/95 backdrop-blur-md">
        <div className="container-page flex h-20 items-center justify-between">
          <button className="icon-button lg:hidden" onClick={() => setMenuOpen(true)} aria-label="Открыть меню">
            <Menu className="size-6" strokeWidth={1.7} />
          </button>
          <Link href="/" className="brand">skinxyou</Link>
          <nav className="hidden items-center gap-8 lg:flex" aria-label="Основная навигация">
            {NAV.map((item) => (
              <Link key={item.href} href={item.href} className="nav-link">
                {item.label}
              </Link>
            ))}
          </nav>
          <div className="flex items-center gap-1 sm:gap-3">
            <button className="icon-button" onClick={() => setSearchOpen(true)} aria-label="Поиск">
              <Search className="size-5" strokeWidth={1.7} />
            </button>
            <CartButton />
          </div>
        </div>
      </header>

      <CartDrawer />

      {menuOpen && (
        <div className="fixed inset-0 z-50 bg-background p-6 lg:hidden" role="dialog" aria-modal="true" aria-label="Меню">
          <div className="flex items-center justify-between">
            <span className="brand">skinxyou</span>
            <button className="icon-button" onClick={() => setMenuOpen(false)} aria-label="Закрыть меню">
              <X className="size-6" strokeWidth={1.7} />
            </button>
          </div>
          <nav className="mt-16 flex flex-col items-start gap-7">
            {NAV.map((item) => (
              <Link key={item.href} href={item.href} className="font-serif text-4xl" onClick={() => setMenuOpen(false)}>
                {item.label}
              </Link>
            ))}
          </nav>
        </div>
      )}

      {searchOpen && (
        <div className="fixed inset-0 z-50 bg-primary/50 p-4 backdrop-blur-sm" onClick={() => setSearchOpen(false)}>
          <div className="mx-auto mt-20 max-w-2xl bg-background p-6 sm:p-8" role="dialog" aria-modal="true"
            aria-label="Поиск" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between">
              <p className="font-serif text-3xl">Что вы ищете?</p>
              <button className="icon-button" onClick={() => setSearchOpen(false)} aria-label="Закрыть поиск">
                <X className="size-5" strokeWidth={1.7} />
              </button>
            </div>
            <form action="/catalog" method="get" role="search" className="mt-6 flex items-center border-b border-primary"
              onSubmit={() => setSearchOpen(false)}>
              <Search className="size-5" strokeWidth={1.7} />
              <input autoFocus name="q" type="search" maxLength={100} aria-label="Поиск товаров"
                className="w-full bg-transparent px-3 py-4 outline-none" placeholder="Название, бренд или артикул" />
            </form>
            <p className="mt-5 text-xs uppercase tracking-widest text-muted-foreground">
              Например: тонер · солнцезащитный крем · сыворотка
            </p>
          </div>
        </div>
      )}
    </>
  );
}

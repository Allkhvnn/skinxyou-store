import { categoryLabel } from "@/lib/format";
import { ArrowRight } from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { connection } from "next/server";
import { Suspense } from "react";

import { ProductGrid, ProductGridSkeleton } from "@/components/product-card";
import { WhatsAppButton } from "@/components/whatsapp-button";
import { BannerCarousel } from "@/components/banner-carousel";
import { getBanners, getBrands, getCategories, getProducts } from "@/lib/catalog-api";

// Placeholder mood photos from Unsplash (free for commercial use); to be replaced with own photos.
const HERO_IMAGE = "https://images.unsplash.com/photo-1739980737820-b6bb1a9b8456?auto=format&fit=crop&w=1400&q=88";
const STORY_IMAGE = "https://images.unsplash.com/photo-1653784097013-786a8965ea3b?auto=format&fit=crop&w=1200&q=85";

const CONSULT_MESSAGE = "Здравствуйте! Помогите, пожалуйста, подобрать уход.";

export default function Home() {
  return (
    <>
      <Suspense fallback={<div className="container-page pt-6 lg:pt-8"><div className="aspect-square animate-pulse bg-image md:aspect-[1600/560]" /></div>}>
        <Hero />
      </Suspense>

      <section id="shop" className="container-page py-20 lg:py-28">
        <div className="mb-10 flex flex-col justify-between gap-7 md:flex-row md:items-end">
          <div>
            <p className="eyebrow mb-3">Популярное</p>
            <h2 className="section-title">Красота в каждой капле.</h2>
          </div>
          <Suspense fallback={null}>
            <CategoryPills />
          </Suspense>
        </div>
        <Suspense fallback={<ProductGridSkeleton />}>
          <Popular />
        </Suspense>
        <div className="mt-12 flex justify-center">
          <Link href="/catalog" className="outline-button">
            Смотреть весь каталог <ArrowRight className="size-4" strokeWidth={1.7} />
          </Link>
        </div>
      </section>

      <Suspense fallback={null}>
        <NewArrivals />
      </Suspense>

      <section className="grid bg-forest text-cream lg:grid-cols-2">
        <div className="relative min-h-[420px] lg:min-h-[650px]">
          <Image src={STORY_IMAGE} alt="Косметика для ежедневного ухода" fill sizes="(min-width: 1024px) 50vw, 100vw"
            className="object-cover" />
        </div>
        <div className="flex items-center px-7 py-16 sm:px-12 lg:px-[8vw]">
          <div className="max-w-lg">
            <p className="eyebrow text-gold-light">Почему SkinxYou</p>
            <h2 className="mt-4 font-serif text-5xl leading-[1.04] tracking-[-0.03em] sm:text-6xl">
              Уход, подобранный для вас.
            </h2>
            <p className="mt-6 leading-7 text-cream/70">
              Мы собрали в одном месте любимые бренды уходовой и декоративной косметики. Не знаете, с чего начать?
              Напишите нам — поможем подобрать средства под ваш тип кожи и задачи.
            </p>
            <Suspense fallback={<div className="mt-9 h-[106px] border-y border-cream/20" />}>
              <StoreStats />
            </Suspense>
            <WhatsAppButton variant="light" message={CONSULT_MESSAGE} className="mt-8">
              Написать в WhatsApp
            </WhatsAppButton>
          </div>
        </div>
      </section>

      <section className="container-page py-20 lg:py-24">
        <div className="mb-8 flex items-end justify-between gap-4">
          <div>
            <p className="eyebrow mb-3">Бренды</p>
            <h2 className="section-title">Любимые марки.</h2>
          </div>
          <Link href="/brands" className="text-button hidden sm:inline-flex">Все бренды</Link>
        </div>
        <Suspense fallback={<div className="h-24 animate-pulse bg-image" aria-hidden />}>
          <TopBrands />
        </Suspense>
      </section>

      <section className="bg-blush px-5 py-20 text-center">
        <p className="eyebrow">Остались вопросы?</p>
        <h2 className="mx-auto mt-3 max-w-2xl font-serif text-4xl tracking-[-0.03em] sm:text-5xl">
          Напишите нам в WhatsApp.
        </h2>
        <p className="mx-auto mt-4 max-w-lg text-sm leading-6 text-foreground/70">
          Подскажем по наличию, составу и доставке по Алматы.
        </p>
        <WhatsAppButton message="Здравствуйте! У меня вопрос." className="mt-7">Написать</WhatsAppButton>
      </section>
    </>
  );
}

/** Banners from the admin panel replace the static hero; the hero stays as a fallback when there are none. */
async function Hero() {
  await connection();
  const banners = await getBanners();
  if (banners.length > 0) {
    return (
      <>
        <h1 className="sr-only">SkinxYou — уходовая и декоративная косметика в Алматы</h1>
        <BannerCarousel banners={banners} />
      </>
    );
  }
  return (
      <section className="grid lg:min-h-[calc(100vh-7.25rem)] lg:grid-cols-[minmax(0,0.88fr)_minmax(0,1.12fr)]">
        <div className="flex items-center bg-sand px-7 py-16 sm:px-12 lg:px-[8vw]">
          <div className="max-w-xl">
            <p className="eyebrow mb-5">Уход и макияж · Алматы</p>
            <h1 className="display-title">Сияние в чистом виде.</h1>
            <p className="mt-6 max-w-md text-base leading-7 text-[#526058] sm:text-lg">
              Уходовая и декоративная косметика проверенных брендов. Актуальные цены и наличие, заказ на сайте
              или в WhatsApp.
            </p>
            <div className="mt-9 flex flex-wrap gap-3">
              <Link href="/catalog" className="primary-button">
                Перейти в каталог <ArrowRight className="size-4" strokeWidth={1.7} />
              </Link>
              <WhatsAppButton variant="text" message={CONSULT_MESSAGE}>Подобрать уход</WhatsAppButton>
            </div>
          </div>
        </div>
        <div className="relative min-h-[460px] overflow-hidden lg:min-h-0">
          <Image src={HERO_IMAGE} alt="Средства для ухода за кожей" fill priority
            sizes="(min-width: 1024px) 56vw, 100vw" className="object-cover" />
          <div className="absolute bottom-6 right-6 max-w-[230px] bg-cream/92 p-5 backdrop-blur">
            <p className="eyebrow">Консультация</p>
            <p className="mt-2 font-serif text-2xl leading-tight">Подберём уход под вашу кожу.</p>
          </div>
        </div>
      </section>
  );
}

// Data sections render per request (from cached data) so the build does not depend on the backend.

async function Popular() {
  await connection();
  const page = await getProducts({ inStock: true, sort: "popular", size: 8 });
  return <ProductGrid products={page.items} />;
}

/** Hidden until something arrives after the first BILLZ import. */
async function NewArrivals() {
  await connection();
  const page = await getProducts({ inStock: true, onlyNew: true, sort: "newest", size: 4 });
  if (page.items.length === 0) return null;
  return (
    <section className="container-page pb-20 lg:pb-28">
      <div className="mb-10">
        <p className="eyebrow mb-3">Только что привезли</p>
        <h2 className="section-title">Новинки.</h2>
      </div>
      <ProductGrid products={page.items} />
    </section>
  );
}

async function CategoryPills() {
  await connection();
  const categories = (await getCategories()).toSorted((a, b) => b.productCount - a.productCount).slice(0, 4);
  if (categories.length === 0) return null;
  return (
    <nav aria-label="Категории" className="flex max-w-full gap-2 overflow-x-auto pb-2">
      {categories.map((c) => (
        <Link key={c.slug} href={`/catalog?category=${c.slug}`} className="category-pill">{categoryLabel(c.name)}</Link>
      ))}
    </nav>
  );
}

async function StoreStats() {
  await connection();
  const [brands, inStock] = await Promise.all([getBrands(), getProducts({ inStock: true, size: 1 })]);
  const stats = [
    { value: String(brands.length), label: "Брендов" },
    { value: String(inStock.totalItems), label: "В наличии" },
    { value: "Алматы", label: "Доставка" },
  ];
  return (
    <div className="mt-9 grid grid-cols-3 gap-4 border-y border-cream/20 py-6 text-center">
      {stats.map((s) => (
        <div key={s.label}>
          <strong className="font-serif text-3xl font-normal">{s.value}</strong>
          <span className="mt-1 block text-[10px] uppercase tracking-widest opacity-60">{s.label}</span>
        </div>
      ))}
    </div>
  );
}

async function TopBrands() {
  await connection();
  const brands = (await getBrands()).toSorted((a, b) => b.productCount - a.productCount).slice(0, 12);
  return (
    <ul className="flex flex-wrap gap-2">
      {brands.map((brand) => (
        <li key={brand.slug}>
          <Link href={`/brands/${brand.slug}`} className="category-pill block">{brand.name}</Link>
        </li>
      ))}
    </ul>
  );
}

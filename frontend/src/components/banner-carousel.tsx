"use client";

import { ChevronLeft, ChevronRight } from "lucide-react";
import { getImageProps } from "next/image";
import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";

import type { Banner } from "@/lib/catalog-api";

const AUTOPLAY_MS = 5000;

/**
 * Home page promo slider: native scroll-snap (swipe works on phones), arrows and dots,
 * autoplay that pauses on hover/focus and is off for people who prefer reduced motion.
 */
export function BannerCarousel({ banners }: { banners: Banner[] }) {
  const track = useRef<HTMLDivElement>(null);
  const [current, setCurrent] = useState(0);
  const [paused, setPaused] = useState(false);
  const count = banners.length;

  const goTo = useCallback((index: number) => {
    const el = track.current;
    if (!el) return;
    const next = (index + count) % count;
    el.scrollTo({ left: next * el.clientWidth, behavior: "smooth" });
  }, [count]);

  useEffect(() => {
    if (count < 2 || paused) return;
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
    const timer = window.setInterval(() => {
      if (document.visibilityState === "visible") goTo(current + 1);
    }, AUTOPLAY_MS);
    return () => window.clearInterval(timer);
  }, [count, paused, current, goTo]);

  return (
    <section aria-roledescription="карусель" aria-label="Акции и предложения" className="container-page pt-6 lg:pt-8"
      onMouseEnter={() => setPaused(true)} onMouseLeave={() => setPaused(false)}
      onFocus={() => setPaused(true)} onBlur={() => setPaused(false)}>
      <div className="relative">
        <div ref={track} className="flex snap-x snap-mandatory overflow-x-auto [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
          onScroll={(e) => {
            const el = e.currentTarget;
            setCurrent(Math.round(el.scrollLeft / el.clientWidth));
          }}>
          {banners.map((banner, i) => (
            <div key={banner.id} className="w-full shrink-0 snap-start" aria-roledescription="слайд"
              aria-label={`${i + 1} из ${count}`}>
              <Slide banner={banner} first={i === 0} />
            </div>
          ))}
        </div>

        {count > 1 && (
          <>
            <button type="button" aria-label="Предыдущий баннер" onClick={() => goTo(current - 1)}
              className="absolute top-1/2 left-3 hidden size-11 -translate-y-1/2 place-items-center rounded-full bg-cream/90 shadow-sm transition hover:bg-cream md:grid">
              <ChevronLeft className="size-5" strokeWidth={1.7} />
            </button>
            <button type="button" aria-label="Следующий баннер" onClick={() => goTo(current + 1)}
              className="absolute top-1/2 right-3 hidden size-11 -translate-y-1/2 place-items-center rounded-full bg-cream/90 shadow-sm transition hover:bg-cream md:grid">
              <ChevronRight className="size-5" strokeWidth={1.7} />
            </button>
          </>
        )}
      </div>

      {count > 1 && (
        <div className="mt-4 flex justify-center gap-1">
          {banners.map((banner, i) => (
            <button key={banner.id} type="button" onClick={() => goTo(i)} aria-label={`Баннер ${i + 1}`}
              aria-current={i === current ? "true" : undefined} className="grid size-6 place-items-center">
              <span className={`block h-2 rounded-full transition-all ${i === current ? "w-5 bg-primary" : "w-2 bg-primary/25"}`} />
            </button>
          ))}
        </div>
      )}
    </section>
  );
}

/** Art direction: a wide picture on desktop, the square one (or the wide one if absent) on phones. */
function Slide({ banner, first }: { banner: Banner; first: boolean }) {
  const common = { alt: banner.title, sizes: "(min-width: 1380px) 1300px, 100vw", quality: 85 };
  const { props: { srcSet: desktop } } = getImageProps({ ...common, src: banner.desktopImageUrl, width: 1600, height: 560 });
  const { props: { srcSet: mobile, ...img } } = getImageProps({
    ...common,
    src: banner.mobileImageUrl ?? banner.desktopImageUrl,
    width: banner.mobileImageUrl ? 800 : 1600,
    height: banner.mobileImageUrl ? 800 : 560,
    loading: first ? "eager" : "lazy",
    fetchPriority: first ? "high" : "auto",
  });

  const picture = (
    <picture>
      <source media="(min-width: 768px)" srcSet={desktop} />
      <source media="(max-width: 767px)" srcSet={mobile} />
      {/* eslint-disable-next-line jsx-a11y/alt-text -- alt comes from getImageProps */}
      <img {...img} className={`block h-auto w-full bg-image object-cover ${banner.mobileImageUrl ? "aspect-square md:aspect-[1600/560]" : "aspect-[1600/560]"}`} />
    </picture>
  );

  if (!banner.linkUrl) return picture;
  return banner.linkUrl.startsWith("/")
    ? <Link href={banner.linkUrl} className="block">{picture}</Link>
    : <a href={banner.linkUrl} target="_blank" rel="noopener noreferrer" className="block">{picture}</a>;
}

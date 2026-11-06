import { getCheckoutOptions } from "@/lib/catalog-api";
import { formatPrice } from "@/lib/format";
import { STORE } from "@/lib/store-info";

export async function AnnouncementBar() {
  const options = await getCheckoutOptions();
  return (
    <div className="bg-primary px-4 py-2.5 text-[11px] font-medium uppercase tracking-[0.18em] text-primary-foreground">
      <div className="mx-auto flex max-w-[1380px] items-center justify-center gap-6 sm:justify-between">
        <span className="hidden sm:inline">{STORE.hours}</span>
        <span className="text-center">Бесплатная доставка по Алматы от {formatPrice(options.freeCourierFrom)}</span>
        <a href={STORE.phoneHref} className="hidden whitespace-nowrap hover:underline sm:inline">{STORE.phone}</a>
      </div>
    </div>
  );
}

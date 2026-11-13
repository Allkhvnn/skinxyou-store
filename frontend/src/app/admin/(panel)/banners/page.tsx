import type { Metadata } from "next";
import { Suspense } from "react";

import { BannerEditor, NewBannerForm } from "@/components/admin/banner-editor";
import { listBanners } from "@/lib/admin/api";
import { requireSession } from "@/lib/admin/session";

export const metadata: Metadata = { title: "Баннеры" };

export default function BannersPage() {
  return (
    <div className="flex flex-col gap-8">
      <div>
        <h1 className="page-title">Баннеры</h1>
        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
          Листаются на главной вместо большого блока. Картинка для компьютера — 1600×560, для телефона — 800×800
          (если её нет, на телефоне покажется картинка для компьютера). JPEG, PNG или WebP до 5 МБ.
        </p>
      </div>
      <NewBannerForm />
      <Suspense fallback={<div className="h-96 animate-pulse bg-image" aria-hidden />}>
        <BannerList />
      </Suspense>
    </div>
  );
}

async function BannerList() {
  const session = await requireSession();
  const banners = await listBanners(session);
  if (banners.length === 0) {
    return <p className="text-sm text-muted-foreground">Баннеров пока нет. Пока их нет, на главной показывается обычный блок.</p>;
  }
  return (
    <ol className="flex flex-col gap-6">
      {banners.map((banner, i) => (
        <li key={banner.id}>
          <BannerEditor banner={banner} first={i === 0} last={i === banners.length - 1} />
        </li>
      ))}
    </ol>
  );
}

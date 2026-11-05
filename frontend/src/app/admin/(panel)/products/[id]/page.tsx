import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Suspense } from "react";

import { ProductContentForm } from "@/components/admin/product-content-form";
import { getProduct } from "@/lib/admin/api";
import { requireSession } from "@/lib/admin/session";
import { formatPrice } from "@/lib/format";

export const metadata: Metadata = { title: "Товар" };

export default function ProductPage(props: PageProps<"/admin/products/[id]">) {
  return (
    <Suspense fallback={<div className="h-96 animate-pulse bg-image" aria-hidden />}>
      <ProductEditor params={props.params} />
    </Suspense>
  );
}

async function ProductEditor({ params }: Pick<PageProps<"/admin/products/[id]">, "params">) {
  const session = await requireSession();
  const id = Number((await params).id);
  if (!Number.isInteger(id) || id <= 0) notFound();
  const p = await getProduct(session, id);

  return (
    <div className="flex flex-col gap-8">
      <div>
        <Link href="/admin/products" className="eyebrow text-muted-foreground hover:text-foreground">← Все товары</Link>
        <h1 className="mt-3 font-serif text-4xl leading-tight">{p.name}</h1>
        <Link href={`/product/${p.slug}`} target="_blank" className="mt-2 inline-block text-sm underline">
          Открыть на сайте
        </Link>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
        <section className="border border-primary/15 p-5">
          <h2 className="eyebrow mb-4">Контент сайта</h2>
          <ProductContentForm product={p} />
        </section>

        <aside className="flex h-fit flex-col gap-4 border border-primary/15 p-5 text-sm">
          <h2 className="eyebrow">Из BILLZ (только чтение)</h2>
          <div className="relative aspect-[4/5] overflow-hidden bg-image">
            {p.imageUrl && <Image src={p.imageUrl} alt={p.name} fill sizes="340px" className="object-cover" />}
          </div>
          <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1.5">
            <dt className="text-muted-foreground">Цена</dt>
            <dd>
              {p.price != null ? formatPrice(p.price) : "—"}
              {p.promoPrice != null && <span className="text-destructive"> · акция {formatPrice(p.promoPrice)}</span>}
            </dd>
            <dt className="text-muted-foreground">Остаток</dt><dd>{Number(p.stock)} шт.</dd>
            <dt className="text-muted-foreground">Бренд</dt><dd>{p.brand ?? "не указан"}</dd>
            <dt className="text-muted-foreground">Категории</dt><dd>{p.categories.join(", ") || "не указаны"}</dd>
            <dt className="text-muted-foreground">Артикул</dt><dd>{p.sku ?? "—"}</dd>
            <dt className="text-muted-foreground">Штрихкод</dt><dd>{p.barcode ?? "—"}</dd>
            <dt className="text-muted-foreground">В BILLZ</dt><dd>{p.availableInBillz ? "есть" : "удалён"}</dd>
          </dl>
          <p className="text-xs text-muted-foreground">Чтобы изменить эти данные, отредактируйте товар в BILLZ.</p>
        </aside>
      </div>
    </div>
  );
}

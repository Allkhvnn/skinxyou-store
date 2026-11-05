"use client";

import Image from "next/image";
import { useActionState } from "react";

import { managePhoto, type FormState } from "@/app/admin/actions";
import type { AdminProductDetails } from "@/lib/admin/api";

export function ProductPhotos({ product }: { product: AdminProductDetails }) {
  const [state, action, pending] = useActionState<FormState, FormData>(managePhoto, {});

  return (
    <div className="flex flex-col gap-4">
      {product.images.length === 0 ? (
        <p className="text-sm text-muted-foreground">Фото пока нет.</p>
      ) : (
        <ul className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          {product.images.map((image, index) => (
            <li key={image.id} className="flex flex-col gap-2">
              <div className="relative aspect-[4/5] overflow-hidden bg-image">
                <Image src={image.url} alt="" fill sizes="200px" className="object-cover" />
                {index === 0 && <span className="product-badge">Главное</span>}
              </div>
              <p className="text-xs text-muted-foreground">{image.source === "SITE" ? "Загружено на сайте" : "Из BILLZ"}</p>
              {image.source === "SITE" && (
                <form action={action} className="flex flex-wrap gap-x-3 gap-y-1 text-xs">
                  <input type="hidden" name="id" value={product.id} />
                  <input type="hidden" name="imageId" value={image.id} />
                  {index !== 0 && (
                    <button type="submit" name="op" value="main" disabled={pending} className="underline">
                      Сделать главным
                    </button>
                  )}
                  <button type="submit" name="op" value="delete" disabled={pending} className="text-destructive underline"
                    onClick={(e) => { if (!confirm("Удалить это фото?")) e.preventDefault(); }}>
                    Удалить
                  </button>
                </form>
              )}
            </li>
          ))}
        </ul>
      )}

      <form action={action} className="flex flex-wrap items-center gap-3">
        <input type="hidden" name="id" value={product.id} />
        <input type="hidden" name="op" value="upload" />
        <input type="file" name="file" accept="image/jpeg,image/png,image/webp" required
          className="text-sm file:mr-3 file:border file:border-primary file:bg-transparent file:px-3 file:py-2 file:text-xs file:font-semibold file:uppercase file:tracking-widest" />
        <button type="submit" disabled={pending} className="primary-button min-h-11 disabled:opacity-60">
          {pending ? "Загружаем…" : "Загрузить фото"}
        </button>
      </form>
      <p className="text-xs text-muted-foreground">
        JPEG, PNG или WebP до 5 МБ, лучше вертикальные 4:5. Свои фото показываются перед фото из BILLZ; первое — главное.
      </p>
      {state.error && <p role="alert" className="text-sm text-destructive">{state.error}</p>}
    </div>
  );
}

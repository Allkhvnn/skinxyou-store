import Image from "next/image";

/** Main photo plus a scrollable strip of the rest; no client JS needed. */
export function ProductGallery({ images, name }: { images: { url: string }[]; name: string }) {
  if (images.length === 0) {
    return (
      <div className="flex aspect-square items-center justify-center rounded-xl bg-muted text-sm text-muted-foreground">
        Нет фото
      </div>
    );
  }
  const [main, ...rest] = images;
  return (
    <div className="flex flex-col gap-3">
      <div className="relative aspect-square overflow-hidden rounded-xl bg-muted">
        <Image src={main.url} alt={name} fill priority sizes="(min-width: 768px) 50vw, 100vw" className="object-cover" />
      </div>
      {rest.length > 0 && (
        <ul className="flex gap-2 overflow-x-auto">
          {rest.map((image, i) => (
            <li key={image.url} className="relative aspect-square w-24 shrink-0 overflow-hidden rounded-lg bg-muted">
              <Image src={image.url} alt={`${name}, фото ${i + 2}`} fill sizes="96px" className="object-cover" />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

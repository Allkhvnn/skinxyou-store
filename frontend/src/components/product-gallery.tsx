import Image from "next/image";

/** Main photo plus a scrollable strip of the rest; no client JS needed. */
export function ProductGallery({ images, name }: { images: { url: string }[]; name: string }) {
  if (images.length === 0) {
    return (
      <div className="flex aspect-[4/5] items-center justify-center bg-image text-xs uppercase tracking-widest text-muted-foreground">
        Нет фото
      </div>
    );
  }
  const [main, ...rest] = images;
  return (
    <div className="flex flex-col gap-3">
      <div className="relative aspect-[4/5] overflow-hidden bg-image">
        <Image src={main.url} alt={name} fill priority sizes="(min-width: 1024px) 55vw, 100vw" className="object-cover" />
      </div>
      {rest.length > 0 && (
        <ul className="flex gap-3 overflow-x-auto">
          {rest.map((image, i) => (
            <li key={image.url} className="relative aspect-[4/5] w-24 shrink-0 overflow-hidden bg-image">
              <Image src={image.url} alt={`${name}, фото ${i + 2}`} fill sizes="96px" className="object-cover" />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

import Link from "next/link";

import { cn } from "@/lib/utils";

/** Plain links so every page is crawlable and works without JavaScript. */
export function Pagination({
  page,
  totalPages,
  hrefFor,
}: {
  page: number;
  totalPages: number;
  hrefFor: (page: number) => string;
}) {
  if (totalPages <= 1) return null;

  const pages = new Set([1, totalPages, page - 1, page, page + 1].filter((p) => p >= 1 && p <= totalPages));
  const sorted = [...pages].sort((a, b) => a - b);

  return (
    <nav aria-label="Страницы" className="flex flex-wrap items-center justify-center gap-1">
      {page > 1 && <PageLink href={hrefFor(page - 1)} label="←" ariaLabel="Предыдущая страница" />}
      {sorted.map((p, i) => (
        <span key={p} className="flex items-center gap-1">
          {i > 0 && p - sorted[i - 1] > 1 && <span className="px-1 text-muted-foreground">…</span>}
          <PageLink href={hrefFor(p)} label={String(p)} current={p === page} />
        </span>
      ))}
      {page < totalPages && <PageLink href={hrefFor(page + 1)} label="→" ariaLabel="Следующая страница" />}
    </nav>
  );
}

function PageLink({
  href,
  label,
  current,
  ariaLabel,
}: {
  href: string;
  label: string;
  current?: boolean;
  ariaLabel?: string;
}) {
  return (
    <Link
      href={href}
      aria-label={ariaLabel}
      aria-current={current ? "page" : undefined}
      className={cn(
        "flex h-9 min-w-9 items-center justify-center rounded-lg px-3 text-sm",
        current ? "bg-primary text-primary-foreground" : "hover:bg-muted",
      )}
    >
      {label}
    </Link>
  );
}

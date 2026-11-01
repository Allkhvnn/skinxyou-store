import { cn } from "@/lib/utils";
import { formatPrice } from "@/lib/format";

export function Price({
  price,
  promoPrice,
  className,
}: {
  price: number | null;
  promoPrice: number | null;
  className?: string;
}) {
  if (price == null) return null;
  if (promoPrice == null || promoPrice >= price) {
    return <span className={cn("font-medium", className)}>{formatPrice(price)}</span>;
  }
  return (
    <span className={cn("inline-flex flex-wrap items-baseline gap-x-2", className)}>
      <span className="font-medium text-destructive">{formatPrice(promoPrice)}</span>
      <s className="text-[0.85em] text-muted-foreground">{formatPrice(price)}</s>
    </span>
  );
}

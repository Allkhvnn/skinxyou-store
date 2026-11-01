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
    return <span className={cn("font-semibold", className)}>{formatPrice(price)}</span>;
  }
  return (
    <span className={cn("inline-flex flex-wrap items-baseline gap-x-2", className)}>
      <span className="font-semibold text-destructive">{formatPrice(promoPrice)}</span>
      <s className="text-sm text-muted-foreground">{formatPrice(price)}</s>
    </span>
  );
}

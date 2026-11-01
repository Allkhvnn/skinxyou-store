import { ORDER_STATUS_LABELS, PAYMENT_STATUS_LABELS } from "@/lib/admin/labels";
import { cn } from "@/lib/utils";

const ORDER_TONES: Record<string, string> = {
  NEW: "bg-blush text-foreground",
  CONFIRMED: "bg-sand text-foreground",
  SHIPPED: "bg-sand text-foreground",
  READY_FOR_PICKUP: "bg-sand text-foreground",
  COMPLETED: "bg-forest text-cream",
  CANCELLED: "bg-muted text-muted-foreground line-through",
};

const PAYMENT_TONES: Record<string, string> = {
  PENDING: "border border-destructive text-destructive",
  PAID: "bg-forest text-cream",
  CANCELLED: "bg-muted text-muted-foreground",
};

export function OrderStatusBadge({ status }: { status: string }) {
  return <Badge className={ORDER_TONES[status]}>{ORDER_STATUS_LABELS[status] ?? status}</Badge>;
}

export function PaymentStatusBadge({ status }: { status: string }) {
  return <Badge className={PAYMENT_TONES[status]}>{PAYMENT_STATUS_LABELS[status] ?? status}</Badge>;
}

function Badge({ className, children }: { className?: string; children: React.ReactNode }) {
  return (
    <span className={cn("inline-block whitespace-nowrap px-2 py-1 text-[0.65rem] font-semibold uppercase tracking-wider", className)}>
      {children}
    </span>
  );
}

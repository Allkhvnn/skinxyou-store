import { ArrowRight } from "lucide-react";

import { cn } from "@/lib/utils";
import { whatsappLink } from "@/lib/whatsapp";

const VARIANTS = {
  primary: "primary-button",
  outline: "outline-button",
  light: "light-button",
  text: "text-button",
} as const;

// whatsapp_click analytics will be attached here in the analytics stage.
export function WhatsAppButton({
  message,
  children,
  variant = "primary",
  className,
}: {
  message?: string;
  children: React.ReactNode;
  variant?: keyof typeof VARIANTS;
  className?: string;
}) {
  return (
    <a href={whatsappLink(message)} target="_blank" rel="noopener noreferrer" className={cn(VARIANTS[variant], className)}>
      {children}
      {variant !== "text" && <ArrowRight className="size-4" strokeWidth={1.7} />}
    </a>
  );
}

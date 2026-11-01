import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { whatsappLink } from "@/lib/whatsapp";

// whatsapp_click analytics will be attached here in the analytics stage.
export function WhatsAppButton({
  message,
  children,
  variant = "default",
  className,
}: {
  message?: string;
  children: React.ReactNode;
  variant?: "default" | "outline";
  className?: string;
}) {
  return (
    <a
      href={whatsappLink(message)}
      target="_blank"
      rel="noopener noreferrer"
      className={cn(
        buttonVariants({ variant, size: "lg" }),
        "h-11 px-4 text-base",
        variant === "default" && "bg-[#25D366] text-white hover:bg-[#1ebe5b]",
        className,
      )}
    >
      {children}
    </a>
  );
}

import type { Metadata } from "next";

export const metadata: Metadata = {
  title: { default: "Админка", template: "%s — Админка SkinxYou" },
  robots: { index: false, follow: false },
};

export default function AdminLayout({ children }: LayoutProps<"/admin">) {
  return <div className="flex min-h-full flex-1 flex-col bg-background">{children}</div>;
}

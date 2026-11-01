import type { Metadata, Viewport } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";

// cyrillic-ext covers Kazakh letters (ә, қ, ң, ө, ұ, ү, і).
const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin", "cyrillic", "cyrillic-ext"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin", "cyrillic"],
});

const siteUrl = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";

export const metadata: Metadata = {
  metadataBase: new URL(siteUrl),
  title: {
    default: "SkinxYou — косметика в Алматы",
    template: "%s — SkinxYou",
  },
  description: "Интернет-магазин уходовой и декоративной косметики в Алматы.",
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="ru"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col">{children}</body>
    </html>
  );
}

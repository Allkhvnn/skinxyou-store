import type { Metadata } from "next";

import { LoginForm } from "@/components/admin/login-form";

export const metadata: Metadata = { title: "Вход" };

export default function AdminLoginPage() {
  return (
    <main className="flex flex-1 items-center justify-center px-5 py-16">
      <div className="w-full max-w-sm">
        <p className="brand text-center">skinxyou</p>
        <p className="eyebrow mt-2 text-center text-muted-foreground">Панель управления</p>
        <LoginForm />
      </div>
    </main>
  );
}

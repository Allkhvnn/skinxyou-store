import Link from "next/link";

import { buttonVariants } from "@/components/ui/button";

export default function NotFound() {
  return (
    <div className="flex flex-col items-center gap-3 py-16 text-center">
      <h1 className="text-xl font-semibold">Страница не найдена</h1>
      <Link href="/catalog" className={buttonVariants()}>Перейти в каталог</Link>
    </div>
  );
}

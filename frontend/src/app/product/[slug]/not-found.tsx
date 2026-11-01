import Link from "next/link";

import { buttonVariants } from "@/components/ui/button";

export default function ProductNotFound() {
  return (
    <div className="flex flex-col items-center gap-3 py-16 text-center">
      <h1 className="text-xl font-semibold">Товар не найден</h1>
      <p className="text-sm text-muted-foreground">Возможно, он снят с продажи или ссылка устарела.</p>
      <Link href="/catalog" className={buttonVariants()}>Перейти в каталог</Link>
    </div>
  );
}

import Link from "next/link";

export default function ProductNotFound() {
  return (
    <div className="flex flex-col items-center gap-4 py-16 text-center">
      <h1 className="page-title">Товар не найден.</h1>
      <p className="text-sm text-muted-foreground">Возможно, он снят с продажи или ссылка устарела.</p>
      <Link href="/catalog" className="primary-button mt-4">Перейти в каталог</Link>
    </div>
  );
}

import Link from "next/link";

export default function NotFound() {
  return (
    <div className="container-page flex flex-col items-center gap-4 py-24 text-center">
      <p className="eyebrow">404</p>
      <h1 className="page-title">Страница не найдена.</h1>
      <Link href="/catalog" className="primary-button mt-4">Перейти в каталог</Link>
    </div>
  );
}

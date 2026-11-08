export function InfoPage({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <article className="container-page max-w-3xl py-12 lg:py-16">
      <p className="eyebrow mb-3">Покупателям</p>
      <h1 className="page-title">{title}</h1>
      <div className="mt-8 flex flex-col gap-5 text-[0.9375rem] leading-7 [&_h2]:mt-4 [&_h2]:font-serif [&_h2]:text-2xl">
        {children}
      </div>
    </article>
  );
}

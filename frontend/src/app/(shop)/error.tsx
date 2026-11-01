"use client";

import { useEffect } from "react";

export default function Error({ error, retry }: { error: unknown; retry: () => void }) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="container-page flex flex-col items-center gap-4 py-24 text-center">
      <h1 className="page-title">Не удалось загрузить страницу.</h1>
      <p className="max-w-sm text-sm text-muted-foreground">
        Попробуйте ещё раз через несколько секунд. Если ошибка повторяется, напишите нам в WhatsApp.
      </p>
      <button className="primary-button mt-4" onClick={() => retry()}>Попробовать снова</button>
    </div>
  );
}

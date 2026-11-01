"use client";

import { useEffect } from "react";

import { Button } from "@/components/ui/button";

export default function Error({ error, retry }: { error: unknown; retry: () => void }) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="flex flex-col items-center gap-3 py-16 text-center">
      <h1 className="text-xl font-semibold">Не удалось загрузить страницу</h1>
      <p className="max-w-sm text-sm text-muted-foreground">
        Попробуйте ещё раз через несколько секунд. Если ошибка повторяется, напишите нам в WhatsApp.
      </p>
      <Button onClick={() => retry()}>Попробовать снова</Button>
    </div>
  );
}

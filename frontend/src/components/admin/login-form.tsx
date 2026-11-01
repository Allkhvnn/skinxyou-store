"use client";

import { useActionState } from "react";

import { login, type FormState } from "@/app/admin/actions";

export function LoginForm() {
  const [state, action, pending] = useActionState<FormState, FormData>(login, {});
  return (
    <form action={action} className="mt-10 flex flex-col gap-4">
      <label className="flex flex-col gap-1.5 text-sm">
        Логин
        <input name="username" autoComplete="username" required maxLength={50} className="field"
          defaultValue={state.username} />
      </label>
      <label className="flex flex-col gap-1.5 text-sm">
        Пароль
        <input name="password" type="password" autoComplete="current-password" required maxLength={200} className="field" />
      </label>
      {state.error && <p role="alert" className="text-sm text-destructive">{state.error}</p>}
      <button type="submit" disabled={pending} className="primary-button mt-2 disabled:opacity-60">
        {pending ? "Входим…" : "Войти"}
      </button>
    </form>
  );
}

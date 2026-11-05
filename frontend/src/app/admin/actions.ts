"use server";

import { cookies } from "next/headers";
import { redirect } from "next/navigation";

import {
  changeOrderStatus,
  ORDER_STATUSES,
  PAYMENT_STATUSES,
  deleteProductImage,
  makeProductImageMain,
  saveProductContent,
  startSync,
  uploadProductImage,
  type OrderStatus,
  type PaymentStatus,
} from "@/lib/admin/api";
import { getSession, SESSION_COOKIE } from "@/lib/admin/session";

// Server Actions are reachable by direct POST: every action re-checks the session itself.

const API_BASE_URL = process.env.API_BASE_URL ?? "http://localhost:8080";

/** `username` is echoed back so the login field survives React's form reset after a failed attempt. */
export type FormState = { error?: string; username?: string };

export async function login(_prev: FormState, formData: FormData): Promise<FormState> {
  const username = String(formData.get("username") ?? "").trim();
  const password = String(formData.get("password") ?? "");
  const fail = (error: string): FormState => ({ error, username });
  if (!username || !password) return fail("Введите логин и пароль");

  let res: Response;
  try {
    res = await fetch(`${API_BASE_URL}/api/admin/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify({ username, password }),
      cache: "no-store",
    });
  } catch {
    return fail("Сервер недоступен. Попробуйте позже.");
  }
  if (res.status === 401 || res.status === 400) return fail("Неверный логин или пароль");
  if (res.status === 429) return fail("Слишком много попыток входа. Подождите 10 минут.");
  if (!res.ok) return fail("Не удалось войти. Попробуйте позже.");

  const { token, expiresAt } = (await res.json()) as { token: string; expiresAt: string };
  (await cookies()).set(SESSION_COOKIE, token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/admin",
    expires: new Date(expiresAt),
  });
  redirect("/admin/orders");
}

export async function logout(): Promise<void> {
  (await cookies()).delete({ name: SESSION_COOKIE, path: "/admin" });
  redirect("/admin/login");
}

export async function changeStatus(_prev: FormState, formData: FormData): Promise<FormState> {
  const session = await getSession();
  if (!session) redirect("/admin/login");

  const number = String(formData.get("number") ?? "");
  const kind = String(formData.get("kind") ?? "");
  const target = String(formData.get("target") ?? "");
  const version = Number(formData.get("version"));
  const note = String(formData.get("note") ?? "").trim().slice(0, 500) || undefined;
  if (!/^SX-\d{1,10}$/.test(number) || !Number.isInteger(version)) return { error: "Некорректный запрос" };

  let result;
  if (kind === "ORDER" && (ORDER_STATUSES as readonly string[]).includes(target)) {
    result = await changeOrderStatus(session, number, { status: target as OrderStatus, note, version });
  } else if (kind === "PAYMENT" && (PAYMENT_STATUSES as readonly string[]).includes(target)) {
    result = await changeOrderStatus(session, number, { paymentStatus: target as PaymentStatus, note, version });
  } else {
    return { error: "Некорректный статус" };
  }
  if (!result.ok) return { error: result.message };
  redirect(`/admin/orders/${number}`);
}

export async function saveProduct(_prev: FormState, formData: FormData): Promise<FormState & { saved?: boolean }> {
  const session = await getSession();
  if (!session) redirect("/admin/login");
  const id = Number(formData.get("id"));
  if (!Number.isInteger(id) || id <= 0) return { error: "Некорректный товар" };
  const text = (name: string) => String(formData.get(name) ?? "");
  const result = await saveProductContent(session, id, {
    description: text("description"),
    seoTitle: text("seoTitle"),
    seoDescription: text("seoDescription"),
    hidden: formData.get("hidden") === "on",
  });
  return result.ok ? { saved: true } : { error: result.message };
}

const MAX_PHOTO_BYTES = 5 * 1024 * 1024;

/** Upload, delete or make-main for a product photo; the clicked button's value picks the operation. */
export async function managePhoto(_prev: FormState, formData: FormData): Promise<FormState> {
  const session = await getSession();
  if (!session) redirect("/admin/login");
  const id = Number(formData.get("id"));
  if (!Number.isInteger(id) || id <= 0) return { error: "Некорректный товар" };
  const op = String(formData.get("op") ?? "");
  const imageId = Number(formData.get("imageId"));

  let result;
  if (op === "upload") {
    const file = formData.get("file");
    if (!(file instanceof File) || file.size === 0) return { error: "Выберите файл" };
    if (file.size > MAX_PHOTO_BYTES) return { error: "Фото должно быть не больше 5 МБ" };
    result = await uploadProductImage(session, id, file);
  } else if ((op === "delete" || op === "main") && Number.isInteger(imageId) && imageId > 0) {
    result = op === "delete"
      ? await deleteProductImage(session, id, imageId)
      : await makeProductImageMain(session, id, imageId);
  } else {
    return { error: "Некорректный запрос" };
  }
  if (!result.ok) return { error: result.message };
  redirect(`/admin/products/${id}`);
}

export async function runSync(_prev: FormState, formData: FormData): Promise<FormState> {
  const session = await getSession();
  if (!session) redirect("/admin/login");
  const type = formData.get("type");
  if (type !== "DELTA" && type !== "FULL") return { error: "Некорректный тип синхронизации" };
  const result = await startSync(session, type);
  if (!result.ok) return { error: result.message };
  redirect("/admin/sync");
}

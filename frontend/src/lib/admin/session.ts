import "server-only";

import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { connection } from "next/server";

/** HttpOnly cookie holding the backend-issued admin token; scripts in the page cannot read it. */
export const SESSION_COOKIE = "sx_admin";

export type AdminSession = {
  token: string;
  username: string;
  name: string;
  role: "ADMIN" | "MANAGER";
};

/**
 * Reads the token's claims for display and expiry only. The signature is verified by the backend
 * on every API call, so a forged cookie gets nothing but a redirect to the login page.
 */
export async function getSession(): Promise<AdminSession | null> {
  // Token expiry must be checked against the time of the actual request.
  await connection();
  const token = (await cookies()).get(SESSION_COOKIE)?.value;
  if (!token) return null;
  try {
    const payload = JSON.parse(Buffer.from(token.split(".")[1], "base64url").toString("utf8"));
    if (typeof payload.exp !== "number" || payload.exp * 1000 <= Date.now()) return null;
    return { token, username: payload.sub, name: payload.name ?? payload.sub, role: payload.role };
  } catch {
    return null;
  }
}

export async function requireSession(): Promise<AdminSession> {
  const session = await getSession();
  if (!session) redirect("/admin/login");
  return session;
}

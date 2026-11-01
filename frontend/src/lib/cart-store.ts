"use client";

import { useSyncExternalStore } from "react";

/**
 * Browser-side cart (localStorage). Prices here are display snapshots only: checkout always
 * re-prices the cart on the backend, which is the single source of truth.
 */
export type CartItem = {
  slug: string;
  name: string;
  imageUrl: string | null;
  price: number | null;
  promoPrice: number | null;
  quantity: number;
};

const STORAGE_KEY = "skinxyou.cart.v1";
export const MAX_QUANTITY = 99;
const MAX_LINES = 50;
const EMPTY: CartItem[] = [];

let items: CartItem[] | null = null;
const listeners = new Set<() => void>();

function isCartItem(value: unknown): value is CartItem {
  const v = value as CartItem;
  return typeof v?.slug === "string" && typeof v.name === "string" && Number.isInteger(v.quantity) && v.quantity > 0;
}

function read(): CartItem[] {
  if (items) return items;
  try {
    const parsed: unknown = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? "[]");
    items = Array.isArray(parsed) ? parsed.filter(isCartItem).slice(0, MAX_LINES) : [];
  } catch {
    items = [];
  }
  return items;
}

function write(next: CartItem[]) {
  items = next;
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
  } catch {
    // Private mode or full storage: the cart still works for this page view.
  }
  listeners.forEach((listener) => listener());
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  const onStorage = (event: StorageEvent) => {
    if (event.key === STORAGE_KEY) {
      items = null; // another tab changed the cart
      listener();
    }
  };
  window.addEventListener("storage", onStorage);
  return () => {
    listeners.delete(listener);
    window.removeEventListener("storage", onStorage);
  };
}

export function useCart(): CartItem[] {
  return useSyncExternalStore(subscribe, read, () => EMPTY);
}

const clamp = (quantity: number) => Math.min(Math.max(Math.floor(quantity), 1), MAX_QUANTITY);

export const cart = {
  add(product: Omit<CartItem, "quantity">, quantity = 1) {
    // Store only cart fields, whatever richer object the caller passes.
    const { slug, name, imageUrl, price, promoPrice } = product;
    const item = { slug, name, imageUrl, price, promoPrice };
    const current = read();
    const existing = current.find((i) => i.slug === item.slug);
    if (existing) {
      write(current.map((i) => (i.slug === item.slug ? { ...i, ...item, quantity: clamp(i.quantity + quantity) } : i)));
    } else if (current.length < MAX_LINES) {
      write([...current, { ...item, quantity: clamp(quantity) }]);
    }
  },
  setQuantity(slug: string, quantity: number) {
    write(read().map((i) => (i.slug === slug ? { ...i, quantity: clamp(quantity) } : i)));
  },
  remove(slug: string) {
    write(read().filter((i) => i.slug !== slug));
  },
  clear() {
    write([]);
  },
};

export function unitPrice(item: Pick<CartItem, "price" | "promoPrice">): number {
  return item.promoPrice ?? item.price ?? 0;
}

export function cartCount(list: CartItem[]): number {
  return list.reduce((sum, i) => sum + i.quantity, 0);
}

export function cartSubtotal(list: CartItem[]): number {
  return list.reduce((sum, i) => sum + unitPrice(i) * i.quantity, 0);
}

// --- Drawer visibility, shared by the header, add-to-cart buttons and the drawer itself ---

let drawerOpen = false;
const drawerListeners = new Set<() => void>();

export const cartDrawer = {
  open() {
    drawerOpen = true;
    drawerListeners.forEach((l) => l());
  },
  close() {
    drawerOpen = false;
    drawerListeners.forEach((l) => l());
  },
};

export function useCartDrawerOpen(): boolean {
  return useSyncExternalStore(
    (listener) => {
      drawerListeners.add(listener);
      return () => drawerListeners.delete(listener);
    },
    () => drawerOpen,
    () => false,
  );
}

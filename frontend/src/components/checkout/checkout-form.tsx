"use client";

import { ArrowRight } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { cart, useCart } from "@/lib/cart-store";
import type { CheckoutOptions } from "@/lib/catalog-api";
import {
  fetchQuote,
  isValidKzPhone,
  placeOrder,
  type DeliveryMethod,
  type Quote,
  type QuoteProblem,
} from "@/lib/checkout-api";
import { formatPrice } from "@/lib/format";
import { cn } from "@/lib/utils";

type FieldErrors = Partial<Record<"name" | "phone" | "address" | "consent", string>>;

export function CheckoutForm({ options }: { options: CheckoutOptions }) {
  const router = useRouter();
  const items = useCart();
  const [deliveryMethod, setDeliveryMethod] = useState<DeliveryMethod>("COURIER");
  const [quote, setQuote] = useState<Quote | null>(null);
  const [quoteFailed, setQuoteFailed] = useState(false);
  const [errors, setErrors] = useState<FieldErrors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [stockProblems, setStockProblems] = useState<QuoteProblem[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [placed, setPlaced] = useState(false);
  // One key per checkout attempt: retries (double click, network error) cannot create a second order.
  const idempotencyKey = useRef<string | null>(null);

  const cartKey = items.map((i) => `${i.slug}:${i.quantity}`).join(",");

  useEffect(() => {
    if (cartKey === "") return;
    let cancelled = false;
    fetchQuote(
      cartKey.split(",").map((entry) => {
        const [slug, quantity] = entry.split(":");
        return { slug, quantity: Number(quantity) };
      }),
      deliveryMethod,
    )
      .then((q) => {
        if (cancelled) return;
        setQuote(q);
        setQuoteFailed(false);
        setStockProblems(q.problems);
      })
      .catch(() => !cancelled && setQuoteFailed(true));
    return () => {
      cancelled = true;
    };
  }, [cartKey, deliveryMethod]);

  if (placed) {
    return <p className="text-sm text-muted-foreground">Оформляем заказ…</p>;
  }

  if (items.length === 0) {
    return (
      <div className="flex flex-col items-center gap-3 border border-primary/15 px-5 py-20 text-center">
        <p className="font-serif text-3xl">Корзина пуста.</p>
        <Link href="/catalog" className="primary-button mt-3">Перейти в каталог</Link>
      </div>
    );
  }

  async function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting) return;
    const data = new FormData(event.currentTarget);
    const name = String(data.get("name") ?? "").trim();
    const phone = String(data.get("phone") ?? "").trim();
    const address = String(data.get("address") ?? "").trim();
    const comment = String(data.get("comment") ?? "").trim();
    const consent = data.get("consent") === "on";

    const nextErrors: FieldErrors = {};
    if (!name) nextErrors.name = "Укажите имя";
    if (!isValidKzPhone(phone)) nextErrors.phone = "Укажите номер в формате +7 7XX XXX XX XX";
    if (deliveryMethod === "COURIER" && !address) nextErrors.address = "Укажите адрес доставки";
    if (!consent) nextErrors.consent = "Нужно согласие на обработку персональных данных";
    setErrors(nextErrors);
    setFormError(null);
    if (Object.keys(nextErrors).length > 0) return;

    setSubmitting(true);
    idempotencyKey.current ??= crypto.randomUUID();
    const result = await placeOrder({
      idempotencyKey: idempotencyKey.current,
      name,
      phone,
      deliveryMethod,
      address: deliveryMethod === "COURIER" ? address : undefined,
      comment: comment || undefined,
      consent,
      items: items.map((i) => ({ slug: i.slug, quantity: i.quantity })),
    });
    setSubmitting(false);

    if (result.ok) {
      setPlaced(true);
      cart.clear();
      router.push(`/order-success?number=${encodeURIComponent(result.order.number)}`);
      return;
    }
    if (result.kind === "stock") {
      setStockProblems(result.problems);
      setFormError("Некоторых товаров нет в нужном количестве. Исправьте корзину и оформите заказ снова.");
    } else if (result.kind === "validation") {
      setErrors({
        name: result.fields.name,
        phone: result.fields.phone,
        address: result.fields.addressPresentForCourier ?? result.fields.address,
        consent: result.fields.consent,
      });
      setFormError(result.message);
    } else {
      setFormError(result.message);
    }
  }

  const courierFree = quote != null && quote.itemsTotal >= options.freeCourierFrom;

  return (
    <form onSubmit={onSubmit} noValidate className="grid gap-10 lg:grid-cols-[1fr_400px]"
      onChange={(event) => {
        // Hide a field's error as soon as the customer edits it.
        const target = event.target as unknown as HTMLInputElement;
        const field = target.name as keyof FieldErrors;
        if (errors[field]) setErrors((current) => ({ ...current, [field]: undefined }));
      }}>
      <div className="flex flex-col gap-10">
        <fieldset className="flex flex-col gap-4">
          <legend className="eyebrow mb-4">Контакты</legend>
          <Field label="Имя" error={errors.name}>
            <input name="name" autoComplete="name" maxLength={100} className="field" aria-invalid={!!errors.name} />
          </Field>
          <Field label="Телефон" error={errors.phone} hint="Менеджер позвонит или напишет для подтверждения заказа">
            <input name="phone" type="tel" inputMode="tel" autoComplete="tel" maxLength={30}
              placeholder="+7 7__ ___ __ __" className="field" aria-invalid={!!errors.phone} />
          </Field>
        </fieldset>

        <fieldset className="flex flex-col gap-3">
          <legend className="eyebrow mb-4">Доставка</legend>
          <DeliveryOption checked={deliveryMethod === "COURIER"} onSelect={() => setDeliveryMethod("COURIER")}
            title="Курьером по Алматы"
            price={courierFree ? "Бесплатно" : formatPrice(options.courierFee)}
            note={`Бесплатно при заказе от ${formatPrice(options.freeCourierFrom)}`} />
          <DeliveryOption checked={deliveryMethod === "PICKUP"} onSelect={() => setDeliveryMethod("PICKUP")}
            title="Самовывоз" price="Бесплатно" note={options.pickupAddress} />
          {deliveryMethod === "COURIER" && (
            <Field label="Адрес доставки" error={errors.address}>
              <input name="address" autoComplete="street-address" maxLength={500}
                placeholder="Улица, дом, квартира" className="field" aria-invalid={!!errors.address} />
            </Field>
          )}
        </fieldset>

        <fieldset className="flex flex-col gap-3">
          <legend className="eyebrow mb-4">Оплата</legend>
          <div className="border border-primary p-4 text-sm leading-6">
            <p className="font-medium">Kaspi Pay</p>
            <p className="text-muted-foreground">
              После подтверждения заказа менеджер отправит вам счёт в Kaspi. Заказ передаётся в доставку после оплаты.
            </p>
          </div>
        </fieldset>

        <Field label="Комментарий к заказу (необязательно)">
          <textarea name="comment" rows={3} maxLength={1000} className="field h-auto py-2" />
        </Field>
      </div>

      <aside className="h-fit border border-primary/15 p-6 lg:sticky lg:top-28">
        <h2 className="font-serif text-2xl">Ваш заказ</h2>

        {quoteFailed && !quote && (
          <p className="mt-4 text-sm text-destructive">Не удалось обновить цены. Проверьте соединение.</p>
        )}

        <ul className="mt-4 flex flex-col gap-3 text-sm">
          {(quote?.lines ?? []).map((line) => (
            <li key={line.slug} className="flex justify-between gap-4">
              <span className="min-w-0">{line.name} <span className="text-muted-foreground">× {line.quantity}</span></span>
              <span className="shrink-0">{formatPrice(line.lineTotal)}</span>
            </li>
          ))}
        </ul>

        {stockProblems.length > 0 && <StockProblems problems={stockProblems} />}

        {quote && (
          <dl className="mt-5 flex flex-col gap-1.5 border-t border-primary/15 pt-4 text-sm">
            <div className="flex justify-between"><dt>Товары</dt><dd>{formatPrice(quote.itemsTotal)}</dd></div>
            <div className="flex justify-between">
              <dt>Доставка</dt>
              <dd>{quote.deliveryFee === 0 ? "Бесплатно" : formatPrice(quote.deliveryFee)}</dd>
            </div>
            {deliveryMethod === "COURIER" && !courierFree && (
              <p className="text-xs text-muted-foreground">
                До бесплатной доставки: {formatPrice(options.freeCourierFrom - quote.itemsTotal)}
              </p>
            )}
            <div className="mt-2 flex justify-between font-serif text-2xl">
              <dt>Итого</dt><dd>{formatPrice(quote.total)}</dd>
            </div>
          </dl>
        )}

        <label className="mt-6 flex gap-3 text-xs leading-5">
          <input type="checkbox" name="consent" className="mt-0.5 size-4 shrink-0 accent-primary"
            aria-invalid={!!errors.consent} />
          <span>
            Я согласен(на) на обработку персональных данных в соответствии с{" "}
            <Link href="/privacy" target="_blank" className="underline">политикой конфиденциальности</Link>
          </span>
        </label>
        {errors.consent && <p className="mt-1 text-xs text-destructive">{errors.consent}</p>}

        {formError && <p role="alert" className="mt-4 text-sm text-destructive">{formError}</p>}

        <button type="submit" disabled={submitting || stockProblems.length > 0}
          className="primary-button mt-6 w-full disabled:cursor-not-allowed disabled:opacity-50">
          {submitting ? "Оформляем…" : "Подтвердить заказ"} <ArrowRight className="size-4" strokeWidth={1.7} />
        </button>
      </aside>
    </form>
  );
}

function Field({ label, error, hint, children }: {
  label: string;
  error?: string;
  hint?: string;
  children: React.ReactNode;
}) {
  return (
    <label className="flex flex-col gap-1.5">
      <span className="text-sm">{label}</span>
      {children}
      {error ? <span className="text-xs text-destructive">{error}</span>
        : hint ? <span className="text-xs text-muted-foreground">{hint}</span> : null}
    </label>
  );
}

function DeliveryOption({ checked, onSelect, title, price, note }: {
  checked: boolean;
  onSelect: () => void;
  title: string;
  price: string;
  note: string;
}) {
  return (
    <label className={cn("flex cursor-pointer items-start gap-3 border p-4 transition",
      checked ? "border-primary" : "border-input hover:border-primary/50")}>
      <input type="radio" name="deliveryMethod" checked={checked} onChange={onSelect} className="mt-1 accent-primary" />
      <span className="flex flex-1 flex-col gap-0.5">
        <span className="flex justify-between gap-3 text-sm font-medium"><span>{title}</span><span>{price}</span></span>
        <span className="text-xs text-muted-foreground">{note}</span>
      </span>
    </label>
  );
}

function StockProblems({ problems }: { problems: QuoteProblem[] }) {
  return (
    <div role="alert" className="mt-4 border border-destructive/40 bg-destructive/5 p-3 text-sm">
      <ul className="flex flex-col gap-1">
        {problems.map((p) => (
          <li key={p.slug}>
            {p.name ?? "Товар"}:{" "}
            {p.reason === "NOT_AVAILABLE" || p.available === 0 ? "нет в наличии" : `в наличии только ${p.available} шт.`}
          </li>
        ))}
      </ul>
      <button type="button" className="mt-3 text-xs font-semibold uppercase tracking-widest underline"
        onClick={() => problems.forEach((p) =>
          p.reason === "NOT_AVAILABLE" || p.available === 0 ? cart.remove(p.slug) : cart.setQuantity(p.slug, p.available))}>
        Исправить корзину
      </button>
    </div>
  );
}

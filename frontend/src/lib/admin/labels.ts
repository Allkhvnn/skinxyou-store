export const ORDER_STATUS_LABELS: Record<string, string> = {
  NEW: "Новый",
  CONFIRMED: "Подтверждён",
  SHIPPED: "Передан курьеру",
  READY_FOR_PICKUP: "Готов к выдаче",
  COMPLETED: "Выполнен",
  CANCELLED: "Отменён",
};

export const PAYMENT_STATUS_LABELS: Record<string, string> = {
  PENDING: "Ожидает оплаты",
  PAID: "Оплачен",
  CANCELLED: "Оплата отменена",
};

/** Action wording for the buttons that move an order to a status. */
export const ORDER_STATUS_ACTIONS: Record<string, string> = {
  CONFIRMED: "Подтвердить",
  SHIPPED: "Передать курьеру",
  READY_FOR_PICKUP: "Готов к выдаче",
  COMPLETED: "Выполнен",
  CANCELLED: "Отменить заказ",
};

export const PAYMENT_STATUS_ACTIONS: Record<string, string> = {
  PAID: "Отметить оплаченным",
  CANCELLED: "Отменить оплату",
  PENDING: "Вернуть в «Ожидает оплаты»",
};

export const DELIVERY_LABELS: Record<string, string> = {
  COURIER: "Курьер",
  PICKUP: "Самовывоз",
};

const dateFormat = new Intl.DateTimeFormat("ru-RU", {
  timeZone: "Asia/Almaty",
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
});

export function formatDateTime(iso: string): string {
  return dateFormat.format(new Date(iso));
}

/** "+77760050625" → "+7 776 005 06 25" */
export function formatPhone(phone: string): string {
  const d = phone.replace(/\D/g, "");
  return d.length === 11 ? `+${d[0]} ${d.slice(1, 4)} ${d.slice(4, 7)} ${d.slice(7, 9)} ${d.slice(9)}` : phone;
}

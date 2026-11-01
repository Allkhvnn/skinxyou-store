"use client";

/**
 * A GET form that resubmits when a select or checkbox changes, so sorting and filters apply
 * instantly. Without JavaScript it still works through its submit button.
 */
export function AutoSubmitForm({ children, ...props }: React.ComponentProps<"form">) {
  return (
    <form
      {...props}
      method="get"
      onChange={(event) => {
        const target = event.target as HTMLElement;
        if (target instanceof HTMLSelectElement || (target instanceof HTMLInputElement && target.type === "checkbox")) {
          event.currentTarget.requestSubmit();
        }
      }}
    >
      {children}
    </form>
  );
}

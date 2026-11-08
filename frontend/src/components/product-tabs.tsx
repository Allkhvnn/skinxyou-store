"use client";

import { useId, useState } from "react";

/** All panels stay in the HTML (hidden, not removed), so search engines can read them. */
export function ProductTabs({ panels }: { panels: { label: string; content: React.ReactNode }[] }) {
  const [active, setActive] = useState(0);
  const id = useId();

  const onKeyDown = (e: React.KeyboardEvent) => {
    const step = e.key === "ArrowRight" ? 1 : e.key === "ArrowLeft" ? -1 : 0;
    if (!step) return;
    e.preventDefault();
    const next = (active + step + panels.length) % panels.length;
    setActive(next);
    document.getElementById(`${id}-tab-${next}`)?.focus();
  };

  return (
    <div>
      <div role="tablist" aria-label="О товаре" onKeyDown={onKeyDown}
        className="flex gap-6 overflow-x-auto border-b border-primary/15">
        {panels.map((panel, i) => (
          <button key={panel.label} id={`${id}-tab-${i}`} type="button" role="tab" className="tab-button"
            aria-selected={i === active} aria-controls={`${id}-panel-${i}`} tabIndex={i === active ? 0 : -1}
            onClick={() => setActive(i)}>
            {panel.label}
          </button>
        ))}
      </div>
      {panels.map((panel, i) => (
        <div key={panel.label} id={`${id}-panel-${i}`} role="tabpanel" aria-labelledby={`${id}-tab-${i}`}
          hidden={i !== active} className="pt-5">
          {panel.content}
        </div>
      ))}
    </div>
  );
}

const LONG_TEXT = 420;

export function ExpandableText({ text }: { text: string }) {
  const [expanded, setExpanded] = useState(false);
  const long = text.length > LONG_TEXT;
  return (
    <div className="text-sm leading-7 text-foreground/80">
      <p className={`whitespace-pre-line ${long && !expanded ? "line-clamp-6" : ""}`}>{text}</p>
      {long && (
        <button type="button" className="mt-2 font-semibold text-foreground underline underline-offset-4"
          onClick={() => setExpanded(!expanded)}>
          {expanded ? "Свернуть" : "Читать дальше"}
        </button>
      )}
    </div>
  );
}

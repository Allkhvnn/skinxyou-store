-- Home page promo slides, managed in the admin panel. Images live in Cloudflare R2.
CREATE TABLE banner (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title              VARCHAR(120) NOT NULL,
    link_url           VARCHAR(500),
    desktop_image_url  VARCHAR(1000),
    mobile_image_url   VARCHAR(1000),
    position           INT          NOT NULL,
    active             BOOLEAN      NOT NULL DEFAULT FALSE,
    -- Inclusive dates in Almaty time; null means no limit.
    starts_on          DATE,
    ends_on            DATE,
    created_at         TIMESTAMPTZ  NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_banner_position ON banner (position);

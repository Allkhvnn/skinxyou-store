-- Catalog synced from BILLZ. Columns marked "site" are owned by SkinxYou and never touched by sync.

CREATE TABLE brand (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    billz_id    VARCHAR(64)  NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(255) NOT NULL UNIQUE, -- site
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);

CREATE TABLE category (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    billz_id         VARCHAR(64)  NOT NULL UNIQUE,
    parent_billz_id  VARCHAR(64),
    name             VARCHAR(255) NOT NULL,
    slug             VARCHAR(255) NOT NULL UNIQUE, -- site
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL
);

CREATE TABLE product (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    billz_id            VARCHAR(64)   NOT NULL UNIQUE,
    -- BILLZ-owned
    name                VARCHAR(500)  NOT NULL,
    sku                 VARCHAR(100),
    barcode             VARCHAR(100),
    brand_id            BIGINT REFERENCES brand (id),
    price               NUMERIC(12, 2),
    promo_price         NUMERIC(12, 2),
    currency            VARCHAR(3),
    stock               NUMERIC(12, 3) NOT NULL DEFAULT 0,
    billz_updated_at    TIMESTAMP,
    available_in_billz  BOOLEAN       NOT NULL DEFAULT TRUE,
    synced_at           TIMESTAMPTZ   NOT NULL,
    -- site
    slug                VARCHAR(255)  NOT NULL UNIQUE,
    description         TEXT,
    seo_title           VARCHAR(255),
    seo_description     VARCHAR(500),
    hidden              BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ   NOT NULL,
    updated_at          TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_product_brand ON product (brand_id);
CREATE INDEX idx_product_sku ON product (sku);
CREATE INDEX idx_product_barcode ON product (barcode);
CREATE INDEX idx_product_name_trgm ON product USING gin (name gin_trgm_ops);

CREATE TABLE product_category (
    product_id   BIGINT NOT NULL REFERENCES product (id) ON DELETE CASCADE,
    category_id  BIGINT NOT NULL REFERENCES category (id) ON DELETE CASCADE,
    PRIMARY KEY (product_id, category_id)
);

CREATE INDEX idx_product_category_category ON product_category (category_id);

CREATE TABLE product_image (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id  BIGINT       NOT NULL REFERENCES product (id) ON DELETE CASCADE,
    url         VARCHAR(1000) NOT NULL,
    sequence    INT          NOT NULL DEFAULT 0,
    is_main     BOOLEAN      NOT NULL DEFAULT FALSE,
    -- BILLZ images are replaced on sync; SITE images are managed in the admin panel
    source      VARCHAR(16)  NOT NULL CHECK (source IN ('BILLZ', 'SITE'))
);

CREATE INDEX idx_product_image_product ON product_image (product_id);

CREATE TABLE billz_sync_run (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type                  VARCHAR(16)  NOT NULL CHECK (type IN ('DELTA', 'FULL')),
    status                VARCHAR(16)  NOT NULL CHECK (status IN ('RUNNING', 'SUCCESS', 'FAILED')),
    started_at            TIMESTAMPTZ  NOT NULL,
    finished_at           TIMESTAMPTZ,
    -- delta runs request changes since this moment (UTC)
    updated_since         TIMESTAMPTZ,
    products_fetched      INT          NOT NULL DEFAULT 0,
    products_created      INT          NOT NULL DEFAULT 0,
    products_updated      INT          NOT NULL DEFAULT 0,
    products_deactivated  INT          NOT NULL DEFAULT 0,
    error_message         VARCHAR(2000)
);

CREATE INDEX idx_billz_sync_run_started ON billz_sync_run (started_at DESC);

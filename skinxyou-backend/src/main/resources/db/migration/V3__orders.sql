-- Orders placed on the site. Customer data is stored on the order itself (no accounts yet);
-- item names and prices are snapshotted so later catalog changes never alter a placed order.

CREATE SEQUENCE order_number_seq START WITH 1001;

CREATE TABLE customer_order (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    number            VARCHAR(32)   NOT NULL UNIQUE,
    -- Client-generated key: a retried submission returns the existing order instead of a duplicate
    idempotency_key   UUID          NOT NULL UNIQUE,
    status            VARCHAR(32)   NOT NULL
        CHECK (status IN ('NEW', 'CONFIRMED', 'SHIPPED', 'READY_FOR_PICKUP', 'COMPLETED', 'CANCELLED')),
    payment_status    VARCHAR(32)   NOT NULL CHECK (payment_status IN ('PENDING', 'PAID', 'CANCELLED')),
    payment_method    VARCHAR(32)   NOT NULL CHECK (payment_method IN ('KASPI_INVOICE')),
    delivery_method   VARCHAR(32)   NOT NULL CHECK (delivery_method IN ('COURIER', 'PICKUP')),
    customer_name     VARCHAR(100)  NOT NULL,
    customer_phone    VARCHAR(20)   NOT NULL,
    delivery_address  VARCHAR(500),
    comment           VARCHAR(1000),
    items_total       NUMERIC(12, 2) NOT NULL,
    delivery_fee      NUMERIC(12, 2) NOT NULL,
    total             NUMERIC(12, 2) NOT NULL,
    currency          VARCHAR(3)    NOT NULL,
    created_at        TIMESTAMPTZ   NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_customer_order_created ON customer_order (created_at DESC);
CREATE INDEX idx_customer_order_phone ON customer_order (customer_phone);

CREATE TABLE order_item (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id          BIGINT        NOT NULL REFERENCES customer_order (id) ON DELETE CASCADE,
    -- Kept for reference; the snapshot below stays valid even if the product is removed
    product_id        BIGINT        REFERENCES product (id) ON DELETE SET NULL,
    product_billz_id  VARCHAR(64)   NOT NULL,
    product_slug      VARCHAR(255)  NOT NULL,
    product_name      VARCHAR(500)  NOT NULL,
    sku               VARCHAR(100),
    unit_price        NUMERIC(12, 2) NOT NULL,
    quantity          INT           NOT NULL CHECK (quantity > 0),
    line_total        NUMERIC(12, 2) NOT NULL
);

CREATE INDEX idx_order_item_order ON order_item (order_id);

CREATE TABLE order_status_history (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id     BIGINT       NOT NULL REFERENCES customer_order (id) ON DELETE CASCADE,
    type         VARCHAR(16)  NOT NULL CHECK (type IN ('ORDER', 'PAYMENT')),
    from_status  VARCHAR(32),
    to_status    VARCHAR(32)  NOT NULL,
    changed_by   VARCHAR(100) NOT NULL,
    note         VARCHAR(500),
    created_at   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_order_status_history_order ON order_status_history (order_id);

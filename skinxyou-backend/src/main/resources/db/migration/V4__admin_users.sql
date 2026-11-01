-- Staff accounts for the admin panel; each person has their own login so order history shows who changed what.
CREATE TABLE admin_user (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username       VARCHAR(50)  NOT NULL UNIQUE,
    password_hash  VARCHAR(100) NOT NULL,
    display_name   VARCHAR(100) NOT NULL,
    role           VARCHAR(16)  NOT NULL CHECK (role IN ('ADMIN', 'MANAGER')),
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at  TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL
);

-- Optimistic locking: two managers cannot silently overwrite each other's status changes.
ALTER TABLE customer_order ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

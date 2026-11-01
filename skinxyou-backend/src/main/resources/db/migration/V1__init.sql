-- Trigram index support for catalog search (name, brand, SKU) on PostgreSQL.
-- pg_trgm is a trusted extension (PG13+), so the database owner can create it.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

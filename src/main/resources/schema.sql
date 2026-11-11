-- Executed on every start (spring.datasource.initialization-mode=always), so every statement must be idempotent.
-- Hibernate does not manage the schema (spring.jpa.hibernate.ddl-auto=none).

-- Items still waiting to be mailed. Existing rows get FALSE, so they are never mailed again.
ALTER TABLE ITEMS ADD COLUMN IF NOT EXISTS SHOULD_NOTIFY BOOLEAN DEFAULT FALSE NOT NULL;

-- Errors that are not tied to a shop (e.g. email delivery failed for all recipients).
ALTER TABLE ERRORS ALTER COLUMN SHOP_ID SET NULL;

-- Every scanned item is looked up by link (+ price) to decide whether it is new (ItemsService.knownKeys).
-- The development DB already has this index; this makes sure every copy of the DB does.
CREATE INDEX IF NOT EXISTS IDX_LINK ON ITEMS(LINK);

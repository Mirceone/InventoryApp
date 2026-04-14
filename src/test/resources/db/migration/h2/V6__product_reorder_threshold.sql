ALTER TABLE products ADD COLUMN reorder_enabled boolean NOT NULL DEFAULT true;
ALTER TABLE products ADD COLUMN reorder_threshold integer NULL;

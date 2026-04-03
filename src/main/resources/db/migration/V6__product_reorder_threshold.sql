-- Prag reaprovizionare: NULL = foloseste app.inventory.default-reorder-threshold

ALTER TABLE products
    ADD COLUMN reorder_enabled boolean NOT NULL DEFAULT true,
    ADD COLUMN reorder_threshold integer NULL;

COMMENT ON COLUMN products.reorder_threshold IS 'Min stock before buy list; NULL uses application default';

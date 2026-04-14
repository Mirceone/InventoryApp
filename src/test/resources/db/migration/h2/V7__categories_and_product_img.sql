CREATE TABLE categories (
    id uuid DEFAULT RANDOM_UUID() PRIMARY KEY,
    firm_id uuid NOT NULL REFERENCES firms(id) ON DELETE CASCADE,
    name varchar(255) NOT NULL,
    created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_categories_firm_name UNIQUE (firm_id, name)
);

CREATE INDEX idx_categories_firm_id ON categories(firm_id);

INSERT INTO categories (firm_id, name)
SELECT id, 'Misc' FROM firms;

ALTER TABLE products ADD COLUMN category_id uuid;
ALTER TABLE products ADD COLUMN img_url varchar(2048);

UPDATE products p
SET category_id = (
    SELECT c.id FROM categories c WHERE c.firm_id = p.firm_id AND c.name = 'Misc'
);

ALTER TABLE products ALTER COLUMN category_id SET NOT NULL;

ALTER TABLE products
    ADD CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id);

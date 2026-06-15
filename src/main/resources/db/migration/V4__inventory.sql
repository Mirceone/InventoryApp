-- Inventory: categories, products, stock change audit trail.

CREATE TABLE categories (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    firm_id uuid NOT NULL REFERENCES firms(id) ON DELETE CASCADE,
    name varchar(255) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_categories_firm_name UNIQUE (firm_id, name)
);

CREATE INDEX idx_categories_firm_id ON categories(firm_id);

CREATE TABLE products (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    firm_id uuid NOT NULL REFERENCES firms(id) ON DELETE CASCADE,
    name varchar(255) NOT NULL,
    sku varchar(128),
    current_quantity integer NOT NULL DEFAULT 0,
    reorder_enabled boolean NOT NULL DEFAULT true,
    reorder_threshold integer NULL,
    category_id uuid NOT NULL,
    img_url varchar(2048),
    preferred_route_stop_id uuid REFERENCES route_stops(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT uk_products_firm_sku UNIQUE (firm_id, sku),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

COMMENT ON COLUMN products.reorder_threshold IS 'Min stock before buy list; NULL uses application default';

CREATE INDEX idx_products_firm_id ON products(firm_id);
CREATE INDEX idx_products_preferred_route_stop ON products(preferred_route_stop_id);

CREATE TABLE stock_change_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    firm_id uuid NOT NULL REFERENCES firms(id) ON DELETE CASCADE,
    product_id uuid NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    actor_user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    change_type varchar(16) NOT NULL,
    previous_quantity integer NOT NULL,
    new_quantity integer NOT NULL,
    delta integer NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_stock_change_events_product_id ON stock_change_events(product_id);
CREATE INDEX idx_stock_change_events_firm_id ON stock_change_events(firm_id);
CREATE INDEX idx_stock_change_events_actor_user_id ON stock_change_events(actor_user_id);
CREATE INDEX idx_stock_change_events_created_at ON stock_change_events(created_at);

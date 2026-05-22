ALTER TABLE products
    ADD COLUMN preferred_route_stop_id uuid REFERENCES route_stops(id) ON DELETE SET NULL;

CREATE INDEX idx_products_preferred_route_stop ON products(preferred_route_stop_id);

CREATE TABLE ops_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at timestamptz NOT NULL DEFAULT now(),
    prompt_excerpt text,
    response_excerpt text,
    model varchar(255)
);

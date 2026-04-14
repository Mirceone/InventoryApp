CREATE TABLE stock_change_events (
    id uuid DEFAULT RANDOM_UUID() PRIMARY KEY,
    firm_id uuid NOT NULL REFERENCES firms(id) ON DELETE CASCADE,
    product_id uuid NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    actor_user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    change_type varchar(16) NOT NULL,
    previous_quantity integer NOT NULL,
    new_quantity integer NOT NULL,
    delta integer NOT NULL,
    created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_stock_change_events_product_id ON stock_change_events(product_id);
CREATE INDEX idx_stock_change_events_firm_id ON stock_change_events(firm_id);
CREATE INDEX idx_stock_change_events_actor_user_id ON stock_change_events(actor_user_id);
CREATE INDEX idx_stock_change_events_created_at ON stock_change_events(created_at);

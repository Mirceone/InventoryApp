-- Free-form activity log entries authored by firm members against a work order.

CREATE TABLE work_order_activity (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    work_order_id uuid NOT NULL REFERENCES firm_work_orders(id) ON DELETE CASCADE,
    firm_id uuid NOT NULL REFERENCES firms(id),
    title varchar(255) NOT NULL,
    description varchar(1000),
    created_by_user_id uuid NOT NULL REFERENCES users(id),
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_work_order_activity_work_order_created
    ON work_order_activity (work_order_id, created_at DESC);

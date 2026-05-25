CREATE TABLE firm_status_history (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    firm_id uuid NOT NULL REFERENCES firms(id) ON DELETE CASCADE,
    previous_status varchar(32),
    new_status varchar(32) NOT NULL,
    message varchar(512),
    actor_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    source varchar(32) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_firm_status_history_firm_id_created_at
    ON firm_status_history(firm_id, created_at DESC);

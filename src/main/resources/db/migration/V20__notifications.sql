CREATE TABLE notifications (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    firm_id uuid NOT NULL REFERENCES firms(id) ON DELETE CASCADE,
    recipient_user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type varchar(64) NOT NULL,
    level varchar(32) NOT NULL,
    title varchar(255) NOT NULL,
    body varchar(1024) NOT NULL,
    metadata_json text,
    read_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_recipient_created_at
    ON notifications(recipient_user_id, created_at DESC);

CREATE INDEX idx_notifications_recipient_read_at
    ON notifications(recipient_user_id, read_at, created_at DESC);

ALTER TABLE firms
    ADD COLUMN status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN status_message varchar(512),
    ADD COLUMN status_updated_at timestamptz NOT NULL DEFAULT now();

UPDATE firms SET status = 'ACTIVE' WHERE status IS NULL;

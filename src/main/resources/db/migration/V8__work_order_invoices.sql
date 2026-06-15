-- Invoice uploads per work order: original blob + async MarkItDown extraction to markdown.

CREATE TABLE work_order_invoices (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    firm_id uuid NOT NULL REFERENCES firms(id),
    work_order_id uuid NOT NULL REFERENCES firm_work_orders(id) ON DELETE CASCADE,
    uploaded_by_user_id uuid NOT NULL REFERENCES users(id),
    display_name varchar(255) NOT NULL,
    extension varchar(16),
    mime_type varchar(255),
    size_bytes bigint NOT NULL CHECK (size_bytes >= 0),
    checksum_sha256 varchar(64),
    storage_key varchar(512) NOT NULL,
    processing_status varchar(16) NOT NULL DEFAULT 'PENDING',
    markdown_text text,
    processing_error text,
    created_at timestamptz NOT NULL DEFAULT now(),
    processed_at timestamptz,

    CONSTRAINT uk_work_order_invoices_storage_key UNIQUE (storage_key)
);

CREATE INDEX idx_work_order_invoices_work_order_created
    ON work_order_invoices (work_order_id, created_at DESC);
CREATE INDEX idx_work_order_invoices_firm
    ON work_order_invoices (firm_id);
CREATE INDEX idx_work_order_invoices_pending
    ON work_order_invoices (processing_status) WHERE processing_status = 'PENDING';

-- Structured extraction of invoice line items from the markdown produced by the
-- invoice processing pipeline (Phase 1: extraction only; matching/apply come later).

CREATE TABLE invoice_extractions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id uuid NOT NULL REFERENCES work_order_invoices(id) ON DELETE CASCADE,
    firm_id uuid NOT NULL REFERENCES firms(id) ON DELETE CASCADE,
    work_order_id uuid NOT NULL REFERENCES firm_work_orders(id) ON DELETE CASCADE,
    status varchar(16) NOT NULL DEFAULT 'PENDING',
    supplier_name varchar(512),
    invoice_number varchar(128),
    invoice_date date,
    currency varchar(8),
    total_amount numeric(18, 2),
    model varchar(255),
    error text,
    created_at timestamptz NOT NULL DEFAULT now(),
    extracted_at timestamptz,

    CONSTRAINT uk_invoice_extractions_invoice UNIQUE (invoice_id)
);

CREATE INDEX idx_invoice_extractions_firm ON invoice_extractions (firm_id);
CREATE INDEX idx_invoice_extractions_pending
    ON invoice_extractions (status) WHERE status = 'PENDING';

CREATE TABLE invoice_line_items (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    extraction_id uuid NOT NULL REFERENCES invoice_extractions(id) ON DELETE CASCADE,
    firm_id uuid NOT NULL REFERENCES firms(id) ON DELETE CASCADE,
    line_no int NOT NULL,
    raw_description text NOT NULL,
    sku varchar(128),
    quantity numeric(18, 3),
    unit varchar(32),
    unit_price numeric(18, 2),
    line_total numeric(18, 2),
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_invoice_line_items_extraction ON invoice_line_items (extraction_id);
CREATE INDEX idx_invoice_line_items_firm_sku ON invoice_line_items (firm_id, sku);

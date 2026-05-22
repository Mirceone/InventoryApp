CREATE TABLE firm_clients (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    firm_id uuid NOT NULL REFERENCES firms(id) ON DELETE CASCADE,
    display_name varchar(255) NOT NULL,
    external_ref varchar(255),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_firm_clients_firm_id ON firm_clients(firm_id);

CREATE TABLE client_documents (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    firm_id uuid NOT NULL REFERENCES firms(id) ON DELETE CASCADE,
    client_id uuid NOT NULL REFERENCES firm_clients(id) ON DELETE CASCADE,
    stored_path varchar(1024) NOT NULL,
    original_filename varchar(512) NOT NULL,
    content_type varchar(255),
    size_bytes bigint NOT NULL,
    uploaded_by_user_id uuid NOT NULL REFERENCES users(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    ai_summary text,
    ai_tags text,
    ai_model varchar(255),
    ai_processed_at timestamptz
);

CREATE INDEX idx_client_documents_firm_client ON client_documents(firm_id, client_id);

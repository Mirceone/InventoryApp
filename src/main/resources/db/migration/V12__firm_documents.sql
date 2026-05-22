CREATE TABLE firm_documents (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    firm_id uuid NOT NULL REFERENCES firms(id) ON DELETE CASCADE,
    uploaded_by_user_id uuid NOT NULL REFERENCES users(id),
    original_filename varchar(512) NOT NULL,
    mime_type varchar(255),
    size_bytes bigint NOT NULL CHECK (size_bytes >= 0),
    storage_key varchar(1024) NOT NULL,
    checksum_sha256 varchar(64),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_firm_documents_storage_key UNIQUE (storage_key)
);

CREATE INDEX idx_firm_documents_firm_created ON firm_documents(firm_id, created_at DESC);

ALTER TABLE firm_documents
    ADD COLUMN folder_path varchar(512),
    ADD COLUMN processing_status varchar(32) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN organization_source varchar(16),
    ADD COLUMN organization_error text;

UPDATE firm_documents
SET processing_status = 'CLASSIFIED',
    folder_path = ''
WHERE processing_status = 'PENDING';

CREATE INDEX idx_firm_documents_firm_folder_created
    ON firm_documents (firm_id, folder_path, created_at DESC);

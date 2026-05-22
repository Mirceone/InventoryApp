CREATE TABLE firm_dossiers (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    firm_id uuid NOT NULL REFERENCES firms(id) ON DELETE CASCADE,
    name varchar(255) NOT NULL,
    created_by_user_id uuid NOT NULL REFERENCES users(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_firm_dossiers_firm_name UNIQUE (firm_id, name)
);

CREATE INDEX idx_firm_dossiers_firm_created ON firm_dossiers (firm_id, created_at DESC);

ALTER TABLE firm_documents
    ADD COLUMN dossier_id uuid;

-- One default dossier per firm that already has documents; assign orphans.
INSERT INTO firm_dossiers (id, firm_id, name, created_by_user_id, created_at)
SELECT gen_random_uuid(),
       fd.firm_id,
       'Dosar general',
       (SELECT d.uploaded_by_user_id
        FROM firm_documents d
        WHERE d.firm_id = fd.firm_id
        ORDER BY d.created_at ASC
        LIMIT 1),
       now()
FROM (SELECT DISTINCT firm_id FROM firm_documents) fd;

UPDATE firm_documents d
SET dossier_id = (
    SELECT dos.id
    FROM firm_dossiers dos
    WHERE dos.firm_id = d.firm_id
      AND dos.name = 'Dosar general'
    LIMIT 1
)
WHERE d.dossier_id IS NULL;

ALTER TABLE firm_documents
    ALTER COLUMN dossier_id SET NOT NULL;

ALTER TABLE firm_documents
    ADD CONSTRAINT fk_firm_documents_dossier
        FOREIGN KEY (dossier_id) REFERENCES firm_dossiers(id) ON DELETE CASCADE;

CREATE INDEX idx_firm_documents_dossier_folder_created
    ON firm_documents (dossier_id, folder_path, created_at DESC);

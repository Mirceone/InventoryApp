ALTER TABLE work_order_files
    ADD COLUMN classification_status VARCHAR(32) NOT NULL DEFAULT 'CLASSIFIED',
    ADD COLUMN classification_source VARCHAR(16),
    ADD COLUMN classification_error TEXT;

CREATE INDEX idx_work_order_files_classification_pending
    ON work_order_files (classification_status)
    WHERE classification_status = 'PENDING';

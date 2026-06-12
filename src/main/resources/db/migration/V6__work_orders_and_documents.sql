-- Work orders with a user-customizable virtual folder tree and per-folder extension rules.
-- The blob store is opaque (UUID-based keys); all logical structure lives in these tables.

CREATE TABLE firm_work_orders (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    firm_id uuid NOT NULL REFERENCES firms(id) ON DELETE CASCADE,
    name varchar(255) NOT NULL,
    client_name varchar(255) NOT NULL,
    location varchar(255) NOT NULL,
    description text,
    estimated_end_date date NOT NULL,
    status varchar(32) NOT NULL,
    created_by_user_id uuid NOT NULL REFERENCES users(id),
    created_at timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT uk_firm_work_orders_firm_name UNIQUE (firm_id, name)
);

CREATE INDEX idx_firm_work_orders_firm_created ON firm_work_orders (firm_id, created_at DESC);
CREATE INDEX idx_firm_work_orders_firm_status_created ON firm_work_orders (firm_id, status, created_at DESC);

-- Virtual folder tree per work order. Exactly one catch-all folder per work order
-- receives files whose extension matches no rule.
CREATE TABLE work_order_folders (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    work_order_id uuid NOT NULL REFERENCES firm_work_orders(id) ON DELETE CASCADE,
    parent_id uuid REFERENCES work_order_folders(id) ON DELETE CASCADE,
    name varchar(64) NOT NULL,
    catch_all boolean NOT NULL DEFAULT false,
    sort_order int NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uk_work_order_folders_root_name
    ON work_order_folders (work_order_id, lower(name)) WHERE parent_id IS NULL;
CREATE UNIQUE INDEX uk_work_order_folders_sibling_name
    ON work_order_folders (work_order_id, parent_id, lower(name)) WHERE parent_id IS NOT NULL;
CREATE UNIQUE INDEX uk_work_order_folders_catch_all
    ON work_order_folders (work_order_id) WHERE catch_all;
CREATE INDEX idx_work_order_folders_work_order ON work_order_folders (work_order_id);
CREATE INDEX idx_work_order_folders_parent ON work_order_folders (parent_id);

-- One extension maps to exactly one folder within a work order (deterministic classification).
-- work_order_id is denormalized to enforce that uniqueness at the DB level.
CREATE TABLE work_order_folder_rules (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    work_order_id uuid NOT NULL REFERENCES firm_work_orders(id) ON DELETE CASCADE,
    folder_id uuid NOT NULL REFERENCES work_order_folders(id) ON DELETE CASCADE,
    extension varchar(16) NOT NULL,

    CONSTRAINT uk_work_order_folder_rules_extension UNIQUE (work_order_id, extension)
);

CREATE INDEX idx_work_order_folder_rules_folder ON work_order_folder_rules (folder_id);

-- File metadata. folder_id intentionally has no ON DELETE action: a folder that still
-- contains files cannot be deleted (the service empties or reassigns first).
CREATE TABLE work_order_files (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    firm_id uuid NOT NULL REFERENCES firms(id),
    work_order_id uuid NOT NULL REFERENCES firm_work_orders(id),
    folder_id uuid NOT NULL REFERENCES work_order_folders(id),
    uploaded_by_user_id uuid NOT NULL REFERENCES users(id),
    display_name varchar(255) NOT NULL,
    extension varchar(16),
    mime_type varchar(255),
    size_bytes bigint NOT NULL CHECK (size_bytes >= 0),
    checksum_sha256 varchar(64),
    storage_key varchar(512) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT uk_work_order_files_storage_key UNIQUE (storage_key)
);

CREATE UNIQUE INDEX uk_work_order_files_folder_display_name
    ON work_order_files (folder_id, lower(display_name));
CREATE INDEX idx_work_order_files_work_order_created ON work_order_files (work_order_id, created_at DESC);
CREATE INDEX idx_work_order_files_folder_created ON work_order_files (folder_id, created_at DESC);
CREATE INDEX idx_work_order_files_firm ON work_order_files (firm_id);

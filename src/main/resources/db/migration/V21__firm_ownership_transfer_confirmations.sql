CREATE TABLE firm_ownership_transfer_confirmations (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    firm_id uuid NOT NULL REFERENCES firms(id) ON DELETE CASCADE,
    requester_user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    new_owner_user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code_hash varchar(64) NOT NULL,
    expires_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_firm_ownership_transfer_confirmations_lookup
    ON firm_ownership_transfer_confirmations(firm_id, requester_user_id, new_owner_user_id, created_at DESC);

CREATE INDEX idx_firm_ownership_transfer_confirmations_expires_at
    ON firm_ownership_transfer_confirmations(expires_at);

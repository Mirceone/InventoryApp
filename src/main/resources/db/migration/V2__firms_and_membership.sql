-- Firms, membership, status lifecycle, invitations, ownership transfer confirmations.

CREATE TABLE firms (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name varchar(255) NOT NULL,
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    status_message varchar(512),
    status_updated_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_firms_owner_user_id ON firms(owner_user_id);

CREATE TABLE firm_members (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    firm_id uuid NOT NULL REFERENCES firms(id) ON DELETE CASCADE,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role varchar(32) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT uk_firm_members UNIQUE (firm_id, user_id)
);

CREATE INDEX idx_firm_members_user_id ON firm_members(user_id);

CREATE TABLE firm_status_history (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    firm_id uuid NOT NULL REFERENCES firms(id) ON DELETE CASCADE,
    previous_status varchar(32),
    new_status varchar(32) NOT NULL,
    message varchar(512),
    actor_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    source varchar(32) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_firm_status_history_firm_id_created_at
    ON firm_status_history(firm_id, created_at DESC);

CREATE TABLE firm_invitations (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    firm_id uuid NOT NULL REFERENCES firms(id) ON DELETE CASCADE,
    email varchar(255) NOT NULL,
    role varchar(32) NOT NULL,
    token_hash varchar(128) NOT NULL UNIQUE,
    status varchar(32) NOT NULL,
    invited_by_user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at timestamptz NOT NULL,
    accepted_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT chk_firm_invitations_role CHECK (role IN ('MEMBER')),
    CONSTRAINT chk_firm_invitations_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REVOKED', 'EXPIRED'))
);

CREATE UNIQUE INDEX uk_firm_invitations_pending_email
    ON firm_invitations (firm_id, email)
    WHERE status = 'PENDING';

CREATE INDEX idx_firm_invitations_firm_id ON firm_invitations(firm_id);
CREATE INDEX idx_firm_invitations_expires_at ON firm_invitations(expires_at);

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

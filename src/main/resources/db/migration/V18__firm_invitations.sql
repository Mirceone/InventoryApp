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

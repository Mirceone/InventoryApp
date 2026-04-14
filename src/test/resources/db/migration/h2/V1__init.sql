-- H2-compatible copy of V1 (no pgcrypto; RANDOM_UUID instead of gen_random_uuid)

CREATE TABLE users (
    id uuid DEFAULT RANDOM_UUID() PRIMARY KEY,
    email varchar(255) NOT NULL UNIQUE,
    password_hash varchar(255),
    provider varchar(32) NOT NULL,
    provider_sub varchar(255) NOT NULL,
    display_name varchar(255),
    created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_users_provider_sub UNIQUE (provider, provider_sub)
);

CREATE TABLE firms (
    id uuid DEFAULT RANDOM_UUID() PRIMARY KEY,
    owner_user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name varchar(255) NOT NULL,
    created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE firm_members (
    id uuid DEFAULT RANDOM_UUID() PRIMARY KEY,
    firm_id uuid NOT NULL REFERENCES firms(id) ON DELETE CASCADE,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role varchar(32) NOT NULL,
    created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_firm_members UNIQUE (firm_id, user_id)
);

CREATE TABLE products (
    id uuid DEFAULT RANDOM_UUID() PRIMARY KEY,
    firm_id uuid NOT NULL REFERENCES firms(id) ON DELETE CASCADE,
    name varchar(255) NOT NULL,
    sku varchar(128),
    current_quantity integer NOT NULL DEFAULT 0,
    created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_products_firm_sku UNIQUE (firm_id, sku)
);

CREATE INDEX idx_users_provider ON users(provider);
CREATE INDEX idx_firms_owner_user_id ON firms(owner_user_id);
CREATE INDEX idx_firm_members_user_id ON firm_members(user_id);
CREATE INDEX idx_products_firm_id ON products(firm_id);

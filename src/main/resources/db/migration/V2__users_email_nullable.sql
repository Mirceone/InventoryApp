-- Allow optional email for compatibility with existing social-provider records.
-- PostgreSQL allows multiple NULLs with UNIQUE constraints.

ALTER TABLE users
    ALTER COLUMN email DROP NOT NULL;

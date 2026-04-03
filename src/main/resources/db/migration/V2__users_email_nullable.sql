-- Allow social providers (Google) to have no email claim at first login.
-- PostgreSQL allows multiple NULLs with UNIQUE constraints.

ALTER TABLE users
    ALTER COLUMN email DROP NOT NULL;

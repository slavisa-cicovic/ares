DROP INDEX IF EXISTS ux_users_email_active;

CREATE UNIQUE INDEX ux_users_email_active
    ON users (LOWER(email))
    WHERE deleted_at IS NULL;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE companies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    deleted_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies (id),
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    status VARCHAR(32) NOT NULL,
    deleted_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NULL REFERENCES companies (id),
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    scope VARCHAR(32) NOT NULL,
    system_role BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE role_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL REFERENCES roles (id),
    permission_id UUID NOT NULL REFERENCES permissions (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE user_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id),
    role_id UUID NOT NULL REFERENCES roles (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX ux_users_email_active
    ON users (LOWER(email), company_id)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_roles_platform_code
    ON roles (code)
    WHERE company_id IS NULL;

CREATE UNIQUE INDEX ux_roles_company_code
    ON roles (company_id, code)
    WHERE company_id IS NOT NULL;

CREATE UNIQUE INDEX ux_permissions_code
    ON permissions (code);

CREATE UNIQUE INDEX ux_role_permissions_role_permission
    ON role_permissions (role_id, permission_id);

CREATE UNIQUE INDEX ux_user_roles_user_role
    ON user_roles (user_id, role_id);

CREATE INDEX ix_users_company_id ON users (company_id);
CREATE INDEX ix_users_deleted_at ON users (deleted_at);
CREATE INDEX ix_companies_deleted_at ON companies (deleted_at);
CREATE INDEX ix_roles_company_id ON roles (company_id);

INSERT INTO permissions (code, name) VALUES
    ('company.manage', 'Manage company settings'),
    ('user.manage', 'Manage company users'),
    ('role.manage', 'Manage company roles'),
    ('client.read', 'Read clients'),
    ('client.write', 'Manage clients'),
    ('item.read', 'Read items'),
    ('item.write', 'Manage items'),
    ('invoice.read', 'Read invoices'),
    ('invoice.write', 'Manage invoices'),
    ('payment.read', 'Read payments'),
    ('payment.write', 'Manage payments'),
    ('report.read', 'Read reports');

INSERT INTO roles (code, name, scope, system_role) VALUES
    ('super-admin', 'Super Admin', 'PLATFORM', TRUE),
    ('company-admin', 'Company Admin', 'COMPANY', TRUE),
    ('user', 'User', 'COMPANY', TRUE);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON TRUE
WHERE r.code = 'super-admin';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'company.manage',
    'user.manage',
    'role.manage',
    'client.read',
    'client.write',
    'item.read',
    'item.write',
    'invoice.read',
    'invoice.write',
    'payment.read',
    'payment.write',
    'report.read'
)
WHERE r.code = 'company-admin';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'client.read',
    'client.write',
    'item.read',
    'item.write',
    'invoice.read',
    'invoice.write',
    'payment.read',
    'payment.write',
    'report.read'
)
WHERE r.code = 'user';

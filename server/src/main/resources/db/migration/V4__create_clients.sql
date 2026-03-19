CREATE TABLE clients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies (id),
    name VARCHAR(255) NOT NULL,
    pib VARCHAR(32) NULL,
    mb VARCHAR(32) NULL,
    email VARCHAR(320) NULL,
    phone VARCHAR(50) NULL,
    address VARCHAR(255) NULL,
    city VARCHAR(120) NULL,
    postal_code VARCHAR(20) NULL,
    country VARCHAR(120) NULL,
    status VARCHAR(32) NOT NULL,
    deleted_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_clients_company_id ON clients (company_id);
CREATE INDEX ix_clients_deleted_at ON clients (deleted_at);
CREATE INDEX ix_clients_company_name ON clients (company_id, name);

alter table invoices
    add column if not exists issued_at timestamp with time zone;

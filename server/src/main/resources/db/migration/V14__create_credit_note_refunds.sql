create table credit_note_refunds (
    id uuid primary key,
    company_id uuid not null references companies(id),
    invoice_id uuid not null references invoices(id),
    refund_date date not null,
    amount numeric(19, 2) not null,
    note varchar(1000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_credit_note_refunds_invoice_id
    on credit_note_refunds (invoice_id);

create index idx_credit_note_refunds_company_id
    on credit_note_refunds (company_id);

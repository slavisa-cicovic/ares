create table payments (
    id uuid primary key,
    invoice_id uuid not null,
    company_id uuid not null,
    payment_date date not null,
    amount numeric(19, 2) not null,
    note varchar(1000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_payments_invoice foreign key (invoice_id) references invoices (id),
    constraint fk_payments_company foreign key (company_id) references companies (id)
);

create index ix_payments_invoice_id on payments (invoice_id);
create index ix_payments_company_id on payments (company_id);
create index ix_payments_company_payment_date on payments (company_id, payment_date);

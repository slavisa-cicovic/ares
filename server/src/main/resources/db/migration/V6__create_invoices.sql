create table invoices (
    id uuid primary key,
    company_id uuid not null,
    client_id uuid not null,
    invoice_number varchar(32) not null,
    sequence_year integer not null,
    sequence_number integer not null,
    issue_date date not null,
    due_date date not null,
    currency varchar(3) not null,
    status varchar(32) not null,
    note varchar(1000),
    subtotal numeric(19, 2) not null,
    tax_total numeric(19, 2) not null,
    total numeric(19, 2) not null,
    deleted_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_invoices_company foreign key (company_id) references companies (id),
    constraint fk_invoices_client foreign key (client_id) references clients (id)
);

create table invoice_lines (
    id uuid primary key,
    invoice_id uuid not null,
    item_id uuid,
    line_number integer not null,
    item_name varchar(255) not null,
    item_code varchar(64),
    unit varchar(50) not null,
    quantity numeric(15, 3) not null,
    unit_price numeric(19, 2) not null,
    tax_rate numeric(5, 2) not null,
    line_subtotal numeric(19, 2) not null,
    line_tax_total numeric(19, 2) not null,
    line_total numeric(19, 2) not null,
    created_at timestamp with time zone not null,
    constraint fk_invoice_lines_invoice foreign key (invoice_id) references invoices (id),
    constraint fk_invoice_lines_item foreign key (item_id) references items (id)
);

create index ix_invoices_company_id on invoices (company_id);
create index ix_invoices_deleted_at on invoices (deleted_at);
create index ix_invoices_company_issue_date on invoices (company_id, issue_date);
create unique index ux_invoices_company_number on invoices (company_id, invoice_number);
create unique index ux_invoices_company_sequence on invoices (company_id, sequence_year, sequence_number);
create index ix_invoice_lines_invoice_id on invoice_lines (invoice_id);

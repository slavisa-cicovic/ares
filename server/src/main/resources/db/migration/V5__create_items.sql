create table items (
    id uuid primary key,
    company_id uuid not null,
    name varchar(255) not null,
    code varchar(64),
    description varchar(1000),
    unit varchar(50) not null,
    price numeric(19, 2) not null,
    tax_rate numeric(5, 2) not null,
    status varchar(32) not null,
    deleted_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_items_company foreign key (company_id) references companies (id)
);

create index ix_items_company_id on items (company_id);
create index ix_items_deleted_at on items (deleted_at);
create index ix_items_company_name on items (company_id, name);
create unique index ux_items_company_code_active on items (company_id, upper(code))
    where deleted_at is null and code is not null;

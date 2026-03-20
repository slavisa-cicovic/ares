alter table invoices
    add column document_type varchar(32);

update invoices
set document_type = 'INVOICE'
where document_type is null;

alter table invoices
    alter column document_type set not null;

alter table invoices
    add column original_invoice_id uuid;

alter table invoices
    add constraint fk_invoices_original_invoice
        foreign key (original_invoice_id) references invoices (id);

create unique index ux_invoices_original_invoice_id
    on invoices (original_invoice_id)
    where original_invoice_id is not null;

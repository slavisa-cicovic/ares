alter table items
    add column default_value numeric(15, 3);

update items
set default_value = 1.000
where default_value is null;

alter table items
    alter column default_value set not null;

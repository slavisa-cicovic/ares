alter table items add column created_by_user_id uuid;

update items
set created_by_user_id = (
    select users.id
    from users
    where users.company_id = items.company_id
      and users.deleted_at is null
    order by users.created_at
    fetch first 1 row only
);

alter table items
    alter column created_by_user_id set not null;

alter table items
    add constraint fk_items_created_by_user foreign key (created_by_user_id) references users (id);

create index ix_items_created_by_user_id on items (created_by_user_id);

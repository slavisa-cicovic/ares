create unique index if not exists ux_companies_pib_active
    on companies (pib)
    where deleted_at is null and pib is not null;

create unique index if not exists ux_companies_mb_active
    on companies (mb)
    where deleted_at is null and mb is not null;

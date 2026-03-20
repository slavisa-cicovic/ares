alter table companies
    add column brand_color varchar(7),
    add column logo_url varchar(512),
    add column theme_preset varchar(32) not null default 'AMBER';

create table if not exists customer
(
    id         uuid         not null default gen_random_uuid(),
    first_name varchar(45),
    last_name  varchar(45),
    email      varchar(128) not null unique,
    address1   varchar(255) null,
    address2   varchar(255) null,
    postcode   varchar(16)  null,
    city       varchar(255) null,
    country    varchar(128) null,

    primary key (id)
);

create table if not exists product
(
    id        uuid           not null default gen_random_uuid(),
    version   int            not null default 0,
    inventory int            not null,
    name      varchar(128)   not null,
    price     numeric(19, 2) not null,
    sku       varchar(32)    not null unique,

    primary key (id)
);

create type shipment_status as enum
(
    'placed',
    'confirmed',
    'cancelled',
    'delivered'
);

create cast (varchar as shipment_status) with inout as implicit;

create table if not exists purchase_order
(
    id             uuid            not null default gen_random_uuid(),
    customer_id    uuid            not null,
    total_price    numeric(19, 2)  not null,
    tags           varchar(128)    null,
    status         shipment_status not null default 'placed',
    date_placed    timestamptz     not null default now(),
    date_updated   timestamptz     not null default now(),

    deliv_address1 varchar(255)    null,
    deliv_address2 varchar(255)    null,
    deliv_postcode varchar(16)     null,
    deliv_city     varchar(255)    null,
    deliv_country  varchar(128)    null,

    primary key (id)
);

create index on purchase_order (status);

create table if not exists purchase_order_item
(
    order_id   uuid           not null,
    product_id uuid           not null,
    quantity   int            not null,
    unit_price numeric(19, 2) not null,
    item_pos   int            not null,

    primary key (order_id, item_pos)
);

-- Keyed per (scope, idempotency_key), where scope is the authenticated principal or tenant.
-- Fingerprint is the raw 32 byte SHA-256 of the canonical request. Expired rows are removed
-- in batches by the application (IdempotencyEvictor), using the expires_at index below.
-- Only standard PostgreSQL features are used, so the schema is portable.
create table idempotency_record
(
    scope             varchar(255) not null,
    idempotency_key   varchar(255) not null,
    fingerprint       bytea        not null,
    resource_id       uuid         not null,
    created_at        timestamptz  not null default now(),
    expires_at        timestamptz  not null,

    primary key (scope, idempotency_key)
);

-- Indexes and constraints

create index idx_idempotency_record_expires
    on idempotency_record (expires_at);

alter table product
    add constraint check_product_positive_inventory check (product.inventory >= 0);

alter table if exists purchase_order_item
    add constraint fk_order_item_ref_product
    foreign key (product_id)
    references product;

alter table if exists purchase_order_item
    add constraint fk_order_item_ref_order
    foreign key (order_id)
    references purchase_order;

alter table if exists purchase_order
    add constraint fk_order_ref_customer
    foreign key (customer_id)
    references customer;

-- Foreign key indexes
create index fk_order_item_ref_product_idx on purchase_order_item (product_id);
create index fk_order_ref_customer_idx on purchase_order (customer_id);

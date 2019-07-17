drop table if exists account;
create table account
(
    n      identity  not null primary key,
    id     bigint    not null,
    fd     timestamp not null,
    td     timestamp not null,
    status int       not null
);
drop sequence if exists sq_account;
create sequence sq_account;
create index ix_account_id on account (id);

drop table if exists operation;
create table operation
(
    id             identity  not null primary key,
    source_account long null,
    target_account long null,
    amount         long    not null,
    ts             timestamp not null
);

drop table if exists balance;
create table balance
(
    n            identity  not null primary key,
    account_id   long      not null,
    balance      long    not null,
    operation_id long      not null,
    foreign key (operation_id) references operation (id),
    fd           timestamp not null,
    td           timestamp not null
);
create index ix_balance_account_id on balance (account_id);

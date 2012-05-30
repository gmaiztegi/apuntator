# --- Add access and refresh tokens and clients to data

# --- !Ups

create sequence client_seq start with 1;

create table clients (
    id                          bigint not null default nextval('client_seq'),
    random_id                   varchar(64) not null,
    name                        varchar(255) not null,
    secret                      varchar(255) not null,
    grant_types                 varchar(255) not null,
    constraint pk_clients primary key (id),
    constraint uq_clients_random_id unique (random_id))
;

create unique index idx_clients_random_id on clients (random_id);


create sequence access_token_seq start with 1;

create table access_tokens (
    id                          bigint not null default nextval('access_token_seq'),
    client_id                   bigint,
    user_id                     bigint not null,
    token                       varchar(255) not null,
    expires_at                  timestamp not null,
    constraint pk_access_tokens primary key (id),
    constraint uq_access_tokens_token unique (token),
    constraint fk_access_tokens_client_id foreign key (client_id) references clients (id) on delete cascade,
    constraint fk_access_tokens_user_id foreign key (user_id) references users (id) on delete cascade)
;

create unique index idx_access_tokens_token on access_tokens (token);
create unique index idx_access_tokens_token_expires_at on access_tokens (token, expires_at);


create sequence refresh_token_seq start with 1;

create table refresh_tokens (
    id                          bigint not null,
    client_id                   bigint,
    user_id                     bigint not null,
    current_token_id            bigint,
    token                       varchar(255) not null,
    expires_at                  timestamp not null,
    constraint pk_refresh_tokens primary key (id),
    constraint uq_refresh_tokens_token unique (token),
    constraint fk_refresh_tokens_client_id foreign key (client_id) references clients (id) on delete cascade,
    constraint fk_refresh_tokens_user_id foreign key (user_id) references users(id) on delete cascade,
    constraint fk_refresh_tokens_current_token_id foreign key (current_token_id) references access_tokens(id) on delete set null)
;

create unique index idx_refresh_tokens_token on refresh_tokens (token);
create unique index idx_refresh_tokens_token_expires_at on refresh_tokens (token, expires_at);

# --- !Downs

drop table if exists refresh_tokens;
drop table if exists access_tokens;
drop table if exists clients;

drop sequence if exists client_seq;
drop sequence if exists access_token_seq;
drop sequence if exists refresh_token_seq;

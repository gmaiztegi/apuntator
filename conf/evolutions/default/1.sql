# --- Add users to database

# --- !Ups

create sequence user_seq start with 1;

create table users (
    id                          bigint not null,
    username                    varchar(255) not null,
    username_canonical          varchar(255) unique not null,
    email                       varchar(255) not null,
    email_canonical             varchar(255) unique not null,
    enabled                     boolean not null,
    salt                        varchar(255) not null,
    password                    varchar(255) not null,
    algorithm                   varchar(64) not null,
    created_at                  timestamp not null,
    last_login                  timestamp,
    locked                      boolean not null,
    confirmation_token          varchar(255),
    password_requested_at       timestamp,
  constraint pk_user primary key (id))
;

create unique index idx_users_username on users (username_canonical);
create unique index idx_users_email on users (email_canonical);

# --- !Downs

drop table if exists users;

drop sequence if exists user_seq;

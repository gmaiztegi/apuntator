# --- !Ups

set ignorecase true;

create table users (
  id                        bigint not null,
  username                  varchar(255) not null,
  email                     varchar(255) not null,
  salt                      varchar(255) not null,
  password                  varchar(255) not null,
  algorithm                 varchar(64) not null,
  constraint pk_user primary key (id))
;

create sequence user_seq start with 1000;

# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists users;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists user_seq;

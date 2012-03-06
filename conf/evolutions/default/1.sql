# --- First database schema

# --- !Ups

create table files (
  id                        bigint not null,
  name                      varchar(255) not null,
  description               text not null,
  path                      varchar(255) not null,
  constraint pk_file primary key (id))
;

create sequence file_seq start with 1000;

# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists files;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists file_seq;


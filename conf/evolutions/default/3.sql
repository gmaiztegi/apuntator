# --- !Ups

set ignorecase true;

create table access_tokens (
  id                        bigint not null,
  client_id                 bigint,
  user_id                   bigint not null,
  token                     varchar(255) not null,
  expires_at                datetime not null,
  constraint pk_access_tokens primary key (id),
  constraint fk_access_tokens_user_id foreign key (user_id) references users (id))
;

create unique index idx_access_tokens_token on access_tokens (token);

create sequence access_token_seq start with 1;

# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists access_tokens;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists access_token_seq;
# --- Add files database schema

# --- !Ups

create sequence file_seq start with 1;

create table files (
    id                          bigint not null default nextval('file_seq'),
    name                        varchar(255) not null,
    description                 text not null,
    random_id                   varchar(64) not null,
    filename                    varchar(255) not null,
    user_id                     bigint not null,
    created_at                  timestamp not null,
    updated_at                  timestamp not null,
    constraint pk_file primary key (id),
    constraint fk_files_user_id foreign key (user_id) references users (id))
;

create index idx_files_name on files (name);
create index idx_files_filename on files (filename);

# --- !Downs

drop table if exists files;

drop sequence if exists file_seq;


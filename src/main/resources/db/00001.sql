create table appuser
(
    username      varchar(255) not null
        constraint pk_username
            primary key,
    password      varchar(255) not null,
    password_salt varchar(255) not null,
    version       integer default 0
);

alter table appuser
    owner to blog_admin;

create table article
(
    id        varchar(36) not null,
    title     text        not null,
    content   text        not null,
    author_id varchar(255)
        constraint article_user_username_fk
            references appuser,
    version   integer default 0
);

alter table article
    owner to blog_admin;

create unique index article_id_uindex
    on article (id);

create table roles_perms
(
    role varchar(255) not null
        constraint pk_roles_perms
            primary key,
    perm varchar(255) not null
);

alter table roles_perms
    owner to blog_admin;

create table user_roles
(
    username varchar(255) not null
        constraint fk_username
            references appuser,
    role     varchar(255) not null
        constraint fk_roles
            references roles_perms,
    constraint pk_user_roles
        primary key (username, role)
);

alter table user_roles
    owner to blog_admin;

create table http_session
(
    id       char(36)     not null
        constraint http_session_pkey
            primary key,
    username varchar(255) not null
        constraint pk_http_session_username
            references appuser
);

alter table http_session
    owner to blog_admin;


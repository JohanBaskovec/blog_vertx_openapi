create table article
(
    id      varchar(36) not null,
    title   text        not null,
    content text        not null
);

alter table article
    owner to blog_admin;

create unique index article_id_uindex
    on article (id);


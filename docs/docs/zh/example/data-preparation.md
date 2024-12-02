# 数据准备

``` mermaid
erDiagram
    BookStore |o--o{ Book : contains
    Book }o--o{ Author : written_by
```

=== "PostgreSQL"

    ```sql
    create database ktor_jimmer_rest_demo;
    \c ktor_jimmer_rest_demo;

    create table book_store
    (
        id      bigserial primary key,
        name    text not null,
        website text
    );
    alter table book_store
        add constraint business_key_book_store
            unique (name)
    ;

    create table book
    (
        id       bigserial primary key,
        name     text           not null,
        edition  integer        not null,
        price    numeric(10, 2) not null,
        store_id bigint
    );
    alter table book
        add constraint business_key_book
            unique (name, edition)
    ;
    alter table book
        add constraint fk_book__book_store
            foreign key (store_id)
                references book_store (id)
    ;

    create type gender_enum as enum ('M', 'F');
    create table author
    (
        id            bigserial primary key,
        first_name    text        not null,
        last_name     text        not null,
        gender        gender_enum not null,
        created_time  timestamp   not null,
        modified_time timestamp   not null
    );
    alter table author
        add constraint business_key_author
            unique (first_name, last_name)
    ;

    create table book_author_mapping
    (
        book_id   bigint not null,
        author_id bigint not null
    );
    alter table book_author_mapping
        add constraint pk_book_author_mapping
            primary key (book_id, author_id)
    ;
    alter table book_author_mapping
        add constraint fk_book_author_mapping__book
            foreign key (book_id)
                references book (id)
                on delete cascade
    ;
    alter table book_author_mapping
        add constraint fk_book_author_mapping__author
            foreign key (author_id)
                references author (id)
                on delete cascade
    ;

    ALTER SEQUENCE book_store_id_seq RESTART WITH 100;
    ALTER SEQUENCE book_id_seq RESTART WITH 100;
    ALTER SEQUENCE author_id_seq RESTART WITH 100;

    insert into book_store(id, name)
    values (1, 'O''REILLY'),
        (2, 'MANNING')
    ;

    insert into book(id, name, edition, price, store_id)
    values (1, 'Learning GraphQL', 1, 50, 1),
        (2, 'Learning GraphQL', 2, 55, 1),
        (3, 'Learning GraphQL', 3, 51, 1),

        (4, 'Effective TypeScript', 1, 73, 1),
        (5, 'Effective TypeScript', 2, 69, 1),
        (6, 'Effective TypeScript', 3, 88, 1),

        (7, 'Programming TypeScript', 1, 47.5, 1),
        (8, 'Programming TypeScript', 2, 45, 1),
        (9, 'Programming TypeScript', 3, 48, 1),

        (10, 'GraphQL in Action', 1, 80, 2),
        (11, 'GraphQL in Action', 2, 81, 2),
        (12, 'GraphQL in Action', 3, 80, 2)
    ;

    insert into author(id, first_name, last_name, gender, created_time, modified_time)
    values (1, 'Eve', 'Procello', 'F', now(), now()),
        (2, 'Alex', 'Banks', 'M', now(), now()),
        (3, 'Dan', 'Vanderkam', 'M', now(), now()),
        (4, 'Boris', 'Cherny', 'M', now(), now()),
        (5, 'Samer', 'Buna', 'M', now(), now())
    ;

    insert into book_author_mapping(book_id, author_id)
    values (1, 1),
        (2, 1),
        (3, 1),

        (1, 2),
        (2, 2),
        (3, 2),

        (4, 3),
        (5, 3),
        (6, 3),

        (7, 4),
        (8, 4),
        (9, 4),

        (10, 5),
        (11, 5),
        (12, 5)
    ;
    ```

=== "MySQL"

    ```sql
    create database ktor_jimmer_rest_demo;
    use ktor_jimmer_rest_demo;

    create table book_store
    (
        id      bigint unsigned not null auto_increment primary key,
        name    varchar(50)     not null,
        website varchar(100)
    ) engine = innodb;
    alter table book_store auto_increment = 100;
    alter table book_store
        add constraint business_key_book_store
            unique (name)
    ;

    create table book
    (
        id       bigint unsigned not null auto_increment primary key,
        name     varchar(50)     not null,
        edition  integer         not null,
        price    numeric(10, 2)  not null,
        store_id bigint unsigned
    ) engine = innodb;
    alter table book_store auto_increment = 100;
    alter table book
        add constraint business_key_book
            unique (name, edition)
    ;
    alter table book
        add constraint fk_book__book_store
            foreign key (store_id)
                references book_store (id)
    ;

    create table author
    (
        id            bigint unsigned not null auto_increment primary key,
        first_name    varchar(25)     not null,
        last_name     varchar(25)     not null,
        gender        char(1)         not null,
        created_time  datetime        not null,
        modified_time datetime        not null
    ) engine = innodb;
    alter table author auto_increment = 100;
    alter table author
        add constraint business_key_author
            unique (first_name, last_name)
    ;
    alter table author
        add constraint ck_author_gender
            check (gender in ('M', 'F'));

    create table book_author_mapping
    (
        book_id   bigint unsigned not null,
        author_id bigint unsigned not null
    ) engine = innodb;
    alter table book_author_mapping
        add constraint pk_book_author_mapping
            primary key (book_id, author_id)
    ;
    alter table book_author_mapping
        add constraint fk_book_author_mapping__book
            foreign key (book_id)
                references book (id)
                on delete cascade
    ;
    alter table book_author_mapping
        add constraint fk_book_author_mapping__author
            foreign key (author_id)
                references author (id)
                on delete cascade
    ;

    insert into book_store(id, name)
    values (1, 'O''REILLY'),
           (2, 'MANNING')
    ;

    insert into book(id, name, edition, price, store_id)
    values (1, 'Learning GraphQL', 1, 50, 1),
           (2, 'Learning GraphQL', 2, 55, 1),
           (3, 'Learning GraphQL', 3, 51, 1),

           (4, 'Effective TypeScript', 1, 73, 1),
           (5, 'Effective TypeScript', 2, 69, 1),
           (6, 'Effective TypeScript', 3, 88, 1),

           (7, 'Programming TypeScript', 1, 47.5, 1),
           (8, 'Programming TypeScript', 2, 45, 1),
           (9, 'Programming TypeScript', 3, 48, 1),

           (10, 'GraphQL in Action', 1, 80, 2),
           (11, 'GraphQL in Action', 2, 81, 2),
           (12, 'GraphQL in Action', 3, 80, 2)
    ;

    insert into author(id, first_name, last_name, gender, created_time, modified_time)
    values (1, 'Eve', 'Procello', 'F', now(), now()),
           (2, 'Alex', 'Banks', 'M', now(), now()),
           (3, 'Dan', 'Vanderkam', 'M', now(), now()),
           (4, 'Boris', 'Cherny', 'M', now(), now()),
           (5, 'Samer', 'Buna', 'M', now(), now())
    ;

    insert into book_author_mapping(book_id, author_id)
    values (1, 1),
           (2, 1),
           (3, 1),

           (1, 2),
           (2, 2),
           (3, 2),

           (4, 3),
           (5, 3),
           (6, 3),

           (7, 4),
           (8, 4),
           (9, 4),

           (10, 5),
           (11, 5),
           (12, 5)
    ;
    ```

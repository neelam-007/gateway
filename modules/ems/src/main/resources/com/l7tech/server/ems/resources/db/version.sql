-- --------------------------------------------------------------------------
-- Schema for schema versioning
-- --------------------------------------------------------------------------
--
-- This is created from a script since we use JDBC for access
--

create table schema_version (
    current_version bigint NOT NULL
);

insert into schema_version values ( 1 );

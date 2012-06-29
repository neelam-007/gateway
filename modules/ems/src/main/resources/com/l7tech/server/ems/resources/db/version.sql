-- --------------------------------------------------------------------------
-- Schema for schema versioning
-- --------------------------------------------------------------------------
--
-- This is created from a script since we use JDBC for access
--

create table schema_version (
    current_version bigint NOT NULL
);

--
-- Insert the current schema version (see SchemaUpdaterImpl.SCHEMA_VERSION)
--
insert into schema_version values ( 3 );

--
-- Initialize the object identifier high value to 1
--
create sequence hibernate_sequence start with 1;

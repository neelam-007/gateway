-- --------------------------------------------------------------------------
-- Schema upgrade script to update database to schema version 1
-- --------------------------------------------------------------------------
--
--
--

--
-- Changes to support schema upgrading
--

create table schema_version (
    current_version bigint NOT NULL
);

insert into schema_version values ( 1 );

--
-- Changes to support offline policy migration
--
alter table migration alter column target_cluster_oid null;


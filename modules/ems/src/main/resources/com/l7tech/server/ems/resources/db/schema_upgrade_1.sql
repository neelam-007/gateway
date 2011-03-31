-- --------------------------------------------------------------------------
-- Schema upgrade script to update database to schema version 1
-- --------------------------------------------------------------------------
--
-- Note that some schema updates occur automatically.
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

--
-- Internal user changes
--
update internal_user set enabled = 1;


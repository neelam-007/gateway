-- --------------------------------------------------------------------------
-- Schema upgrade script to update database to schema version 4
-- --------------------------------------------------------------------------
--
-- Note that some schema updates occur automatically.
--


--
-- Set schema version
--
update schema_version set current_version = 4;

alter table migration_mapping alter column source_entity_value set data type varchar(8192);
alter table migration_mapping alter column target_entity_value set data type varchar(8192);

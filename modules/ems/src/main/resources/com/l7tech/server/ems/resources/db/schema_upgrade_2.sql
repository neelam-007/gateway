-- --------------------------------------------------------------------------
-- Schema upgrade script to update database to schema version 2
-- --------------------------------------------------------------------------
--
-- Note that some schema updates occur automatically.
--


--
-- Set schema version
--

update schema_version set current_version = 2;

--
-- Monitoring updates
--

alter table entity_monitoring_property_setup alter column property_type set data type varchar(32);

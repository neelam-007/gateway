-- --------------------------------------------------------------------------
-- Schema upgrade script to update database to schema version 3
-- --------------------------------------------------------------------------
--
-- Note that some schema updates occur automatically.
--


--
-- Set schema version
--
update schema_version set current_version = 3;

--
-- IPv6 Updates. See bug 12568
--
alter table audit_main alter column ip_address set data type varchar(39);

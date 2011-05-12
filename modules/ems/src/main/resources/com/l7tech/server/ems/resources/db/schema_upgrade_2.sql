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
-- Sequence updates, set sequence to use value from old table and remove the table.
--
drop sequence hibernate_sequence restrict;
create procedure CREATESEQUENCEFROMTABLE ( name varchar( 128 ), tableName varchar( 128 ), columnName varchar( 128 ) ) parameter style java modifies sql data language java external name 'com.l7tech.server.ems.setup.DatabaseFunctions.createSequenceFromTable';
call CREATESEQUENCEFROMTABLE( 'hibernate_sequence', 'hibernate_unique_key', 'next_hi' );
drop procedure CREATESEQUENCEFROMTABLE;
drop table hibernate_unique_key;

--
-- Monitoring updates
--
alter table entity_monitoring_property_setup alter column property_type set data type varchar(32);

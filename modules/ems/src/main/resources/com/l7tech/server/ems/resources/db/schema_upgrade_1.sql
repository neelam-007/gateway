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
alter table internal_user alter column password set data type varchar(256);
update internal_user set enabled = 1, digest = password;
update internal_user set password = '';

--
-- Drop function so it is recreated with updated signature
--
create procedure DROPIFEXISTS ( type varchar( 128 ), name varchar( 128 ) ) parameter style java modifies sql data language java external name 'com.l7tech.server.ems.setup.DatabaseFunctions.dropIfExists';
call DROPIFEXISTS( 'function', 'getauditdetails' );
call DROPIFEXISTS( 'procedure', 'dropifexists' );

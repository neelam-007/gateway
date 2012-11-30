-- --------------------------------------------------------------------------
-- Test script for upgrading from 2.0.0 to 3.0.0
-- --------------------------------------------------------------------------

create table ugprade_v2_to_v3 (
    objectid bigint not null,
    primary key (objectid)
);

update ssg_version set current_version = '3.0.0';
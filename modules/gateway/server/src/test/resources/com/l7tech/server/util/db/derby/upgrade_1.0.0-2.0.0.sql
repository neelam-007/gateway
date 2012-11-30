-- --------------------------------------------------------------------------
-- Test script for upgrading from 1.0.0 to 2.0.0
-- --------------------------------------------------------------------------

create table ugprade_v1_to_v2 (
    objectid bigint not null,
    primary key (objectid)
);

update ssg_version set current_version = '2.0.0';
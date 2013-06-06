-- --------------------------------------------------------------------------
-- Test script for upgrading from 2.0.0 to 3.0.0
-- --------------------------------------------------------------------------


update ssg_version set current_version = '3.0.1';


-- create table ssg_version (
--     objectid bigint not null,
--     primary key (objectid)
-- );
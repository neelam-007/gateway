-- --------------------------------------------------------------------------
-- Test script for upgrading from version 'x' to version 'y'
-- --------------------------------------------------------------------------

update some_table set some_column = 'some_value';

update ssg_version set current_version = 'y';
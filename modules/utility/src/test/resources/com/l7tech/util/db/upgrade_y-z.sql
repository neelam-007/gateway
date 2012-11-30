-- --------------------------------------------------------------------------
-- Test script for upgrading from version 'y' to version 'z'
-- --------------------------------------------------------------------------

update some_table set some_column = 'some_value';

update ssg_version set current_version = 'z';
--
-- Script to update mysql ssg database from 5.2.0 to 5.3.0
--
-- Layer 7 Technologies, inc
--

--
-- Disable FK while manipulating tables
--
SET FOREIGN_KEY_CHECKS=0;

--
-- Update the version
--

UPDATE ssg_version SET current_version = '5.3.0';

--
-- Remove 'READ' permission on 'SERVICE' for 'Manage JDBC Connections' role.
--
DELETE FROM rbac_permission WHERE objectid = -955;

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--

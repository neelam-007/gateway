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
-- Add 'READ' permission on JDBC_CONNECTION for 'Publish Webservices' role.
--
INSERT INTO rbac_permission VALUES (-357,0,-350,'READ',NULL,'JDBC_CONNECTION');

--
-- Add 'READ' permission on JDBC_CONNECTION for 'Manage Webservices' role.
--
INSERT INTO rbac_permission VALUES (-438,0,-400,'READ',NULL,'JDBC_CONNECTION');

--
-- Upgrade task for manage service/policy roles
--
INSERT INTO cluster_properties
    (objectid, version, propkey, propvalue)
    values (-500301, 0, "upgrade.task.500301", "com.l7tech.server.upgrade.Upgrade52To53UpdateRoles");

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--

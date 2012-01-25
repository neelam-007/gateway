--
-- Script to update mysql ssg database from 6.1.5 to 6.2.0
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
UPDATE ssg_version SET current_version = '6.2.0';


--
-- Table for generic (runtime) entity types
--
CREATE TABLE generic_entity (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  name varchar(255),
  description mediumtext,
  classname varchar(255) NOT NULL,
  enabled boolean DEFAULT TRUE,
  value_xml mediumtext,
  PRIMARY KEY (objectid),
  UNIQUE KEY i_classname_name (classname, name)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Change "Manage JMS Connections" role to restrict access for both JMS and MQ native destination management.
-- Add permissions to manage SSG Active Connectors of type MqNative.
-- Add read permission for Secure Password management.
--
UPDATE rbac_role SET name='Manage Message Destinations', description='Users assigned to the {0} role have the ability to read, create, update and delete message destinations.' WHERE objectid=-650;
INSERT INTO rbac_permission VALUES (-662,1,-650,'READ',NULL,'SSG_ACTIVE_CONNECTOR');
INSERT INTO rbac_predicate VALUES (-663,0,-662);
INSERT INTO rbac_predicate_attribute VALUES (-663,'type','MqNative');
INSERT INTO rbac_permission VALUES (-664,1,-650,'DELETE',NULL,'SSG_ACTIVE_CONNECTOR');
INSERT INTO rbac_predicate VALUES (-665,0,-664);
INSERT INTO rbac_predicate_attribute VALUES (-665,'type','MqNative');
INSERT INTO rbac_permission VALUES (-666,1,-650,'CREATE',NULL,'SSG_ACTIVE_CONNECTOR');
INSERT INTO rbac_predicate VALUES (-667,0,-666);
INSERT INTO rbac_predicate_attribute VALUES (-667,'type','MqNative');
INSERT INTO rbac_permission VALUES (-668,1,-650,'UPDATE',NULL,'SSG_ACTIVE_CONNECTOR');
INSERT INTO rbac_predicate VALUES (-669,0,-668);
INSERT INTO rbac_predicate_attribute VALUES (-669,'type','MqNative');
INSERT INTO rbac_permission VALUES (-670,0,-650,'READ',NULL,'SECURE_PASSWORD');

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--

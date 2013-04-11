--
-- Script to update mysql ssg database from 7.1.0 to 8.0.0
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
UPDATE ssg_version SET current_version = '8.0.0';

--
-- Security Zones
--
CREATE TABLE security_zone (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  description varchar(255) NOT NULL,
  entity_types varchar(4096) NOT NULL,
  PRIMARY KEY (objectid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS rbac_predicate_security_zone;
CREATE TABLE rbac_predicate_security_zone (
  objectid bigint(20) NOT NULL,
  security_zone_oid bigint(20) NOT NULL,
  PRIMARY KEY (objectid),
  FOREIGN KEY (objectid) REFERENCES rbac_predicate (objectid) ON DELETE CASCADE,
  FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

alter table policy add column security_zone_oid bigint(20);
alter table policy add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table published_service add column security_zone_oid bigint(20);
alter table published_service add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

--
-- RBAC for Assertions: Update "Publish Webservices" and "Manage Webservices" canned roles so they can still use policy assertions in 8.0
--
INSERT INTO rbac_permission VALUES (-360,0,-350,'READ',NULL,'ASSERTION_ACCESS');
INSERT INTO rbac_permission VALUES (-361,0,-350,'CREATE',NULL,'ASSERTION_ACCESS');
INSERT INTO rbac_permission VALUES (-442,0,-400,'CREATE',NULL,'ASSERTION_ACCESS');
INSERT INTO rbac_permission VALUES (-443,0,-400,'READ',NULL,'ASSERTION_ACCESS');

--
-- Register upgrade task for adding Assertion Access to auto-created "Manage <Blah>" roles
--
INSERT INTO cluster_properties
    (objectid, version, propkey, propvalue, properties)
    values (-800000, 0, 'upgrade.task.800000', 'com.l7tech.server.upgrade.Upgrade71To80UpdateRoles', null);

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--

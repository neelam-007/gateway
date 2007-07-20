---
--- Script to update mysql ssg database from 4.1 to 4.2
---
--- Layer 7 Technologies, inc
---

--
-- Table structure for revocation checking policies
--

DROP TABLE IF EXISTS revocation_check_policy;
CREATE TABLE revocation_check_policy (
  objectid bigint(20) NOT NULL,
  version int(11) default NULL,
  name varchar(128) NOT NULL,
  revocation_policy_xml mediumtext,
  default_policy tinyint default '0',
  default_success tinyint default '0',
  PRIMARY KEY  (objectid),
  UNIQUE KEY rcp_name_idx (name)  
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Add RBAC permissions to the 'Manage Certificates (truststore)' role
--
UPDATE rbac_role set description = 'Users assigned to the {0} role have the ability to read, create, update and delete trusted certificates and policies for revocation checking.';
DELETE FROM rbac_permission WHERE objectid in (-605, -606, -607, -608);
INSERT INTO rbac_permission VALUES (-605,0,-600,'UPDATE',NULL,'REVOCATION_CHECK_POLICY');
INSERT INTO rbac_permission VALUES (-606,0,-600,'READ',NULL,'REVOCATION_CHECK_POLICY');
INSERT INTO rbac_permission VALUES (-607,0,-600,'DELETE',NULL,'REVOCATION_CHECK_POLICY');
INSERT INTO rbac_permission VALUES (-608,0,-600,'CREATE',NULL,'REVOCATION_CHECK_POLICY');

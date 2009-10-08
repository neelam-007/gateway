---
--- Script to update mysql ssg database from 5.1.0 to 5.2.0
---
--- Layer 7 Technologies, inc
---

--
-- Disable FK while manipulating tables
--
SET FOREIGN_KEY_CHECKS=0;

--
-- Update the version
--
UPDATE ssg_version SET current_version = '5.2.0';

--
-- Convert DN columns to allow larger values
--
ALTER TABLE client_cert MODIFY COLUMN subject_dn VARCHAR(500), MODIFY COLUMN issuer_dn VARCHAR(500);
ALTER TABLE trusted_cert MODIFY COLUMN subject_dn VARCHAR(500) NOT NULL, MODIFY COLUMN issuer_dn VARCHAR(500) NOT NULL;

--
-- Upgrade task for DN canonicalization
--
INSERT INTO cluster_properties
    (objectid, version, propkey, propvalue)
    values (-500200, 0, "upgrade.task.500200", "com.l7tech.server.upgrade.Upgrade51To52CanonicalizeDNs");

--
-- Create Table structure for JDBC Connections
--
DROP TABLE IF EXISTS jdbc_connection;
CREATE TABLE jdbc_connection (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  driver_class varchar(128) NOT NULL,
  jdbc_url varchar(128) NOT NULL,
  user_name varchar(128) NOT NULL,
  password varchar(128) NOT NULL,
  enabled tinyint(1) NOT NULL DEFAULT 1,
  min_pool_size integer NOT NULL DEFAULT 3,
  max_pool_size integer NOT NULL DEFAULT 15,
  additional_properties mediumtext,
  PRIMARY KEY (objectid),
  UNIQUE(name)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

INSERT INTO rbac_role VALUES (-950,0,'Manage JDBC Connections', null,null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete JDBC connections.');
INSERT INTO rbac_permission VALUES (-951,0,-950,'READ',NULL,'JDBC_CONNECTION');
INSERT INTO rbac_permission VALUES (-952,0,-950,'CREATE',NULL,'JDBC_CONNECTION');
INSERT INTO rbac_permission VALUES (-953,0,-950,'UPDATE',NULL,'JDBC_CONNECTION');
INSERT INTO rbac_permission VALUES (-954,0,-950,'DELETE',NULL,'JDBC_CONNECTION');
INSERT INTO rbac_permission VALUES (-955,0,-950,'READ',NULL,'SERVICE');

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--

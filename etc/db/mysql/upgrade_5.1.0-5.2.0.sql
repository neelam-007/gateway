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
  driver_class varchar(256) NOT NULL,
  jdbc_url varchar(256) NOT NULL,
  user_name varchar(128) NOT NULL,
  password varchar(64) NOT NULL,
  enabled tinyint(1) NOT NULL DEFAULT 1,
  min_pool_size integer NOT NULL DEFAULT 3,
  max_pool_size integer NOT NULL DEFAULT 15,
  additional_properties mediumtext,
  PRIMARY KEY (objectid),
  UNIQUE(name)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for UDDI Registries
-- Note: base_url is unique and has a size limit of 255 bytes, which is the max allowed for a unique key
-- in mysql when using utf-8 encoding. It is the max size of a hostname
--
DROP TABLE IF EXISTS uddi_registries;
CREATE TABLE uddi_registries (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  enabled tinyint(1) NOT NULL DEFAULT 0,
  registry_type varchar(128) NOT NULL,
  base_url varchar(255) NOT NULL,
  security_url varchar(255) NOT NULL,
  inquiry_url varchar(255) NOT NULL,
  publish_url varchar(255) NOT NULL,
  subscription_url varchar(255) NULL,
  client_auth tinyint(1) NOT NULL DEFAULT 0,
  keystore_oid bigint(20) NULL,
  key_alias varchar(255) NULL,
  user_name varchar(128) NOT NULL,
  password varchar(128) NOT NULL,
  metrics_enabled tinyint(1) NOT NULL DEFAULT 0,
  metrics_publish_frequency integer NOT NULL DEFAULT 0,
  monitoring_enabled tinyint(1) NOT NULL DEFAULT 0,
  subscribe_for_notifications tinyint(1) NOT NULL DEFAULT 0,
  monitor_frequency integer NOT NULL DEFAULT 0,
  PRIMARY KEY (objectid),
  UNIQUE(name),
  UNIQUE(base_url)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

INSERT INTO rbac_role VALUES (-950,0,'Manage JDBC Connections', null,null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete JDBC connections.');
INSERT INTO rbac_permission VALUES (-951,0,-950,'READ',NULL,'JDBC_CONNECTION');
INSERT INTO rbac_permission VALUES (-952,0,-950,'CREATE',NULL,'JDBC_CONNECTION');
INSERT INTO rbac_permission VALUES (-953,0,-950,'UPDATE',NULL,'JDBC_CONNECTION');
INSERT INTO rbac_permission VALUES (-954,0,-950,'DELETE',NULL,'JDBC_CONNECTION');
INSERT INTO rbac_permission VALUES (-955,0,-950,'READ',NULL,'SERVICE');

INSERT INTO rbac_role VALUES (-1000,0,'Manage UDDI Registries', null,null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete UDDI Registry connections.');
INSERT INTO rbac_permission VALUES (-1001,0,-1000,'READ',NULL,'UDDI_REGISTRY');
INSERT INTO rbac_permission VALUES (-1002,0,-1000,'CREATE',NULL,'UDDI_REGISTRY');
INSERT INTO rbac_permission VALUES (-1003,0,-1000,'UPDATE',NULL,'UDDI_REGISTRY');
INSERT INTO rbac_permission VALUES (-1004,0,-1000,'DELETE',NULL,'UDDI_REGISTRY');
INSERT INTO rbac_permission VALUES (-1005,0,-1000,'READ',NULL,'SERVICE');

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--

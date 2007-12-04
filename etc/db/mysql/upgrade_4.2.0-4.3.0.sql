---
--- Script to update mysql ssg database from 4.2 to 4.3
---
--- Layer 7 Technologies, inc
---

--
-- Disable FK while manipulating tables
--
SET FOREIGN_KEY_CHECKS=0;

-- HTTP and HTTPS listeners
CREATE TABLE connector (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  enabled tinyint(1) NOT NULL DEFAULT 0,
  port int(8) NOT NULL,
  scheme varchar(128) NOT NULL DEFAULT 'http',
  endpoints varchar(256) NOT NULL,
  secure tinyint(1) NOT NULL DEFAULT 0,
  client_auth tinyint(1) NOT NULL DEFAULT 0,
  keystore_oid bigint(20) NULL,
  key_alias varchar(255) NULL,
  PRIMARY KEY (objectid)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

-- listener properties
CREATE TABLE connector_property (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  connector_oid bigint(20) NOT NULL,
  name varchar(128) NOT NULL,
  value MEDIUMTEXT NOT NULL,
  PRIMARY KEY (objectid),
  FOREIGN KEY (connector_oid) REFERENCES connector (objectid) ON DELETE CASCADE
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

-- The default connectors are NOT included here; since this is an upgrade from pre-4.3, they will be imported
-- from server.xml and ftpserver.properties instead.

-- Create the 'Manage Listen Ports' role
DELETE FROM rbac_permission WHERE role_oid=-750;
DELETE FROM rbac_role WHERE objectid=-750;
INSERT INTO rbac_role VALUES (-750,0,'Manage Listen Ports', null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete Gateway listen ports (HTTP(S) and FTP(S)).');
INSERT INTO rbac_permission VALUES (-751,0,-750,'READ',NULL,'SSG_CONNECTOR');
INSERT INTO rbac_permission VALUES (-752,0,-750,'CREATE',NULL,'SSG_CONNECTOR');
INSERT INTO rbac_permission VALUES (-753,0,-750,'UPDATE',NULL,'SSG_CONNECTOR');
INSERT INTO rbac_permission VALUES (-754,0,-750,'DELETE',NULL,'SSG_CONNECTOR');

-- Create the sink_config table for logging sink configurations
DROP TABLE IF EXISTS sink_config;
CREATE TABLE sink_config (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  name varchar(32) NOT NULL,
  description mediumtext,
  type varchar(32) NOT NULL DEFAULT 'FILE',
  enabled tinyint(1) NOT NULL DEFAULT 0,
  severity varchar(32) NOT NULL DEFAULT 'INFO',
  categories mediumtext,
  properties mediumtext,
  PRIMARY KEY  (objectid)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;


----------------------------------------
--           POLICY INCLUDES          --
----------------------------------------

--
-- Table structure for table 'policy'
--
CREATE TABLE policy (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  name varchar(255),
  xml mediumtext NOT NULL,
  policy_type VARCHAR(32) NOT NULL,
  soap TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (objectid),
  INDEX (policy_type)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- TODO published_service.policy_xml column should be dropped someday--can't do it here; the actual policy migration will be done on SSG startup
-- TODO published_service.policy_oid column should be NOT NULL someday--can't do it until after policy migration
--
ALTER TABLE published_service ADD policy_oid BIGINT(20);
ALTER TABLE published_service ADD FOREIGN KEY (policy_oid) REFERENCES policy (objectid);

insert into cluster_properties
    (objectid, version, propkey, propvalue)
    values (-400300, 0, "upgrade.task.400300", "com.l7tech.server.upgrade.Upgrade42To43MigratePolicies");

INSERT INTO cluster_properties
    (objectid, version, propkey, propvalue)
    values (-400301, 0, "upgrade.task.400301", "com.l7tech.server.upgrade.Upgrade42To43AddPolicyPermissions");

---
--- Policy XML rollback support
---

CREATE TABLE policy_version (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  name varchar(255),
  policy_oid bigint(20) NOT NULL,
  ordinal int(20) NOT NULL,
  time bigint(20) NOT NULL,
  user_provider_oid bigint(20),
  user_login varchar(255),
  active boolean,
  xml mediumtext,
  PRIMARY KEY (objectid),
  INDEX (policy_oid),
  FOREIGN KEY (policy_oid) REFERENCES policy (objectid) ON DELETE CASCADE
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

INSERT INTO cluster_properties
    (objectid, version, propkey, propvalue)
    values (-400302, 0, "upgrade.task.400302", "com.l7tech.server.upgrade.Upgrade42To43AddInitialPolicyVersions");

---
--- Delete the client certificates whose identitiy_provider does not exist.
---
DELETE FROM client_cert WHERE provider NOT IN (SELECT objectid FROM identity_provider);

---
--- Add a foreign key in the table client_cert, so when deleting an identity provider, the database will automatically delete the associated client certificate.
---
ALTER TABLE client_cert ADD FOREIGN KEY (provider) REFERENCES identity_provider (objectid) ON DELETE CASCADE;

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

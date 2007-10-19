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

-- Default connectors
INSERT INTO connector VALUES (-670,1,'Main',1,8080,'HTTP','MESSAGE_INPUT,ADMIN_REMOTE,ADMIN_APPLET,OTHER_SERVLETS',0,0,NULL,NULL);
INSERT INTO connector VALUES (-671,1,'Main SSL',1,8443,'HTTPS','MESSAGE_INPUT,ADMIN_REMOTE,ADMIN_APPLET,OTHER_SERVLETS',1,2,NULL,NULL);
INSERT INTO connector VALUES (-672,1,'Main SSL (No Client Auth)',1,9443,'HTTPS','MESSAGE_INPUT,ADMIN_REMOTE,ADMIN_APPLET,OTHER_SERVLETS',1,0,NULL,NULL);

-- Create the 'Manage Listen Ports' role
DELETE FROM rbac_permission WHERE role_oid=-750;
DELETE FROM rbac_role WHERE objectid=-750;
INSERT INTO rbac_role VALUES (-750,0,'Manage Listen Ports', null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete Gateway listen ports (HTTP(S) and FTP(S)).');
INSERT INTO rbac_permission VALUES (-751,0,-750,'READ',NULL,'SSG_CONNECTOR');
INSERT INTO rbac_permission VALUES (-752,0,-750,'CREATE',NULL,'SSG_CONNECTOR');
INSERT INTO rbac_permission VALUES (-753,0,-750,'UPDATE',NULL,'SSG_CONNECTOR');
INSERT INTO rbac_permission VALUES (-754,0,-750,'DELETE',NULL,'SSG_CONNECTOR');

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

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

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

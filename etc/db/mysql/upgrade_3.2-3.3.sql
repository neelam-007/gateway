---
--- Script to update mysql ssg database from 3.2 to 3.3
---
--- Layer 7 Technologies, inc

ALTER TABLE trusted_cert ADD COLUMN trusted_as_saml_attesting_entity tinyint(1) default '0';

CREATE TABLE community_schemas (
  objectid bigint(20) NOT NULL,
  name varchar(128) default '',
  tns varchar(128) default '',
  schema mediumtext default '',
  PRIMARY KEY (objectid)
) TYPE=InnoDB;
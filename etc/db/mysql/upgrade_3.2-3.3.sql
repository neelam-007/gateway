--
-- Script to update mysql ssg database from 3.2 to 3.3
--
-- Layer 7 Technologies, inc

ALTER TABLE trusted_cert ADD COLUMN trusted_as_saml_attesting_entity tinyint(1) default '0';

CREATE TABLE community_schemas (
  objectid bigint(20) NOT NULL,
  name varchar(128) default '',
  tns varchar(128) default '',
  schema mediumtext default '',
  PRIMARY KEY (objectid)
) TYPE=InnoDB;

ALTER TABLE client_cert ADD COLUMN thumbprint_sha1 VARCHAR(64);
ALTER TABLE client_cert ADD INDEX i_thumb (thumbprint_sha1);

ALTER TABLE trusted_cert ADD COLUMN thumbprint_sha1 VARCHAR(64);
ALTER TABLE trusted_cert ADD INDEX i_thumb (thumbprint_sha1);

CREATE TABLE cluster_properties (
  propkey varchar(128) NOT NULL PRIMARY KEY,
  propvalue varchar(255) NOT NULL
) TYPE=InnoDB;

CREATE TABLE sample_messages (
  objectid bigint(20) NOT NULL,
  published_service_oid bigint(20),
  name varchar(128) NOT NULL,
  xml mediumtext NOT NULL,
  operation_name varchar(128),
  INDEX i_ps_oid (published_service_oid),
  INDEX i_operation_name (operation_name),
  PRIMARY KEY (objectid)
) TYPE=InnoDB;

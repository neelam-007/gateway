-- Fix old
ALTER TABLE internal_group MODIFY COLUMN description text;
ALTER TABLE internal_group MODIFY COLUMN description text;
DELETE FROM internal_group where objectid=2;
INSERT INTO internal_group VALUES (2,0,'Gateway Administrators','SecureSpan Manager users with full administrative rights in the SecureSpan Gateway.');
INSERT INTO internal_group VALUES (4,0,'Gateway Operators','SecureSpan Manager users with partial read-only rights in the SecureSpan Gateway.');

ALTER TABLE internal_user DROP COLUMN title;

ALTER TABLE published_service ADD COLUMN soap TINYINT(1) NOT NULL DEFAULT 1;
ALTER TABLE published_service ADD COLUMN routing_uri varchar(128) default NULL;
UPDATE published_services SET soap=1;

ALTER TABLE client_cert ADD COLUMN user_id varchar(255) NOT NULL;

ALTER TABLE service_resolution MODIFY COLUMN soapaction varchar(255) default '';
ALTER TABLE service_resolution MODIFY COLUMN urn varchar(255) default '';
ALTER TABLE service_resolution ADD COLUMN uri varchar(255) default '';
ALTER TABLE service_resolution DROP INDEX soapaction;
ALTER TABLE service_resolution ADD UNIQUE soapaction (soapaction,urn,uri);

ALTER TABLE ssg_logs ADD INDEX idx_requestid(strrequestid);

ALTER TABLE cluster_info ADD COLUMN multicast_address varchar(16) default NULL;

ALTER TABLE identity_provider MODIFY COLUMN description text;

-- New tables

CREATE TABLE trusted_cert (
  objectid bigint NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  subject_dn varchar(255) NOT NULL,
  cert_base64 text NOT NULL,
  trusted_for_ssl tinyint(1) default '0',
  trusted_for_client tinyint(1) default '0',
  trusted_for_server tinyint(1) default '0',
  trusted_for_saml tinyint(1) default '0',
  primary key(objectid),
  unique (subject_dn),
  unique (name)
) TYPE=InnoDB;

CREATE TABLE fed_user (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  name varchar(128) NOT NULL,
  provider_oid bigint(20) NOT NULL,
  subject_dn varchar(255),
  email varchar(128) default NULL,
  login varchar(32),
  first_name varchar(32) default NULL,
  last_name varchar(32) default NULL,
  PRIMARY KEY (objectid),
  INDEX i_provider_oid (provider_oid),
  INDEX i_email (email),
  INDEX i_login (login),
  INDEX i_subject_dn (subject_dn),
  UNIQUE KEY i_name (provider_oid, name)
) Type=InnoDB;

CREATE TABLE fed_group (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  provider_oid bigint(20) NOT NULL,
  name varchar(128) NOT NULL,
  description text,
  PRIMARY KEY  (objectid),
  INDEX i_provider_oid (provider_oid),
  UNIQUE KEY i_name (provider_oid, name)
) TYPE=InnoDB;

CREATE TABLE fed_user_group (
  provider_oid bigint(20) NOT NULL,
  fed_user_oid bigint(20) NOT NULL,
  fed_group_oid bigint(20) NOT NULL,
  PRIMARY KEY (provider_oid,fed_user_oid,fed_group_oid)
) TYPE=InnoDB;

CREATE TABLE fed_group_virtual (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  provider_oid bigint(20) NOT NULL,
  name varchar(128) NOT NULL,
  description text,
  x509_subject_dn_pattern varchar(255),
  saml_email_pattern varchar(128),
  properties text,
  PRIMARY KEY  (objectid),
  INDEX i_provider_oid (provider_oid),
  INDEX i_x509_subject_dn_pattern (x509_subject_dn_pattern),
  INDEX i_saml_email_pattern (saml_email_pattern),
  UNIQUE KEY i_name (provider_oid, name)
) TYPE=InnoDB;

CREATE TABLE audit_main (
  objectid bigint(20) NOT NULL,
  nodeid varchar(18) NOT NULL,
  time bigint(20) NOT NULL,
  audit_level varchar(12) NOT NULL,
  name varchar(255),
  message varchar(255) NOT NULL,
  ip_address varchar(32) NOT NULL,
  PRIMARY KEY  (objectid),
  KEY idx_nodeid (nodeid),
  KEY idx_time (time),
  KEY idx_ip_address (ip_address),
  KEY idx_level (audit_level)
) TYPE=InnoDB;

CREATE TABLE audit_admin (
  objectid bigint(20) NOT NULL,
  admin_login varchar(32) NOT NULL,
  entity_class varchar(255),
  entity_id bigint(20),
  action char(1),
  PRIMARY KEY  (objectid),
  KEY idx_class (entity_class),
  KEY idx_oid (entity_id)
) TYPE=InnoDB;

CREATE TABLE audit_message (
  objectid bigint(20) NOT NULL,
  status varchar(32) NOT NULL,
  request_id varchar(40) NOT NULL,
  service_oid bigint(20),
  user_name varchar(64),
  authenticated tinyint(1) default '0',
  provider_oid bigint(20),
  user_id varchar(128),
  request_length int(11) NOT NULL,
  response_length int(11),
  request_xml text,
  response_xml text,
  PRIMARY KEY  (objectid),
  KEY idx_status (status),
  KEY idx_request_id (request_id),
  KEY idx_service_oid (service_oid),
  KEY idx_provider_oid (provider_oid),
  KEY idx_user_id (user_id)
) TYPE=InnoDB;

CREATE TABLE audit_system (
  objectid bigint(20) NOT NULL,
  component varchar(32) NOT NULL,
  action varchar(32) NOT NULL,
  PRIMARY KEY (objectid),
  KEY idx_component (component),
  KEY idx_action (action)
) TYPE=InnoDB;


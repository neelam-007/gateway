-- $Id$
-- Fix old
DELETE FROM internal_group where objectid=2;
INSERT INTO internal_group VALUES (2,0,'Gateway Administrators','SecureSpan Manager users with full administrative rights in the SecureSpan Gateway.');
INSERT INTO internal_group VALUES (4,0,'Gateway Operators','SecureSpan Manager users with partial read-only rights in the SecureSpan Gateway.');

ALTER TABLE internal_user DROP COLUMN title;
UPDATE internal_user SET name=login WHERE name IS NULL;
ALTER TABLE published_service ADD soap char(1) default 1; 
ALTER TABLE published_service ADD routing_uri varchar(128);
UPDATE published_service SET soap=1;

ALTER TABLE client_cert ADD user_id varchar(255) NOT NULL;

ALTER TABLE service_resolution MODIFY soapaction varchar(255) default '';
ALTER TABLE service_resolution MODIFY urn varchar(255) default '';
ALTER TABLE service_resolution ADD uri varchar(255) default '';
-- DROP INDEX soapaction;
-- Cannot find previous index name, so we'll leave it there
-- And just make a new one
CREATE INDEX soapaction_i ON service_resolution (soapaction,urn,uri);

CREATE INDEX i_s_l_rqid on ssg_logs (strrequestid);

ALTER TABLE cluster_info ADD multicast_address varchar(16);

ALTER TABLE identity_provider MODIFY description varchar2(1024);

-- New tables

CREATE TABLE trusted_cert (
  objectid number(38,0) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  subject_dn varchar(255) NOT NULL,
  cert_base64 CLOB NOT NULL,
  trusted_for_ssl char(1) default '0',
  trusted_for_client char(1) default '0',
  trusted_for_server char(1) default '0',
  trusted_for_saml char(1) default '0',
  primary key(objectid)
);

CREATE UNIQUE INDEX i_tc_sdn on trusted_cert (subject_dn);
CREATE UNIQUE INDEX i_tc_n   on trusted_cert (name);

CREATE TABLE fed_user (
  objectid number(38,0) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  provider_oid number(38,0) NOT NULL,
  subject_dn varchar(255),
  email varchar(128) default NULL,
  login varchar(32),
  first_name varchar(32) default NULL,
  last_name varchar(32) default NULL,
  PRIMARY KEY (objectid)
);

CREATE UNIQUE INDEX i_fu_name ON fed_user (provider_oid, name);
CREATE INDEX i_fu_provider_oid ON fed_user (provider_oid);
CREATE INDEX i_fu_email ON fed_user (email);
CREATE INDEX i_fu_login ON fed_user (login);
CREATE INDEX i_fu_subject_dn ON fed_user (subject_dn);

CREATE TABLE fed_group (
  objectid number(38,0) NOT NULL,
  version integer NOT NULL,
  provider_oid number(38,0) NOT NULL,
  name varchar(128) NOT NULL,
  description varchar2(1024),
  PRIMARY KEY  (objectid)
);

CREATE UNIQUE INDEX i_fg_name ON fed_group (provider_oid, name);
CREATE INDEX i_fg_provider_oid ON fed_group (provider_oid);

CREATE TABLE fed_user_group (
  provider_oid number(38,0) NOT NULL,
  fed_user_oid number(38,0) NOT NULL,
  fed_group_oid number(38,0) NOT NULL,
  PRIMARY KEY (provider_oid,fed_user_oid,fed_group_oid)
);

CREATE TABLE fed_group_virtual (
  objectid number(38,0) NOT NULL,
  version integer NOT NULL,
  provider_oid number(38,0) NOT NULL,
  name varchar(128) NOT NULL,
  description varchar2(1024),
  x509_subject_dn_pattern varchar(255),
  saml_email_pattern varchar(128),
  properties varchar2(1024),
  PRIMARY KEY  (objectid)
);
CREATE UNIQUE INDEX i_fgv_name ON fed_group_virtual (provider_oid, name);
CREATE INDEX i_fgv_provider_oid ON fed_group_virtual (provider_oid);
CREATE INDEX i_fgv_x509_subject_dn_pattern ON fed_group_virtual (x509_subject_dn_pattern);
CREATE INDEX i_fgv_saml_email_pattern ON fed_group_virtual (saml_email_pattern);

CREATE TABLE audit_main (
  objectid number(38,0) NOT NULL,
  nodeid varchar(18) NOT NULL,
  time number(38,0) NOT NULL,
  audit_level varchar(12) NOT NULL,
  name varchar(255),
  message varchar(255) NOT NULL,
  ip_address varchar(32) NOT NULL,
  PRIMARY KEY  (objectid)
);
CREATE INDEX i_am_nodeid ON audit_main (nodeid);
CREATE INDEX i_am_time ON audit_main (time);
CREATE INDEX i_am_level ON audit_main (audit_level);
CREATE INDEX i_am_ip_address ON audit_main (ip_address);

CREATE TABLE audit_admin (
  objectid number(38,0) NOT NULL,
  admin_login varchar(32) NOT NULL,
  entity_class varchar(255),
  entity_id number(38,0),
  action char(1),
  PRIMARY KEY (objectid)
);

CREATE INDEX i_audit_admin_class ON audit_admin (entity_class);
CREATE INDEX i_audit_admin_oid ON audit_admin (entity_id);


CREATE TABLE audit_message (
  objectid number(38,0) NOT NULL,
  status varchar(32) NOT NULL,
  request_id varchar(40) NOT NULL,
  service_oid number(38,0),
  user_name varchar(64),
  authenticated char(1) default '0',
  provider_oid number(38,0),
  user_id varchar(128),
  request_length integer NOT NULL,
  response_length integer,
  request_xml CLOB,
  response_xml CLOB,
  PRIMARY KEY  (objectid)
);

CREATE INDEX i_audit_message_status ON audit_message (status);
CREATE INDEX i_audit_message_request_id ON audit_message (request_id);
CREATE INDEX i_audit_message_service_oid ON audit_message (service_oid);
CREATE INDEX i_audit_message_provider_oid ON audit_message (provider_oid);
CREATE INDEX i_audit_message_user_id ON audit_message (user_id);

CREATE TABLE audit_system (
  objectid number(38,0) NOT NULL,
  component varchar(32) NOT NULL,
  action varchar(32) NOT NULL,
  PRIMARY KEY (objectid)
);
CREATE INDEX i_audit_system_component ON audit_system (component);
CREATE INDEX i_audit_system_action ON audit_system (action);


CREATE TABLE message_id (
  messageid varchar(255) NOT NULL,
  expires number(38,0) NOT NULL,
  PRIMARY KEY (messageid)
);


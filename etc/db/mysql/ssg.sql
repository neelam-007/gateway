--
-- $Id$
--
-- MySQL version of SSG database creation script.
--
-- NOTE: all changes to this script must also be made to ../postgres/ssg.sql as well!
--

-- MySQL dump 8.22
--
-- Host: localhost    Database: ssg
---------------------------------------------------------
-- Server version	3.23.56-log

SET FOREIGN_KEY_CHECKS = 0;

--
-- Table structure for table 'hibernate_unique_key'
--

DROP TABLE IF EXISTS hibernate_unique_key;
CREATE TABLE hibernate_unique_key (
  next_hi int(11) default NULL
) TYPE=InnoDB;

--
-- Dumping data for table 'hibernate_unique_key'
--


INSERT INTO hibernate_unique_key VALUES (70);

--
-- Table structure for table 'identity_provider'
--

DROP TABLE IF EXISTS identity_provider;
CREATE TABLE identity_provider (
  objectid bigint(20) NOT NULL,
  version int(11) default NULL,
  name varchar(128) NOT NULL,
  description mediumtext default '',
  type bigint(20) NOT NULL,
  properties mediumtext,
  PRIMARY KEY  (objectid),
  UNIQUE KEY ipnm_idx (name)
) TYPE=InnoDB;

--
-- Dumping data for table 'identity_provider'
--



--
-- Table structure for table 'internal_group'
--

DROP TABLE IF EXISTS internal_group;
CREATE TABLE internal_group (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  name varchar(128) NOT NULL,
  description mediumtext,
  PRIMARY KEY  (objectid),
  UNIQUE KEY g_idx (name)
) TYPE=InnoDB;

--
-- Dumping data for table 'internal_group'
--


INSERT INTO internal_group VALUES (2,0,'Gateway Administrators','SecureSpan Manager users with full administrative rights in the SecureSpan Gateway.');
INSERT INTO internal_group VALUES (4,0,'Gateway Operators','SecureSpan Manager users with partial read-only rights in the SecureSpan Gateway.');

--
-- Table structure for table 'internal_user'
--

DROP TABLE IF EXISTS internal_user;
CREATE TABLE internal_user (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  name varchar(128) default NULL,
  login varchar(255) NOT NULL,
  password varchar(32) NOT NULL,
  first_name varchar(32) default NULL,
  last_name varchar(32) default NULL,
  email varchar(128) default NULL,
  expiration bigint(20) NOT NULL,
  PRIMARY KEY  (objectid),
  UNIQUE KEY l_idx (login)
) TYPE=InnoDB;

--
-- Dumping data for table 'internal_user'
--


INSERT INTO internal_user VALUES (3,0,'admin','admin','a41306e4b1b5858d3e3d705dd2e738e2','fname','lname','email',-1);

--
-- Table structure for table 'internal_user_group'
--

DROP TABLE IF EXISTS internal_user_group;
CREATE TABLE internal_user_group (
  internal_user bigint(20) NOT NULL,
  internal_group bigint(20) NOT NULL,
  PRIMARY KEY  (internal_user,internal_group),
  UNIQUE KEY  (internal_user,internal_group)
) TYPE=InnoDB;

--
-- Dumping data for table 'internal_user_group'
--


INSERT INTO internal_user_group VALUES (3,2);

--
-- Table structure for table 'published_service'
--

DROP TABLE IF EXISTS published_service;
CREATE TABLE published_service (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  name varchar(255) NOT NULL,
  policy_xml mediumtext,
  wsdl_url varchar(255),
  wsdl_xml mediumtext,
  disabled TINYINT(1) NOT NULL DEFAULT 0,
  soap TINYINT(1) NOT NULL DEFAULT 1,
  routing_uri varchar(128),
  PRIMARY KEY  (objectid)
) TYPE=InnoDB;

--
-- Dumping data for table 'published_service'
--

--
-- Table structure for table 'client_cert'
--

DROP TABLE IF EXISTS client_cert;
CREATE TABLE client_cert (
  objectid bigint NOT NULL,
  provider bigint NOT NULL,
  user_id varchar(255),
  login varchar(255),
  cert mediumtext DEFAULT NULL,
  reset_counter int NOT NULL,
  PRIMARY KEY  (objectid)
) TYPE=InnoDB;

--
-- Dumping data for table 'client_cert'
--

--
-- Table structure for table 'service_resolution'
--

DROP TABLE IF EXISTS service_resolution;
CREATE TABLE service_resolution (
  objectid bigint(20) NOT NULL,
  serviceid bigint NOT NULL,
  soapaction varchar(255) character set latin1 BINARY default '',
  urn varchar(255) character set latin1 BINARY default '',
  uri varchar(255) character set latin1 BINARY default '',
  unique(soapaction, urn, uri),
  PRIMARY KEY (objectid)
) TYPE=InnoDB;

--
-- Dumping data for table 'service_resolution'
--

--
-- Table structure for table 'cluster_info'
--

DROP TABLE IF EXISTS cluster_info;

CREATE TABLE cluster_info (
  mac varchar(18) NOT NULL,
  name varchar(128) NOT NULL,
  address varchar(16) NOT NULL,
  multicast_address varchar(16),
  ismaster TINYINT(1) NOT NULL,
  uptime bigint NOT NULL,
  avgload double NOT NULL,
  statustimestamp bigint NOT NULL,
  PRIMARY KEY(mac)
)  TYPE=InnoDB;

--
-- Dumping data for table 'cluster_info'
--

--
-- Table structure for table 'service_usage'
--

DROP TABLE IF EXISTS service_usage;
CREATE TABLE service_usage (
  serviceid bigint NOT NULL,
  nodeid varchar(18) NOT NULL,
  requestnr bigint NOT NULL,
  authorizedreqnr bigint NOT NULL,
  completedreqnr bigint NOT NULL,
  primary key(serviceid, nodeid)
) TYPE=InnoDB;

--
-- Dumping data for table 'service_usage'
--

--
-- Table structure for table 'jms_connection'
--

DROP TABLE IF EXISTS jms_connection;
CREATE TABLE jms_connection (
  objectid bigint NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  jndi_url varchar(255) NOT NULL,
  factory_classname varchar(255) NOT NULL,
  destination_factory_url varchar(255) default '',
  queue_factory_url varchar(255) default '',
  topic_factory_url varchar(255) default '',
  username varchar(32) default '',
  password varchar(32) default '',
  primary key(objectid)
) TYPE=InnoDB;

--
-- Table structure for table 'jms_endpoint'
--

DROP TABLE IF EXISTS jms_endpoint;
CREATE TABLE jms_endpoint(
  objectid bigint NOT NULL,
  version integer NOT NULL,
  connection_oid bigint NOT NULL,
  name varchar(128) NOT NULL,
  destination_name varchar(128) NOT NULL,
  reply_type integer default '0',
  username varchar(32) default '',
  password varchar(32) default '',
  max_concurrent_requests integer default '1',
  is_message_source tinyint default '0',
  primary key(objectid)
) TYPE=InnoDB;

--
-- Table structure for table 'trusted_cert'
--

DROP TABLE IF EXISTS trusted_cert;
CREATE TABLE trusted_cert (
  objectid bigint NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  subject_dn varchar(255) NOT NULL,
  cert_base64 mediumtext NOT NULL,
  trusted_for_ssl tinyint(1) default '0',
  trusted_for_client tinyint(1) default '0',
  trusted_for_server tinyint(1) default '0',
  trusted_for_saml tinyint(1) default '0',
  primary key(objectid),
  unique (subject_dn),
  unique (name)
) TYPE=InnoDB;

DROP TABLE IF EXISTS fed_user;
CREATE TABLE fed_user (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  name varchar(128) NOT NULL,
  provider_oid bigint(20) NOT NULL,
  subject_dn varchar(255),
  email varchar(128) default NULL,
  login varchar(255),
  first_name varchar(32) default NULL,
  last_name varchar(32) default NULL,
  PRIMARY KEY (objectid),
  INDEX i_provider_oid (provider_oid),
  INDEX i_email (email),
  INDEX i_login (login),
  INDEX i_subject_dn (subject_dn),
  UNIQUE KEY i_name (provider_oid, name)
) Type=InnoDB;

DROP TABLE IF EXISTS fed_group;
CREATE TABLE fed_group (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  provider_oid bigint(20) NOT NULL,
  name varchar(128) NOT NULL,
  description mediumtext,
  PRIMARY KEY  (objectid),
  INDEX i_provider_oid (provider_oid),
  UNIQUE KEY i_name (provider_oid, name)
) TYPE=InnoDB;

DROP TABLE IF EXISTS fed_user_group;
CREATE TABLE fed_user_group (
  provider_oid bigint(20) NOT NULL,
  fed_user_oid bigint(20) NOT NULL,
  fed_group_oid bigint(20) NOT NULL,
  PRIMARY KEY (provider_oid,fed_user_oid,fed_group_oid)
) TYPE=InnoDB;

DROP TABLE IF EXISTS fed_group_virtual;
CREATE TABLE fed_group_virtual (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  provider_oid bigint(20) NOT NULL,
  name varchar(128) NOT NULL,
  description mediumtext,
  x509_subject_dn_pattern varchar(255),
  saml_email_pattern varchar(128),
  properties mediumtext,
  PRIMARY KEY  (objectid),
  INDEX i_provider_oid (provider_oid),
  INDEX i_x509_subject_dn_pattern (x509_subject_dn_pattern),
  INDEX i_saml_email_pattern (saml_email_pattern),
  UNIQUE KEY i_name (provider_oid, name)
) TYPE=InnoDB;

--
-- Table structure for table `audit`
--

DROP TABLE IF EXISTS audit_main;
CREATE TABLE audit_main (
  objectid bigint(20) NOT NULL,
  nodeid varchar(18) NOT NULL,
  time bigint(20) NOT NULL,
  audit_level varchar(12) NOT NULL,
  name varchar(255),
  message varchar(255) NOT NULL,
  ip_address varchar(32),
  PRIMARY KEY  (objectid),
  KEY idx_nodeid (nodeid),
  KEY idx_time (time),
  KEY idx_ip_address (ip_address),
  KEY idx_level (audit_level)
) TYPE=InnoDB;

--
-- Table structure for table `audit_admin`
--

DROP TABLE IF EXISTS audit_admin;
CREATE TABLE audit_admin (
  objectid bigint(20) NOT NULL,
  admin_login varchar(255) NOT NULL,
  entity_class varchar(255),
  entity_id bigint(20),
  action char(1),
  PRIMARY KEY  (objectid),
  KEY idx_class (entity_class),
  KEY idx_oid (entity_id),
  FOREIGN KEY (objectid) REFERENCES audit_main (objectid) ON DELETE CASCADE
) TYPE=InnoDB;

--
-- Table structure for table `audit_message`
--

DROP TABLE IF EXISTS audit_message;
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
  request_xml mediumtext,
  response_xml mediumtext,
  PRIMARY KEY  (objectid),
  KEY idx_status (status),
  KEY idx_request_id (request_id),
  KEY idx_service_oid (service_oid),
  KEY idx_provider_oid (provider_oid),
  KEY idx_user_id (user_id),
  FOREIGN KEY (objectid) REFERENCES audit_main (objectid) ON DELETE CASCADE
) TYPE=InnoDB;

DROP TABLE IF EXISTS audit_system;
CREATE TABLE audit_system (
  objectid bigint(20) NOT NULL,
  component_id integer NOT NULL,
  action varchar(32) NOT NULL,
  PRIMARY KEY (objectid),
  KEY idx_component_id (component_id),
  KEY idx_action (action),
  FOREIGN KEY (objectid) REFERENCES audit_main (objectid) ON DELETE CASCADE
) TYPE=InnoDB;

DROP TABLE IF EXISTS audit_detail;
CREATE TABLE audit_detail (
  objectid bigint(20) NOT NULL,
  audit_oid bigint(20) NOT NULL,
  time bigint(20) NOT NULL,
  component_id integer,
  ordinal integer,
  message_id integer NOT NULL,
  exception MEDIUMTEXT,
  PRIMARY KEY (objectid),
  KEY idx_component_id (component_id),
  KEY idx_audit_oid (audit_oid),
  FOREIGN KEY (audit_oid) REFERENCES audit_main (objectid) ON DELETE CASCADE
) TYPE=InnoDB;

DROP TABLE IF EXISTS audit_detail_params;
CREATE TABLE audit_detail_params (
  audit_detail_oid bigint(20) NOT NULL,
  position integer NOT NULL,
  value varchar(255) NOT NULL,
  PRIMARY KEY (audit_detail_oid, position),
  FOREIGN KEY (audit_detail_oid) REFERENCES audit_detail (objectid) ON DELETE CASCADE
) Type=InnoDB;

DROP TABLE IF EXISTS message_id;
CREATE TABLE message_id (
  messageid varchar(255) NOT NULL PRIMARY KEY,
  expires bigint(20) NOT NULL
) TYPE=InnoDB;

DROP TABLE IF EXISTS counters;
CREATE TABLE counters (
  counterid bigint(20) NOT NULL,
  userid varchar(128),
  providerid bigint(20) NOT NULL,
  countername varchar(128) NOT NULL,
  cnt_sec bigint(20) default 0,
  cnt_hr bigint(20) default 0,
  cnt_day bigint(20) default 0,
  cnt_mnt bigint(20) default 0,
  last_update bigint(20) default 0,
  unique(userid, providerid, countername),
  PRIMARY KEY (counterid)
) TYPE=InnoDB;

SET FOREIGN_KEY_CHECKS = 1;

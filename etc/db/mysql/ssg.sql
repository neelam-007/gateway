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
  objectid bigint(20) NOT NULL default '0',
  version int(11) default NULL,
  name varchar(128) NOT NULL default '',
  description mediumtext default '',
  type bigint(20) NOT NULL default '0',
  properties text,
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
  objectid bigint(20) NOT NULL default '0',
  version int(11) NOT NULL default '0',
  name varchar(128) NOT NULL default '',
  description mediumtext,
  PRIMARY KEY  (objectid),
  UNIQUE KEY g_idx (name)
) TYPE=InnoDB;

--
-- Dumping data for table 'internal_group'
--


INSERT INTO internal_group VALUES (2,0,'Gateway Administrators','Admin console users having administration rights to the gateway');

--
-- Table structure for table 'internal_user'
--

DROP TABLE IF EXISTS internal_user;
CREATE TABLE internal_user (
  objectid bigint(20) NOT NULL default '0',
  version int(11) NOT NULL default '0',
  name varchar(128) default NULL,
  login varchar(32) NOT NULL default '',
  password varchar(32) NOT NULL default '',
  first_name varchar(32) default NULL,
  last_name varchar(32) default NULL,
  email varchar(128) default NULL,
  title varchar(64) default NULL,
  PRIMARY KEY  (objectid),
  UNIQUE KEY l_idx (login)
) TYPE=InnoDB;

--
-- Dumping data for table 'internal_user'
--


INSERT INTO internal_user VALUES (3,0,'admin','admin','a41306e4b1b5858d3e3d705dd2e738e2','fname','lname','email','title');

--
-- Table structure for table 'internal_user_group'
--

DROP TABLE IF EXISTS internal_user_group;
CREATE TABLE internal_user_group (
  internal_user bigint(20) NOT NULL default '0',
  internal_group bigint(20) NOT NULL default '0',
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
  objectid bigint(20) NOT NULL default '0',
  version int(11) NOT NULL default '0',
  name varchar(255) NOT NULL default '',
  policy_xml text,
  wsdl_url varchar(255) NOT NULL default '',
  wsdl_xml text,
  disabled TINYINT(1) NOT NULL default '0',
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
  objectid bigint NOT NULL default '0',
  provider bigint NOT NULL default '0',
  login varchar(255) NOT NULL default '',
  cert text DEFAULT NULL,
  reset_counter int NOT NULL default '0',
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
  objectid bigint(20) NOT NULL default '0',
  serviceid bigint NOT NULL,
  soapaction varchar(128) default '',
  urn varchar(255) default '',
  unique(soapaction, urn),
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
  mac varchar(18) NOT NULL default '',
  name varchar(128) NOT NULL default '',
  address varchar(16) NOT NULL default '',
  ismaster TINYINT(1) NOT NULL default '0',
  uptime bigint NOT NULL default '0',
  avgload double NOT NULL default '0',
  statustimestamp bigint NOT NULL default '0',
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
  serviceid bigint NOT NULL default '0',
  nodeid varchar(18) NOT NULL default '',
  requestnr bigint NOT NULL default '0',
  authorizedreqnr bigint NOT NULL default '0',
  completedreqnr bigint NOT NULL default '0',
  primary key(serviceid, nodeid)
) TYPE=InnoDB;

--
-- Dumping data for table 'service_usage'
--

--
-- Table structure for table 'ssg_logs'
--

DROP TABLE IF EXISTS ssg_logs;
CREATE TABLE ssg_logs (
  objectid bigint NOT NULL default '0',
  nodeid varchar(18) NOT NULL default '',
  message text,
  strlvl varchar(12),
  loggername varchar(128),
  millis bigint,
  sourceclassname varchar(128),
  sourcemethodname varchar(128),
  strrequestid varchar(40),
  PRIMARY KEY(objectid),
  KEY idx_millis(millis)
) TYPE=InnoDB;

--
-- Dumping data for table 'ssg_logs'
--

--
-- Table structure for table 'jms_connection'
--

DROP TABLE IF EXISTS jms_connection;
CREATE TABLE jms_connection (
  objectid bigint NOT NULL default '0',
  version integer NOT NULL default '0',
  name varchar(128) NOT NULL default '',
  jndi_url varchar(255) NOT NULL default '',
  factory_classname varchar(255) NOT NULL default '',
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
  objectid bigint NOT NULL default '0',
  version integer NOT NULL default '0',
  connection_oid bigint NOT NULL default '0',
  name varchar(128) NOT NULL default '',
  destination_name varchar(128) NOT NULL default '',
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
  cert_base64 text NOT NULL,
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
  login varchar(32),

  first_name varchar(32) default NULL,
  last_name varchar(32) default NULL,
  title varchar(64) default NULL,

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
  properties text,

  PRIMARY KEY  (objectid),
  INDEX i_provider_oid (provider_oid),
  INDEX i_x509_subject_dn_pattern (x509_subject_dn_pattern),
  INDEX i_saml_email_pattern (saml_email_pattern),
  UNIQUE KEY i_name (provider_oid, name)
) TYPE=InnoDB;



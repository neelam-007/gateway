--
-- $Id$
--
-- PostgreSQL version of SSG database creation script.
--
-- "oid" is a built-in column in PostgreSQL, so all columns named "oid" have been renamed to "objectid"
--
-- NOTE: all changes to this script must also be made to ../mysql/ssg.sql as well!
--

DROP TABLE hibernate_unique_key;

CREATE TABLE hibernate_unique_key (
  next_hi integer
);

--
-- Dumping data for table 'hibernate_unique_key'
--


INSERT INTO hibernate_unique_key VALUES (70);

--
-- Table structure for table 'identity_provider'
--

DROP TABLE identity_provider;
CREATE TABLE identity_provider (
  objectid bigint NOT NULL PRIMARY KEY default '0',
  version int default NULL,
  name varchar(128) NOT NULL,
  description text default '',
  type bigint NOT NULL,
  properties text,
  UNIQUE (name)
);

--
-- Dumping data for table 'identity_provider'
--



--
-- Table structure for table 'internal_group'
--

DROP TABLE internal_group;
CREATE TABLE internal_group (
  objectid bigint NOT NULL primary key default '0',
  version int NOT NULL,
  name varchar(128) NOT NULL,
  description text
);

alter table internal_group add unique ( name );


--
-- Dumping data for table 'internal_group'
--


INSERT INTO internal_group VALUES (2,0,'Gateway Administrators','Admin console users having full administration rights to the gateway');
INSERT INTO internal_group VALUES (4,0,'Gateway Operators','Admin console users having read only administration rights to the gateway');

--
-- Table structure for table 'internal_user'
--

DROP TABLE internal_user;
CREATE TABLE internal_user (
  objectid bigint NOT NULL primary key default '0',
  version int NOT NULL,
  name varchar(128) default NULL,
  login varchar(32) NOT NULL,
  password varchar(32) NOT NULL,
  first_name varchar(32) default NULL,
  last_name varchar(32) default NULL,
  email varchar(128) default NULL
);

alter table internal_user add unique ( login );
alter table internal_user add unique ( name );

--
-- Dumping data for table 'internal_user'
--


INSERT INTO internal_user VALUES (3,0,'admin','admin','a41306e4b1b5858d3e3d705dd2e738e2','fname','lname','email');

--
-- Table structure for table 'internal_user_group'
--

DROP TABLE internal_user_group;
CREATE TABLE internal_user_group (
  internal_user bigint NOT NULL,
  internal_group bigint NOT NULL,
  PRIMARY KEY  (internal_user,internal_group)
);

--
-- Dumping data for table 'internal_user_group'
--


INSERT INTO internal_user_group VALUES (3,2);

--
-- Table structure for table 'published_service'
--

DROP TABLE published_service;
CREATE TABLE published_service (
  objectid bigint NOT NULL primary key,
  version int NOT NULL,
  name varchar(255) NOT NULL,
  policy_xml text NOT NULL,
  wsdl_url varchar(255) NOT NULL,
  wsdl_xml text NOT NULL,
  disabled boolean NOT NULL,
  soap boolean NOT NULL,
  routing_uri varchar(128)
);

--
-- Dumping data for table 'published_service'
--

--
-- Table structure for table 'client_cert'
--

DROP TABLE client_cert;
CREATE TABLE client_cert (
  objectid bigint NOT NULL primary key default '0',
  provider bigint NOT NULL,
  user_id varchar(255),
  login varchar(255) NOT NULL,
  cert text DEFAULT NULL,
  reset_counter int NOT NULL
);

--
-- Dumping data for table 'client_cert'
--

--
-- Table structure for table 'service_resolution'
--

DROP TABLE service_resolution;
CREATE TABLE service_resolution (
  objectid bigint NOT NULL PRIMARY KEY,
  serviceid bigint NOT NULL,
  soapaction varchar(255),
  urn varchar(255),
  uri varchar(255),
  unique(soapaction, urn, uri)
);

--
-- Dumping data for table 'service_resolution'
--

--
-- Table structure for table 'urlcache'
--


--
-- Dumping data for table 'urlcache'
--

--
-- Table structure for table 'cluster_info'
--

DROP TABLE cluster_info;
CREATE TABLE cluster_info (
  mac varchar(18) NOT NULL,
  name varchar(128) NOT NULL,
  address varchar(16) NOT NULL,
  multicast_address varchar(16),
  ismaster boolean NOT NULL,
  uptime bigint NOT NULL,
  avgload float8 NOT NULL,
  statustimestamp bigint NOT NULL,
  PRIMARY KEY(mac)
);

--
-- Dumping data for table 'cluster_info'
--

--
-- Table structure for table 'service_usage'
--

DROP TABLE service_usage;
CREATE TABLE service_usage (
  serviceid bigint NOT NULL,
  nodeid varchar(18) NOT NULL,
  requestnr bigint NOT NULL,
  authorizedreqnr bigint NOT NULL,
  completedreqnr bigint NOT NULL,
  primary key(serviceid, nodeid)
);

--
-- Dumping data for table 'service_usage'
--

--
-- Table structure for table 'ssg_logs'
--

DROP TABLE ssg_logs;
CREATE TABLE ssg_logs (
  objectid bigint NOT NULL,
  nodeid varchar(18) NOT NULL,
  message text,
  strlvl varchar(12),
  loggername varchar(128),
  millis bigint,
  sourceclassname varchar(128),
  sourcemethodname varchar(128),
  strrequestid varchar(40),
  PRIMARY KEY(objectid)
);
CREATE INDEX idx_nodeid ON ssg_logs (nodeid);
CREATE INDEX idx_millis ON ssg_logs (millis);

--
-- Dumping data for table 'ssg_logs'
--

--
-- Table structure for table 'jms_connection'
--

DROP TABLE jms_connection;
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
);

--
-- Table structure for table 'jms_endpoint'
--

DROP TABLE jms_endpoint;
CREATE TABLE jms_endpoint (
  objectid bigint NOT NULL,
  version integer NOT NULL,
  connection_oid bigint NOT NULL,
  name varchar(128) NOT NULL,
  destination_name varchar(128) NOT NULL,
  reply_type integer default '0',
  username varchar(32) default '',
  password varchar(32) default '',
  max_concurrent_requests integer default '1',
  is_message_source boolean default 'F',
  primary key(objectid)
);

--
-- Table structure for table 'trusted_cert'
--

DROP TABLE trusted_cert;
CREATE TABLE trusted_cert (
  objectid bigint NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  subject_dn varchar(255) NOT NULL,
  cert_base64 text NOT NULL,
  trusted_for_ssl boolean default 'F',
  trusted_for_client boolean default 'F',
  trusted_for_server boolean default 'F',
  trusted_for_saml boolean default 'F',
  primary key(objectid),
  unique(subject_dn)
);

DROP TABLE fed_user;
CREATE TABLE fed_user (
  objectid bigint NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,

  provider_oid bigint NOT NULL,
  subject_dn varchar(255),
  email varchar(128),
  login varchar(32),

  first_name varchar(32) default NULL,
  last_name varchar(32) default NULL,

  PRIMARY KEY (objectid),
  INDEX i_provider_oid (provider_oid),
  INDEX i_email (email),
  INDEX i_login (login),
  INDEX i_subject_dn (subject_dn),
  UNIQUE KEY i_name (provider_oid, name)
);

DROP TABLE fed_group;
CREATE TABLE fed_group (
  objectid bigint NOT NULL,
  version integer NOT NULL,
  provider_oid bigint NOT NULL,
  name varchar(128) NOT NULL,
  description mediumtext,

  PRIMARY KEY  (objectid),
  INDEX i_provider_oid (provider_oid),
  UNIQUE KEY i_name (provider_oid, name)
);

DROP TABLE fed_user_group;
CREATE TABLE fed_user_group (
  provider_oid bigint NOT NULL,
  fed_user_oid bigint NOT NULL,
  fed_group_oid bigint NOT NULL,
  PRIMARY KEY (provider_oid,fed_user_oid,fed_group_oid)
);

DROP TABLE fed_group_virtual;
CREATE TABLE fed_group_virtual (
  objectid bigint NOT NULL,
  version integer NOT NULL,
  provider_oid bigint NOT NULL,
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
);

--
-- Table structure for table `audit`
--

DROP TABLE audit_main;
CREATE TABLE audit_main (
  objectid bigint NOT NULL,
  nodeid varchar(18) NOT NULL,
  time bigint NOT NULL,
  audit_level varchar(12) NOT NULL,
  message varchar(255) NOT NULL,
  name varchar(255),
  ip_address varchar(32) NOT NULL,
  PRIMARY KEY  (objectid),
  INDEX idx_nodeid (nodeid),
  INDEX idx_time (time),
  INDEX idx_level (audit_level),
  INDEX idx_ip_address (ip_address)
);

--
-- Table structure for table `audit_admin`
--

DROP TABLE audit_admin;
CREATE TABLE audit_admin (
  objectid bigint NOT NULL,
  admin_login varchar(32) NOT NULL,
  entity_class varchar(255),
  entity_id number(38,0),
  action char(1),
  PRIMARY KEY  (objectid),
  INDEX idx_class (entity_class),
  INDEX idx_oid (entity_id)
);

--
-- Table structure for table `audit_message`
--

DROP TABLE audit_message;
CREATE TABLE audit_message (
  objectid bigint NOT NULL,
  status varchar(32) NOT NULL,
  request_id varchar(40) NOT NULL,
  service_oid bigint,
  user_name varchar(64),
  authenticated boolean default 'F',
  provider_oid bigint,
  user_id varchar(128),
  request_length integer NOT NULL,
  response_length integer,
  request_xml text,
  response_xml text,
  PRIMARY KEY  (objectid),
  INDEX idx_status (status),
  INDEX idx_request_id (request_id),
  INDEX idx_service_oid (service_oid),
  INDEX idx_provider_oid (provider_oid),
  INDEX idx_user_id (user_id)
);

DROP TABLE audit_system;
CREATE TABLE audit_system (
  objectid bigint NOT NULL,
  component varchar(32) NOT NULL,
  action varchar(32) NOT NULL,
  PRIMARY KEY (objectid),
  INDEX idx_component (component),
  INDEX idx_action (action)
);
--
-- $Id$
-- Oracle version of SSG database creation script.
--

drop TABLE hibernate_unique_key;
CREATE TABLE hibernate_unique_key (
  next_hi integer
);

--
-- Dumping data for table 'hibernate_unique_key'
--


INSERT INTO hibernate_unique_key VALUES (0);

--
-- Table structure for table 'identity_provider'
--

drop TABLE identity_provider ;
CREATE TABLE identity_provider (
  objectid number(38,0) NOT NULL PRIMARY KEY ,
  version int NOT NULL ,
  name varchar(128) NOT NULL ,
  description varchar2(1024),
  type int NOT NULL ,
  properties clob,
  UNIQUE (name)
);

--
-- Dumping data for table 'identity_provider'
--

--
-- Table structure for table 'internal_group'
--
drop table internal_group;

CREATE TABLE internal_group (
  objectid number(38,0) NOT NULL primary key ,
  version int NOT NULL ,
  name varchar(128) NOT NULL ,
  description varchar2(1024)
);

alter table internal_group add unique ( name );


--
-- Dumping data for table 'internal_group'
--


INSERT INTO internal_group VALUES (2,0,'Gateway Administrators','Admin console users having administration rights to the gateway');
--
-- Table structure for table 'internal_user'
--
drop table internal_user;

CREATE TABLE internal_user (
  objectid number(38,0) NOT NULL primary key ,
  version int NOT NULL ,
  name varchar(128) ,
  login varchar(32) NOT NULL ,
  password varchar(32) NOT NULL ,
  first_name varchar(32) ,
  last_name varchar(32) ,
  email varchar(128) ,
  title varchar(64) 
);

alter table internal_user add unique ( login );
alter table internal_user add unique ( name );

--
-- Dumping data for table 'internal_user'
--


INSERT INTO internal_user VALUES (3,0,'admin','admin','a41306e4b1b5858d3e3d705dd2e738e2','fname','lname','email','title');

--
-- Table structure for table 'internal_user_group'
--
drop table internal_user_group;

CREATE TABLE internal_user_group (
  internal_user number(38,0) NOT NULL ,
  internal_group number(38,0) NOT NULL ,
  PRIMARY KEY  (internal_user,internal_group)
);

--
-- Dumping data for table 'internal_user_group'
--


INSERT INTO internal_user_group VALUES (3,2);

--
-- Table structure for table 'published_service'
--
drop table published_service;
CREATE TABLE published_service (
  objectid number(38,0) NOT NULL primary key ,
  version int NOT NULL ,
  name varchar(255) NOT NULL ,
  policy_xml clob NOT NULL ,
  wsdl_url varchar(255) NOT NULL ,
  wsdl_xml clob NOT NULL ,
  disabled char(1) NOT NULL 
);

--
-- Dumping data for table 'published_service'
--

--
-- Table structure for table 'client_cert'
--
drop table client_cert;

CREATE TABLE client_cert (
  objectid number(38,0) NOT NULL primary key ,
  provider number(38,0) NOT NULL ,
  login varchar(255) NOT NULL ,
  cert varchar2(1024) DEFAULT NULL,
  reset_counter int NOT NULL 
);

--
-- Dumping data for table 'client_cert'
--

--
-- Table structure for table 'service_resolution'
--

drop table service_resolution;

CREATE TABLE service_resolution (
  objectid number(38,0) NOT NULL PRIMARY KEY,
  serviceid number(38,0) NOT NULL,
  soapaction varchar(128) default '',
  urn varchar(255) default '',
  unique(soapaction, urn)
);

ALTER TABLE service_resolution

--
-- Dumping data for table 'service_resolution'
--

--
-- Table structure for table 'cluster_info'
--

drop table cluster_info;
CREATE TABLE cluster_info (
  mac varchar(18) NOT NULL ,
  name varchar(128) NOT NULL ,
  address varchar(16) NOT NULL ,
  ismaster char(1) NOT NULL ,
  uptime number(38,0) NOT NULL ,
  avgload number(8,2) NOT NULL ,
  statustimestamp number(38,0) NOT NULL ,
  PRIMARY KEY(mac)
);

--
-- Dumping data for table 'cluster_info'
--

--
-- Table structure for table 'service_usage'
--

drop table service_usage;
CREATE TABLE service_usage (
  serviceid number(38,0) NOT NULL ,
  nodeid varchar(18) NOT NULL ,
  requestnr number(38,0) NOT NULL ,
  authorizedreqnr number(38,0) NOT NULL ,
  completedreqnr number(38,0) NOT NULL ,
  primary key(serviceid, nodeid)
);

--
-- Dumping data for table 'service_usage'
--

--
-- Table structure for table 'ssg_logs'
--

drop table ssg_logs;
CREATE TABLE ssg_logs (
  objectid number(38,0) NOT NULL ,
  nodeid varchar(18) NOT NULL ,
  message varchar2(1024),
  strlvl varchar(12),
  loggername varchar(128),
  millis number(38,0),
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

drop table jms_connection;
CREATE TABLE jms_connection (
  objectid number(38,0) NOT NULL ,
  version integer NOT NULL ,
  name varchar(128) NOT NULL ,
  jndi_url varchar(255) NOT NULL ,
  factory_classname varchar(255) NOT NULL ,
  destination_factory_url varchar(255) ,
  queue_factory_url varchar(255) ,
  topic_factory_url varchar(255) ,
  username varchar(32) ,
  password varchar(32) ,
  primary key(objectid)
);

--
-- Table structure for table 'jms_endpoint'
--

drop table jms_endpoint;

CREATE TABLE jms_endpoint (
  objectid number(38,0) NOT NULL ,
  version integer NOT NULL ,
  connection_oid number(38,0) NOT NULL ,
  name varchar(128) NOT NULL ,
  destination_name varchar(128) NOT NULL ,
  reply_type integer ,
  username varchar(32) ,
  password varchar(32) ,
  max_concurrent_requests integer ,
  is_message_source char(1) ,
  primary key(objectid)
);

--
-- Table structure for table 'trusted_cert'
--

DROP TABLE trusted_cert;
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
  primary key(objectid),
  unique(subject_dn)
);

DROP TABLE fed_user;
CREATE TABLE fed_user (
  objectid number(38,0) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,

  provider_oid number(38,0) NOT NULL,
  subject_dn varchar(255),
  email varchar(128),
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
);

DROP TABLE IF EXISTS fed_group;
CREATE TABLE fed_group (
  objectid number(38,0) NOT NULL,
  version integer NOT NULL,
  provider_oid number(38,0) NOT NULL,
  name varchar(128) NOT NULL,
  description varchar2(1024),

  PRIMARY KEY  (objectid),
  INDEX i_provider_oid (provider_oid),
  UNIQUE KEY i_name (provider_oid, name)
);

DROP TABLE IF EXISTS fed_user_group;
CREATE TABLE fed_user_group (
  provider_oid number(38,0) NOT NULL,
  fed_user_oid number(38,0) NOT NULL,
  fed_group_oid number(38,0) NOT NULL,
  PRIMARY KEY (provider_oid,fed_user_oid,fed_group_oid)
);

DROP TABLE IF EXISTS fed_group_virtual;
CREATE TABLE fed_group_virtual (
  objectid number(38,0)  NOT NULL,
  version integer NOT NULL,
  provider_oid number(38,0)  NOT NULL,
  name varchar(128) NOT NULL,
  description varchar2(1024),

  x509_subject_dn_pattern varchar(255),
  saml_email_pattern varchar(128),
  properties clob,

  PRIMARY KEY  (objectid),
  INDEX i_provider_oid (provider_oid),
  INDEX i_x509_subject_dn_pattern (x509_subject_dn_pattern),
  INDEX i_saml_email_pattern (saml_email_pattern),
  UNIQUE KEY i_name (provider_oid, name)
);



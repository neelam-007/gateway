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
  properties clob
);
CREATE UNIQUE INDEX i_idp_name ON identity_provider (name);

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


INSERT INTO internal_group VALUES (2,0,'Gateway Administrators','Admin console users having full administration rights to the gateway');
INSERT INTO internal_group VALUES (4,0,'Gateway Operators','Admin console users having read only administration rights to the gateway');

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
  email varchar(128)
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
  disabled char(1) NOT NULL ,
  soap char(1) NOT NULL ,
  routing_uri varchar(128)
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
  user_id varchar(255),
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
  serviceid number(38,0) NOT NULL ,
  soapaction varchar(255) default '',
  urn varchar(255) default '',
  uri varchar(255) default '',
  unique(soapaction, urn, uri)
);

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
  multicast_address varchar(16),
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
CREATE INDEX i_log_nodeid ON ssg_logs (nodeid);
CREATE INDEX i_log_millis ON ssg_logs (millis);

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
  PRIMARY KEY (objectid)
);
CREATE UNIQUE INDEX i_fu_name ON fed_user (provider_oid, name);
CREATE INDEX i_fu_provider_oid ON fed_user (provider_oid);
CREATE INDEX i_fu_email ON fed_user (email);
CREATE INDEX i_fu_login ON fed_user (login);
CREATE INDEX i_fu_subject_dn ON fed_user (subject_dn);

DROP TABLE fed_group;
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

DROP TABLE fed_user_group;
CREATE TABLE fed_user_group (
  provider_oid number(38,0) NOT NULL,
  fed_user_oid number(38,0) NOT NULL,
  fed_group_oid number(38,0) NOT NULL,
  PRIMARY KEY (provider_oid,fed_user_oid,fed_group_oid)
);

DROP TABLE fed_group_virtual;
CREATE TABLE fed_group_virtual (
  objectid number(38,0)  NOT NULL,
  version integer NOT NULL,
  provider_oid number(38,0)  NOT NULL,
  name varchar(128) NOT NULL,
  description varchar2(1024),
  x509_subject_dn_pattern varchar(255),
  saml_email_pattern varchar(128),
  properties clob,
  PRIMARY KEY  (objectid)
);
CREATE UNIQUE INDEX i_fgv_name ON fed_group_virtual (provider_oid, name);
CREATE INDEX i_fgv_provider_oid ON fed_group_virtual (provider_oid);
CREATE INDEX i_fgv_x509_subject_dn_pattern ON fed_group_virtual (x509_subject_dn_pattern);
CREATE INDEX i_fgv_saml_email_pattern ON fed_group_virtual (saml_email_pattern);

--
-- Table structure for table `audit`
--

DROP TABLE audit_main;
CREATE TABLE audit_main (
  objectid number(38,0) NOT NULL,
  nodeid varchar(18) NOT NULL,
  time number(38,0) NOT NULL,
  audit_level varchar(12) NOT NULL,
  message varchar(255) NOT NULL,
  ip_address varchar(32) NOT NULL,
  PRIMARY KEY  (objectid)
);
CREATE INDEX i_am_nodeid ON audit_main (nodeid);
CREATE INDEX i_am_time ON audit_main (time);
CREATE INDEX i_am_level ON audit_main (audit_level);
CREATE INDEX i_am_ip_address ON audit_main (ip_address);

--
-- Table structure for table `audit_admin`
--

DROP TABLE audit_admin;
CREATE TABLE audit_admin (
  objectid number(38,0) NOT NULL,
  admin_login varchar(32) NOT NULL,
  entity_class varchar(255),
  entity_id number(38,0),
  action char(1),
  PRIMARY KEY  (objectid)
);
CREATE INDEX i_audit_admin_class ON audit_admin (entity_class);
CREATE INDEX i_audit_admin_oid ON audit_admin (entity_id);

--
-- Table structure for table `audit_message`
--

DROP TABLE audit_message;
CREATE TABLE audit_message (
  objectid number(38,0) NOT NULL,
  status varchar(32) NOT NULL,
  request_id varchar(40) NOT NULL,
  service_oid number(38,0),
  service_name varchar(128),
  user_name varchar(64),
  authenticated char(1),
  provider_oid number(38,0),
  user_id varchar(128),
  request_length integer NOT NULL,
  response_length integer,
  request_xml clob,
  response_xml clob,
  PRIMARY KEY  (objectid)
);
CREATE INDEX i_audit_message_status ON audit_message (status);
CREATE INDEX i_audit_message_request_id ON audit_message (request_id);
CREATE INDEX i_audit_message_service_oid ON audit_message (service_oid);
CREATE INDEX i_audit_message_provider_oid ON audit_message (provider_oid);
CREATE INDEX i_audit_message_user_id ON audit_message (user_id);

DROP TABLE audit_system;
CREATE TABLE audit_system (
  objectid number(38,0) NOT NULL,
  component varchar(32) NOT NULL,
  action varchar(32) NOT NULL,
  PRIMARY KEY (objectid)
);
CREATE INDEX i_audit_system_component ON audit_system (component);
CREATE INDEX i_audit_system_action ON audit_system (action);

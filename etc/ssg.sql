-- MySQL dump 8.22
--
-- Host: localhost    Database: ssg
---------------------------------------------------------
-- Server version	3.23.56

--
-- Table structure for table 'published_service'
--

CREATE TABLE published_service (
  oid bigint(20) not null default '0',
  version int(11) not null default '0',
  name varchar(64) not null default '',
  policy_xml text,
  wsdl_url varchar(255) not null default '',
  wsdl_xml text,
  PRIMARY KEY (oid)
) TYPE=InnoDB;

--
-- Table structure for table 'hibernate_unique_key'
--
--
-- Table structure for table 'hibernate_unique_key'
--

CREATE TABLE hibernate_unique_key (
  next_hi int(11) default NULL
) TYPE=InnoDB;

INSERT INTO hibernate_unique_key VALUES (0);

--
-- Table structure for table 'identity_provider'
--

CREATE TABLE identity_provider (
  oid bigint(20) NOT NULL default '0',
  version int(11) default NULL,
  name varchar(128) NOT NULL default '',
  description mediumtext NOT NULL,
  type bigint(20) NOT NULL default '0',
  PRIMARY KEY  (oid)
) TYPE=InnoDB;

--
-- Table structure for table 'ldap_identity_provider'
--

CREATE TABLE ldap_identity_provider (
  oid bigint(20) NOT NULL default '0',
  version int(11) default NULL,
  name varchar(128) NOT NULL default '',
  description mediumtext NOT NULL,
  type bigint(20) NOT NULL default '0',
  ldap_host_url varchar(128) NOT NULL default '',
  search_base varchar(128) NOT NULL default '',
  PRIMARY KEY  (oid)
) TYPE=InnoDB;

--
-- Table structure for table 'identity_provider_type'
--

CREATE TABLE identity_provider_type (
  oid bigint(20) NOT NULL default '0',
  version int(11) NOT NULL default '0',
  name varchar(128) NOT NULL default '',
  description mediumtext,
  class_name varchar(255) NOT NULL default '',
  PRIMARY KEY  (oid)
) TYPE=InnoDB;

--
-- Table structure for table 'internal_group'
--

CREATE TABLE internal_group (
  oid bigint(20) NOT NULL default '0',
  version int(11) NOT NULL default '0',
  name varchar(128) NOT NULL default '',
  description mediumtext,
  PRIMARY KEY  (oid)
) TYPE=InnoDB;

--
-- Table structure for table 'internal_user'
--

CREATE TABLE internal_user (
  oid bigint(20) NOT NULL default '0',
  version int(11) NOT NULL default '0',
  provider bigint(20) NOT NULL default '0',
  login varchar(32) NOT NULL default '',
  password varchar(32) NOT NULL default '',
  first_name varchar(32) default NULL,
  last_name varchar(32) default NULL,
  email varchar(128) default NULL,
  title varchar(64) default NULL,
  organization bigint(20) default NULL,
  department varchar(128) default NULL,
  address bigint(20) default NULL,
  mailing_address bigint(20) default NULL,
  billing_address bigint(20) default NULL,
  PRIMARY KEY  (oid)
) TYPE=InnoDB;

--
-- Table structure for table 'internal_user_group'
--

CREATE TABLE internal_user_group (
  internal_user bigint(20) NOT NULL default '0',
  internal_group bigint(20) NOT NULL default '0',
  PRIMARY KEY  (internal_user,internal_group)
) TYPE=InnoDB;

--
-- Table structure for table 'object_identity'
--

CREATE TABLE object_identity (
  class_name varchar(255) NOT NULL default '',
  table_name varchar(255) default NULL,
  class_seed smallint(6) NOT NULL auto_increment,
  server_seed smallint(6) default '0',
  key_seed bigint(20) NOT NULL default '0',
  key_batch_size int(11) NOT NULL default '0',
  PRIMARY KEY  (class_seed)
) TYPE=InnoDB;

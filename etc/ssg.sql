-- MySQL dump 8.22
--
-- Host: localhost    Database: ssg
---------------------------------------------------------
-- Server version	3.23.56

--
-- Table structure for table 'address'
--

CREATE TABLE address (
  oid bigint(20) NOT NULL default '0',
  version int(11) NOT NULL default '0',
  address varchar(128) NOT NULL default '',
  address2 varchar(128) default NULL,
  city varchar(64) default NULL,
  state_oid bigint(20) default NULL,
  country_oid bigint(20) default NULL,
  postal_code varchar(64) default NULL,
  PRIMARY KEY  (oid),
  KEY city (city),
  KEY version (version)
) TYPE=InnoDB;

--
-- Table structure for table 'country'
--

CREATE TABLE country (
  oid bigint(20) NOT NULL default '0',
  version int(11) NOT NULL default '0',
  code char(2) NOT NULL default '',
  name varchar(64) NOT NULL default '',
  PRIMARY KEY  (oid),
  KEY version (version,code,name)
) TYPE=InnoDB;

--
-- Table structure for table 'hibernate_unique_key'
--

CREATE TABLE hibernate_unique_key (
  next_hi int(11) default NULL
) TYPE=InnoDB;

--
-- Table structure for table 'identity_provider'
--

CREATE TABLE identity_provider (
  oid bigint(20) NOT NULL default '0',
  version int(11) default NULL,
  name varchar(128) NOT NULL default '',
  description mediumtext NOT NULL,
  type bigint(20) NOT NULL default '0',
  PRIMARY KEY  (oid),
  UNIQUE KEY name (name),
  KEY version (version),
  KEY type (type)
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
  PRIMARY KEY  (oid),
  KEY version (version,name),
  KEY class_name (class_name)
) TYPE=InnoDB;

--
-- Table structure for table 'internal_group'
--

CREATE TABLE internal_group (
  oid bigint(20) NOT NULL default '0',
  version int(11) NOT NULL default '0',
  name varchar(128) NOT NULL default '',
  description mediumtext,
  PRIMARY KEY  (oid),
  KEY version (version)
) TYPE=InnoDB;

--
-- Table structure for table 'internal_organization'
--

CREATE TABLE internal_organization (
  oid bigint(20) NOT NULL default '0',
  version int(11) NOT NULL default '0',
  name varchar(128) NOT NULL default '',
  address_oid bigint(20) default '0',
  billing_address_oid bigint(20) default '0',
  mailing_address_oid bigint(20) default NULL,
  PRIMARY KEY  (oid),
  KEY version (version),
  KEY mailing_address_oid (mailing_address_oid)
) TYPE=InnoDB;

--
-- Table structure for table 'internal_user'
--

CREATE TABLE internal_user (
  oid bigint(20) NOT NULL default '0',
  version int(11) NOT NULL default '0',
  provider_oid bigint(20) NOT NULL default '0',
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
  PRIMARY KEY  (oid),
  UNIQUE KEY email (email),
  KEY provider_oid (provider_oid,login),
  KEY first_name (first_name),
  KEY last_name (last_name),
  KEY mailing_address_oid (mailing_address,billing_address),
  KEY address_oid (address),
  KEY password (password),
  KEY organization_oid (organization)
) TYPE=InnoDB;

--
-- Table structure for table 'internal_user_group'
--

CREATE TABLE internal_user_group (
  user_oid bigint(20) NOT NULL default '0',
  group_oid bigint(20) NOT NULL default '0',
  PRIMARY KEY  (user_oid,group_oid)
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

--
-- Table structure for table 'state'
--

CREATE TABLE state (
  oid bigint(20) NOT NULL default '0',
  country_oid bigint(20) NOT NULL default '0',
  code varchar(16) NOT NULL default '',
  name varchar(64) NOT NULL default '',
  PRIMARY KEY  (oid),
  KEY country_oid (country_oid,code,name)
) TYPE=InnoDB;


--
-- THIS SCRIPT DROPS TABLES!
--
-- $Id$
--

-- MySQL dump 8.22
--
-- Host: spock    Database: ssg
---------------------------------------------------------
-- Server version	3.23.54

--
-- Table structure for table 'address'
--

DROP TABLE IF EXISTS address;
CREATE TABLE address (
  oid bigint(20) NOT NULL default '0',
  version int(11) NOT NULL default '0',
  address varchar(128) NOT NULL default '',
  address2 varchar(128) default NULL,
  city varchar(64) default NULL,
  state bigint(20) default NULL,
  country bigint(20) default NULL,
  postal_code varchar(64) default NULL,
  PRIMARY KEY  (oid)
) TYPE=InnoDB;

--
-- Table structure for table 'country'
--

DROP TABLE IF EXISTS country;
CREATE TABLE country (
  oid bigint(20) NOT NULL default '0',
  version int(11) NOT NULL default '0',
  code char(2) NOT NULL default '',
  name varchar(64) NOT NULL default '',
  PRIMARY KEY  (oid)
) TYPE=InnoDB;

--
-- Table structure for table 'hibernate_unique_key'
--

DROP TABLE IF EXISTS hibernate_unique_key;
CREATE TABLE hibernate_unique_key (
  next_hi int(11) default NULL
) TYPE=InnoDB;

INSERT INTO hibernate_unique_key VALUES (0);

--
-- Table structure for table 'identity_provider'
--

DROP TABLE IF EXISTS identity_provider;
CREATE TABLE identity_provider (
  oid bigint(20) NOT NULL default '0',
  version int(11) default NULL,
  name varchar(128) NOT NULL default '',
  description mediumtext NOT NULL,
  type bigint(20) NOT NULL default '0',
  PRIMARY KEY  (oid)
) TYPE=InnoDB;

--
-- Table structure for table 'identity_provider_type'
--

DROP TABLE IF EXISTS identity_provider_type;
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

DROP TABLE IF EXISTS internal_group;
CREATE TABLE internal_group (
  oid bigint(20) NOT NULL default '0',
  version int(11) NOT NULL default '0',
  provider bigint(20) NOT NULL default '0',
  name varchar(128) NOT NULL default '',
  description mediumtext,
  PRIMARY KEY  (oid)
) TYPE=InnoDB;

--
-- Table structure for table 'internal_organization'
--

DROP TABLE IF EXISTS internal_organization;
CREATE TABLE internal_organization (
  oid bigint(20) NOT NULL default '0',
  version int(11) NOT NULL default '0',
  name varchar(128) NOT NULL default '',
  address bigint(20) default NULL,
  billing_address bigint(20) default NULL,
  mailing_address bigint(20) default NULL,
  PRIMARY KEY  (oid)
) TYPE=InnoDB;

--
-- Table structure for table 'internal_user'
--

DROP TABLE IF EXISTS internal_user;
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

DROP TABLE IF EXISTS internal_user_group;
CREATE TABLE internal_user_group (
  internal_user bigint(20) NOT NULL default '0',
  internal_group bigint(20) NOT NULL default '0',
  PRIMARY KEY  (internal_user,internal_group)
) TYPE=InnoDB;

--
-- Table structure for table 'object_identity'
--

DROP TABLE IF EXISTS object_identity;
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

DROP TABLE IF EXISTS state;
CREATE TABLE state (
  oid bigint(20) NOT NULL default '0',
  country bigint(20) NOT NULL default '0',
  code varchar(16) NOT NULL default '',
  name varchar(64) NOT NULL default '',
  PRIMARY KEY  (oid)
) TYPE=InnoDB;


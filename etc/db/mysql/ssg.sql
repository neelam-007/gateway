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
  oid bigint(20) NOT NULL default '0',
  version int(11) default NULL,
  name varchar(128) NOT NULL default '',
  description mediumtext default '',
  type bigint(20) NOT NULL default '0',
  properties text,
  PRIMARY KEY  (oid),
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
  oid bigint(20) NOT NULL default '0',
  version int(11) NOT NULL default '0',
  name varchar(128) NOT NULL default '',
  description mediumtext,
  PRIMARY KEY  (oid),
  UNIQUE KEY g_idx (name)
) TYPE=InnoDB;

--
-- Dumping data for table 'internal_group'
--


INSERT INTO internal_group VALUES (2,0,'SSGAdmin','Users having administration rights to the ssg');

--
-- Table structure for table 'internal_user'
--

DROP TABLE IF EXISTS internal_user;
CREATE TABLE internal_user (
  oid bigint(20) NOT NULL default '0',
  version int(11) NOT NULL default '0',
  name varchar(128) default NULL,
  login varchar(32) NOT NULL default '',
  password varchar(32) NOT NULL default '',
  first_name varchar(32) default NULL,
  last_name varchar(32) default NULL,
  email varchar(128) default NULL,
  title varchar(64) default NULL,
  cert BLOB DEFAULT NULL,
  cert_reset_counter INT DEFAULT '0',
  PRIMARY KEY  (oid),
  UNIQUE KEY l_idx (login)
) TYPE=InnoDB;

--
-- Dumping data for table 'internal_user'
--


INSERT INTO internal_user VALUES (3,0,'ssgadmin','ssgadmin','309b9c7ab4c3ee2144fce9b071acd440','fname','lname','email','title', NULL, 0);

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
-- Dumping data for table 'object_identity'
--



--
-- Table structure for table 'published_service'
--

DROP TABLE IF EXISTS published_service;
CREATE TABLE published_service (
  oid bigint(20) NOT NULL default '0',
  version int(11) NOT NULL default '0',
  name varchar(255) NOT NULL default '',
  policy_xml text,
  wsdl_url varchar(255) NOT NULL default '',
  wsdl_xml text,
  disabled TINYINT(1) NOT NULL default '0',
  PRIMARY KEY  (oid)
) TYPE=InnoDB;

--
-- Dumping data for table 'published_service'
--

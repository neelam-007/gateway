-- MySQL dump 8.22
--
-- Host: localhost    Database: ssg
---------------------------------------------------------
-- Server version	3.23.56-log

--- those tables are no longer used
DROP TABLE IF EXISTS address;
DROP TABLE IF EXISTS country;

--
-- Table structure for table 'hibernate_unique_key'
--
DROP TABLE IF EXISTS hibernate_unique_key;
CREATE TABLE hibernate_unique_key (
  next_hi int(11) default NULL
) TYPE=InnoDB;

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
  properties text,
  PRIMARY KEY  (oid)
) TYPE=InnoDB;

--
-- Table structure for table 'internal_group'
--

DROP TABLE IF EXISTS internal_group;
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
-- Table structure for table 'published_service'
--

DROP TABLE IF EXISTS published_service;
CREATE TABLE published_service (
  oid bigint(20) NOT NULL default '0',
  version int(11) NOT NULL default '0',
  name varchar(64) NOT NULL default '',
  policy_xml text,
  wsdl_url varchar(255) NOT NULL default '',
  wsdl_xml text,
  PRIMARY KEY  (oid)
) TYPE=InnoDB;


---
--- ACTUAL DATA CREATION
---
--- PLEASE RESPECT THE FOLLOWING RESERVED OIDS
---
--- 2: admin group
--- 3: initial admin user

---
--- Creation of the admin group to which administrator must belong to in order
--- to be authorized to use any admin service functionality from admin console
---

INSERT INTO internal_group VALUES(2, 0, "SSGAdmin", "Users having administration rights to the ssg");

---
--- Creation of the initial admin user
---
INSERT INTO internal_user VALUES(3, 0, 0, "ssgadmin", "ssgadmin", "309b9c7ab4c3ee2144fce9b071acd440", NULL, NULL, NULL, NULL, NULL, NULL);

---
--- Grant ssgadmin ssgadmin rights
---

INSERT INTO internal_user_group VALUES(3, 2);

--
-- Dumping data for table 'hibernate_unique_key'
-- (do we need to add this at creation?)

INSERT INTO hibernate_unique_key VALUES (70);

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
  name varchar(128) NOT NULL default '',
  description text default '',
  type bigint NOT NULL default '0',
  properties text
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
  version int NOT NULL default '0',
  name varchar(128) NOT NULL default '',
  description text
);


--
-- Dumping data for table 'internal_group'
--


INSERT INTO internal_group VALUES (2,0,'Gateway Administrators','Admin console users having administration rights to the gateway');
--
-- Table structure for table 'internal_user'
--

DROP TABLE internal_user;
CREATE TABLE internal_user (
  objectid bigint NOT NULL primary key default '0',
  version int NOT NULL default '0',
  name varchar(128) default NULL,
  login varchar(32) NOT NULL default '',
  password varchar(32) NOT NULL default '',
  first_name varchar(32) default NULL,
  last_name varchar(32) default NULL,
  email varchar(128) default NULL,
  title varchar(64) default NULL,
  cert text DEFAULT NULL,
  cert_reset_counter INT DEFAULT '0'
);

--
-- Dumping data for table 'internal_user'
--


INSERT INTO internal_user VALUES (3,0,'admin','admin','a41306e4b1b5858d3e3d705dd2e738e2','fname','lname','email','title', NULL, 0);

--
-- Table structure for table 'internal_user_group'
--

DROP TABLE internal_user_group;
CREATE TABLE internal_user_group (
  internal_user bigint NOT NULL default '0',
  internal_group bigint NOT NULL default '0',
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
  objectid bigint NOT NULL primary key default '0',
  version int NOT NULL default '0',
  name varchar(255) NOT NULL default '',
  policy_xml text,
  wsdl_url varchar(255) NOT NULL default '',
  wsdl_xml text,
  disabled boolean NOT NULL default 'f'
);

--
-- Dumping data for table 'published_service'
--

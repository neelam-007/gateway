---
--- Script to update mysql ssg database from 3.5 to 3.6
---
--- Layer 7 Technologies, inc
---

--
-- Disable FK while manipulating tables
--
SET FOREIGN_KEY_CHECKS=0;

--
-- 
--
ALTER TABLE published_service ADD COLUMN http_methods mediumtext;

--
-- Modifications to auditing schema to promote identity to top level
--
-- add new columns
alter table audit_main add column user_name varchar(255);
alter table audit_main add column user_id varchar(255);
alter table audit_main add column provider_oid bigint(20) not null default -1;

-- add old columns in case this is a re-run of the script
alter table audit_message add column user_name    varchar(255);
alter table audit_message add column user_id      varchar(255);
alter table audit_message add column provider_oid bigint(20);
alter table audit_admin   add column admin_login  varchar(255);

-- migrate any existing data (with check for already migrated info)
update audit_main,audit_message set audit_main.user_name=audit_message.user_name, audit_main.user_id=audit_message.user_id, audit_main.provider_oid=audit_message.provider_oid where audit_main.objectid=audit_message.objectid and audit_main.user_name is null and audit_main.user_id is null;
update audit_main,audit_admin   set audit_main.user_name=audit_admin.admin_login, audit_main.user_id=(select objectid from internal_user where login=audit_admin.admin_login) where audit_main.objectid=audit_admin.objectid and audit_main.user_name is null and audit_main.user_id is null;

-- drop old user columns
alter table audit_message drop column user_name;
alter table audit_message drop column user_id;
alter table audit_message drop column provider_oid;
alter table audit_admin drop column admin_login;

--
-- Alter auditing of message to compress request and response message data
--
-- add temp columns for creation of compressed data
alter table audit_message add column request_zipxml mediumblob;
alter table audit_message add column response_zipxml mediumblob;

-- add old columns in case this is a re-run of the script
alter table audit_message add column request_xml  mediumtext;
alter table audit_message add column response_xml mediumtext;

-- migrate data
update audit_message set request_zipxml = COMPRESS(request_xml) where request_xml is not null;
update audit_message set response_zipxml = COMPRESS(response_xml) where response_xml is not null;

-- remove old columns
alter table audit_message drop column request_xml; 
alter table audit_message drop column response_xml;

--alter table service_resolution drop index `soapaction`;
alter table service_resolution modify column soapaction mediumtext character set latin1 BINARY default '';
alter table service_resolution modify column urn mediumtext character set latin1 BINARY default '';
alter table service_resolution modify column uri mediumtext character set latin1 BINARY default '';
--alter table service_resolution add digested varchar(32) default '';
--update service_resolution set digested=HEX(MD5(CONCAT(soapaction,urn,uri)));
--alter table service_resolution modify column digested varchar(32) NOT NULL;
--CREATE UNIQUE INDEX digested ON service_resolution (digested);

------------------------
-- META-GROUP SUPPORT --
------------------------

-- Get rid of old composite PK
alter table internal_user_group drop primary key;

-- Add/rename columns
alter table internal_user_group change internal_user user_id varchar(255) null;
alter table internal_user_group add provider_oid bigint(20) not null;
alter table internal_user_group add subgroup_id varchar(255);
alter table internal_user_group add version int(11) not null;

-- Populate new column to reflect belonging to IIP
update internal_user_group set provider_oid = -2;

-- Add new PK with auto_increment to generate unique values
-- (hopefully won't collide with hibernate's high/low generator)
alter table internal_user_group add objectid bigint(20) auto_increment primary key;

-- Redefine PK to exclude auto_increment
alter table internal_user_group change objectid objectid bigint(20) not null;

-- Add new indexes
alter table internal_user_group add index (provider_oid);
alter table internal_user_group add index (user_id);
alter table internal_user_group add index (subgroup_id);

------------------------------------
-- GLOBAL COUNTERS IN CLUSTER FIX --
------------------------------------
alter table counters modify column userid varchar(128) NOT NULL;

---------------------
-- RBAC DATA MODEL --
---------------------

--
-- Table structure for table rbac_role
--

DROP TABLE IF EXISTS rbac_role;
CREATE TABLE rbac_role (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  name varchar(128) default NULL,
  PRIMARY KEY (objectid),
  UNIQUE KEY name (name)
) TYPE=InnoDB;

--
-- Table structure for table rbac_assignment
--

DROP TABLE IF EXISTS rbac_assignment;
CREATE TABLE rbac_assignment (
  objectid bigint(20) NOT NULL,
  provider_oid bigint(20) NOT NULL,
  role_oid bigint(20) NOT NULL,
  user_id varchar(255) NOT NULL,
  PRIMARY KEY  (objectid),
  UNIQUE KEY unique_assignment (provider_oid,role_oid,user_id),
  FOREIGN KEY (provider_oid) REFERENCES identity_provider (objectid) ON DELETE CASCADE,
  FOREIGN KEY (role_oid) REFERENCES rbac_role (objectid) ON DELETE CASCADE
) TYPE=InnoDB;

--
-- Table structure for table rbac_permission
--

DROP TABLE IF EXISTS rbac_permission;
CREATE TABLE rbac_permission (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  role_oid bigint(20) default NULL,
  operation_type varchar(16) default NULL,
  other_operation varchar(255) default NULL,
  entity_type varchar(255) default NULL,
  PRIMARY KEY (objectid),
  FOREIGN KEY (role_oid) REFERENCES rbac_role (objectid) ON DELETE CASCADE
) TYPE=InnoDB;

--
-- Table structure for table rbac_predicate (none necessary for Administrator)
--

DROP TABLE IF EXISTS rbac_predicate;
CREATE TABLE rbac_predicate (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  permission_oid bigint(20) default NULL,
  PRIMARY KEY (objectid),
  FOREIGN KEY (permission_oid) REFERENCES rbac_permission (objectid) ON DELETE CASCADE
) TYPE=InnoDB;

--
-- Table structure for table rbac_predicate_attribute (none necessary for Administrator)
--

DROP TABLE IF EXISTS rbac_predicate_attribute;
CREATE TABLE rbac_predicate_attribute (
  objectid bigint(20) NOT NULL,
  attribute varchar(255) default NULL,
  value varchar(255) default NULL,
  PRIMARY KEY (objectid),
  FOREIGN KEY (objectid) REFERENCES rbac_predicate (objectid) ON DELETE CASCADE
) TYPE=InnoDB;

--
-- Table structure for table rbac_predicate_oid (none necessary for Administrator)
--

DROP TABLE IF EXISTS rbac_predicate_oid;
CREATE TABLE rbac_predicate_oid (
  objectid bigint(20) NOT NULL,
  entity_oid bigint(20) default NULL,
  PRIMARY KEY (objectid),
  FOREIGN KEY (objectid) REFERENCES rbac_predicate (objectid) ON DELETE CASCADE
) TYPE=InnoDB;

-- Create new Administrator role
INSERT INTO rbac_role VALUES (-3,0,'Administrator');

-- Assign Administrator role to existing admin user
INSERT INTO rbac_assignment VALUES (-4, -2, -3, 3);

-- Grant all CRUD permissions to admin role
INSERT INTO rbac_permission VALUES (-5, 0, -3, 'CREATE', null, 'ANY');
INSERT INTO rbac_permission VALUES (-6, 0, -3, 'READ',   null, 'ANY');
INSERT INTO rbac_permission VALUES (-7, 0, -3, 'UPDATE', null, 'ANY');
INSERT INTO rbac_permission VALUES (-8, 0, -3, 'DELETE', null, 'ANY');

--
-- Allow for longer audit detail parameters
--
alter table audit_detail_params modify column value MEDIUMTEXT NOT NULL;

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;
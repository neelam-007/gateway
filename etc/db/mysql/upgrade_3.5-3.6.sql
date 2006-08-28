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
  entity_id varchar(255) default NULL,
  PRIMARY KEY (objectid),
  FOREIGN KEY (objectid) REFERENCES rbac_predicate (objectid) ON DELETE CASCADE
) TYPE=InnoDB;

-- Create new Administrator role with CRUD on ANY
INSERT INTO rbac_role VALUES (-3,0,'Administrator');
INSERT INTO rbac_permission VALUES (-5, 0, -3, 'CREATE', null, 'ANY');
INSERT INTO rbac_permission VALUES (-6, 0, -3, 'READ',   null, 'ANY');
INSERT INTO rbac_permission VALUES (-7, 0, -3, 'UPDATE', null, 'ANY');
INSERT INTO rbac_permission VALUES (-8, 0, -3, 'DELETE', null, 'ANY');

-- Create Operator role with READ on ANY
INSERT INTO rbac_role VALUES (-9,0,'Operator');
INSERT INTO rbac_permission VALUES (-10, 0, -9, 'READ', null, 'ANY');

-- Assign Administrator role to existing admin users
alter table rbac_assignment drop primary key;
alter table rbac_assignment change objectid objectid bigint(20) not null primary key auto_increment;
insert into rbac_assignment
    (role_oid, provider_oid, user_id) 
    select -3, -2, user_id from internal_user_group where internal_group = 2;

-- Create bogus sentinel value so INSERT ... SELECT doesn't fail
insert into internal_user_group
    (objectid, internal_group, provider_oid, user_id)
    values (-2147483648, 4, -2, -2147483648);

-- Assign Operator role to existing operator users
insert into rbac_assignment
    (role_oid, provider_oid, user_id)
    select -9, -2, user_id from internal_user_group where internal_group = 4;

delete from rbac_assignment where user_id = -2147483648;

alter table rbac_assignment change objectid objectid bigint(20) not null;

--Create Other Predefined roles
INSERT INTO `rbac_role` VALUES (-200,1,'Manage Internal Users and Groups'),(-300,2,'Publish LDAP Identity Providers'),(-400,1,'Search Users and Groups'),(-500,0,'Publish Webservices'),(-600,1,'Manage Webservices'),(-700,0,'View Audit Records and Logs'),(-800,0,'View Service Metrics'),(-900,0,'Manage Cluster Status'),(-1000,0,'Manage Certificates (truststore)'),(-2000,0,'Manage JMS Connections'),(-3000,0,'Manage Cluster Properties');

--Create all the other predefined role permissions
INSERT INTO `rbac_permission` VALUES (163840,1,-500,'READ',NULL,'GROUP'),(163841,1,-500,'READ',NULL,'ID_PROVIDER_CONFIG'),(163842,1,-500,'READ',NULL,'USER'),(163843,1,-600,'READ',NULL,'ID_PROVIDER_CONFIG'),(163844,1,-600,'READ',NULL,'GROUP'),(163845,1,-600,'READ',NULL,'USER'),(1179648,1,-400,'READ',NULL,'USER'),(1179649,1,-400,'READ',NULL,'ID_PROVIDER_CONFIG'),(1179650,1,-400,'READ',NULL,'GROUP'),(1179651,1,-500,'CREATE',NULL,'SERVICE'),(1179652,2,-600,'READ',NULL,'SERVICE'),(1179653,2,-600,'CREATE',NULL,'SERVICE'),(1179654,2,-600,'UPDATE',NULL,'SERVICE'),(1179655,2,-600,'DELETE',NULL,'SERVICE'),(1179656,0,-700,'READ',NULL,'CLUSTER_INFO'),(1179657,0,-700,'READ',NULL,'AUDIT_RECORD'),(1179658,0,-800,'READ',NULL,'METRICS_BIN'),(1179659,0,-800,'READ',NULL,'SERVICE'),(1179660,0,-800,'READ',NULL,'CLUSTER_INFO'),(1179661,0,-900,'READ',NULL,'CLUSTER_INFO'),(1179662,0,-900,'UPDATE',NULL,'CLUSTER_INFO'),(1179663,0,-900,'DELETE',NULL,'CLUSTER_INFO'),(1179664,0,-1000,'UPDATE',NULL,'TRUSTED_CERT'),(1179665,0,-1000,'READ',NULL,'TRUSTED_CERT'),(1179666,0,-1000,'DELETE',NULL,'TRUSTED_CERT'),(1179667,0,-1000,'CREATE',NULL,'TRUSTED_CERT'),(1179668,0,-2000,'CREATE',NULL,'JMS_ENDPOINT'),(1179669,0,-2000,'DELETE',NULL,'JMS_ENDPOINT'),(1179670,0,-2000,'UPDATE',NULL,'JMS_ENDPOINT'),(1179671,0,-2000,'READ',NULL,'JMS_ENDPOINT'),(1179672,0,-3000,'READ',NULL,'CLUSTER_PROPERTY'),(1179673,0,-3000,'CREATE',NULL,'CLUSTER_PROPERTY'),(1179674,0,-3000,'UPDATE',NULL,'CLUSTER_PROPERTY'),(1179675,0,-3000,'DELETE',NULL,'CLUSTER_PROPERTY'),(1441792,1,-300,'CREATE',NULL,'ID_PROVIDER_CONFIG'),(4751360,1,-200,'READ',NULL,'USER'),(4751361,1,-200,'READ',NULL,'ID_PROVIDER_CONFIG'),(4751362,1,-200,'UPDATE',NULL,'USER'),(4751363,1,-200,'READ',NULL,'GROUP'),(4751364,1,-200,'DELETE',NULL,'USER'),(4751365,1,-200,'CREATE',NULL,'USER'),(4751366,1,-200,'CREATE',NULL,'GROUP'),(4751367,1,-200,'DELETE',NULL,'GROUP'),(4751368,1,-200,'UPDATE',NULL,'GROUP');

INSERT INTO `rbac_predicate` VALUES (1474560,0,1441792),(4784128,0,4751360),(4784129,0,4751361),(4784130,0,4751362),(4784131,0,4751363),(4784132,0,4751364),(4784133,0,4751365),(4784134,0,4751366),(4784135,0,4751367),(4784136,0,4751368);

INSERT INTO `rbac_predicate_attribute` VALUES (1474560,'typeVal','2'),(4784128,'providerId','-2'),(4784130,'providerId','-2'),(4784131,'providerId','-2'),(4784132,'providerId','-2'),(4784133,'providerId','-2'),(4784134,'providerId','-2'),(4784135,'providerId','-2'),(4784136,'providerId','-2');

INSERT INTO `rbac_predicate_oid` VALUES (4784129,'-2');

--
-- Allow for longer audit detail parameters
--
alter table audit_detail_params modify column value MEDIUMTEXT NOT NULL;

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;
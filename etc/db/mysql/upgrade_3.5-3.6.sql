--
-- Script to update mysql ssg database from 3.5 to 3.6
--
-- Layer 7 Technologies, inc
--

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

--  bgzla #2923
alter table client_cert add unique key i_identity (provider, user_id);

--
-- META-GROUP SUPPORT
--

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

--
-- GLOBAL COUNTERS IN CLUSTER FIX
--
alter table counters modify column userid varchar(128) NOT NULL;

--
-- RBAC DATA MODEL
--

--
-- Table structure for table rbac_role
--

DROP TABLE IF EXISTS rbac_role;
CREATE TABLE rbac_role (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  name varchar(128) default NULL,
  entity_type varchar(255),
  entity_oid bigint(20),
  description varchar(255),
  PRIMARY KEY (objectid),
  UNIQUE KEY name (name),
  UNIQUE KEY entity_info (entity_type, entity_oid),
  INDEX i_rbacrole_etype (entity_type),
  INDEX i_rbacrole_eoid (entity_oid)
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
  FOREIGN KEY (role_oid) REFERENCES rbac_role (objectid) ON DELETE CASCADE,
  INDEX i_rbacassign_poid (provider_oid),
  INDEX i_rbacassign_uid (user_id)
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

-- Create Administrator role
-- XXX NOTE!! COPIED in Role#ADMIN_ROLE_OID

INSERT INTO rbac_role VALUES (-100,0,'Administrator', null,null, 'Users assigned to the {0} role have full access to the gateway.');
INSERT INTO rbac_permission VALUES (-101, 0, -100, 'CREATE', null, 'ANY');
INSERT INTO rbac_permission VALUES (-102, 0, -100, 'READ',   null, 'ANY');
INSERT INTO rbac_permission VALUES (-103, 0, -100, 'UPDATE', null, 'ANY');
INSERT INTO rbac_permission VALUES (-104, 0, -100, 'DELETE', null, 'ANY');

-- Create Operator role
INSERT INTO rbac_role VALUES (-150,0,'Operator', null,null, 'Users assigned to the {0} role have read only access to the gateway.');
INSERT INTO rbac_permission VALUES (-151, 0, -150, 'READ', null, 'ANY');

-- Create other canned roles
INSERT INTO rbac_role VALUES (-200,0,'Manage Internal Users and Groups', null,null, 'Users assigned to the {0} role have the ability to create, read, update and delete users and groups in the internal identity provider.');
INSERT INTO rbac_permission VALUES (-201,0,-200,'READ',NULL,'USER');
INSERT INTO rbac_predicate VALUES (-202,0,-201);
INSERT INTO rbac_predicate_attribute VALUES (-202,'providerId','-2');
INSERT INTO rbac_permission VALUES (-203,0,-200,'READ',NULL,'ID_PROVIDER_CONFIG');
INSERT INTO rbac_predicate VALUES (-204,0,-203);
INSERT INTO rbac_predicate_oid VALUES (-204,'-2');
INSERT INTO rbac_permission VALUES (-205,0,-200,'UPDATE',NULL,'USER');
INSERT INTO rbac_predicate VALUES (-206,0,-205);
INSERT INTO rbac_predicate_attribute VALUES (-206,'providerId','-2');
INSERT INTO rbac_permission VALUES (-207,0,-200,'READ',NULL,'GROUP');
INSERT INTO rbac_predicate VALUES (-208,0,-207);
INSERT INTO rbac_predicate_attribute VALUES (-208,'providerId','-2');
INSERT INTO rbac_permission VALUES (-209,0,-200,'DELETE',NULL,'USER');
INSERT INTO rbac_predicate VALUES (-210,0,-209);
INSERT INTO rbac_predicate_attribute VALUES (-210,'providerId','-2');
INSERT INTO rbac_permission VALUES (-211,0,-200,'CREATE',NULL,'USER');
INSERT INTO rbac_predicate VALUES (-212,0,-211);
INSERT INTO rbac_predicate_attribute VALUES (-212,'providerId','-2');
INSERT INTO rbac_permission VALUES (-213,0,-200,'CREATE',NULL,'GROUP');
INSERT INTO rbac_predicate VALUES (-214,0,-213);
INSERT INTO rbac_predicate_attribute VALUES (-214,'providerId','-2');
INSERT INTO rbac_permission VALUES (-215,0,-200,'DELETE',NULL,'GROUP');
INSERT INTO rbac_predicate VALUES (-216,0,-215);
INSERT INTO rbac_predicate_attribute VALUES (-216,'providerId','-2');
INSERT INTO rbac_permission VALUES (-217,0,-200,'UPDATE',NULL,'GROUP');
INSERT INTO rbac_predicate VALUES (-218,0,-217);
INSERT INTO rbac_predicate_attribute VALUES (-218,'providerId','-2');

INSERT INTO rbac_role VALUES (-250,0,'Publish External Identity Providers', null,null, 'Users assigned to the {0} role have the ability to create new external identity providers.');
INSERT INTO rbac_permission VALUES (-251,0,-250,'CREATE',NULL,'ID_PROVIDER_CONFIG');
INSERT INTO rbac_predicate VALUES (-252,0,-251);
INSERT INTO rbac_predicate_attribute VALUES (-252,'typeVal','2');
INSERT INTO rbac_permission VALUES (-253,0,-250,'CREATE',NULL,'ID_PROVIDER_CONFIG');
INSERT INTO rbac_predicate VALUES (-254,0,-253);
INSERT INTO rbac_predicate_attribute VALUES (-254,'typeVal','3');
INSERT INTO rbac_permission VALUES (-255,0,-250,'READ',NULL,'TRUSTED_CERT');

INSERT INTO rbac_role VALUES (-300,0,'Search Users and Groups', null,null, 'Users assigned to the {0} role have permission to search and view users and groups in all identity providers.');
INSERT INTO rbac_permission VALUES (-301,0,-300,'READ',NULL,'USER');
INSERT INTO rbac_permission VALUES (-302,0,-300,'READ',NULL,'ID_PROVIDER_CONFIG');
INSERT INTO rbac_permission VALUES (-303,0,-300,'READ',NULL,'GROUP');

INSERT INTO rbac_role VALUES (-350,0,'Publish Webservices', null,null, 'Users assigned to the {0} role have the ability to publish new web services.');
INSERT INTO rbac_permission VALUES (-351,0,-350,'READ',NULL,'GROUP');
INSERT INTO rbac_permission VALUES (-352,0,-350,'READ',NULL,'ID_PROVIDER_CONFIG');
INSERT INTO rbac_permission VALUES (-353,0,-350,'READ',NULL,'USER');
INSERT INTO rbac_permission VALUES (-354,0,-350,'CREATE',NULL,'SERVICE');

INSERT INTO rbac_role VALUES (-400,1,'Manage Webservices', null,null, 'Users assigned to the {0} role have the ability to publish new services and edit existing ones.');
INSERT INTO rbac_permission VALUES (-401,0,-400,'READ',NULL,'ID_PROVIDER_CONFIG');
INSERT INTO rbac_permission VALUES (-402,0,-400,'READ',NULL,'GROUP');
INSERT INTO rbac_permission VALUES (-403,0,-400,'READ',NULL,'USER');
INSERT INTO rbac_permission VALUES (-404,0,-400,'READ',NULL,'SERVICE');
INSERT INTO rbac_permission VALUES (-405,0,-400,'CREATE',NULL,'SERVICE');
INSERT INTO rbac_permission VALUES (-406,0,-400,'UPDATE',NULL,'SERVICE');
INSERT INTO rbac_permission VALUES (-407,0,-400,'DELETE',NULL,'SERVICE');
INSERT INTO rbac_permission VALUES (-408,0,-400,'READ',NULL,'CLUSTER_INFO');
INSERT INTO rbac_permission VALUES (-409,0,-400,'READ',NULL,'SERVICE_USAGE');
INSERT INTO rbac_permission VALUES (-410,0,-400,'READ',NULL,'METRICS_BIN');
INSERT INTO rbac_permission VALUES (-411,0,-400,'READ',NULL,'AUDIT_MESSAGE');
INSERT INTO rbac_permission VALUES (-412,0,-400,'READ',NULL,'JMS_CONNECTION');
INSERT INTO rbac_permission VALUES (-413,0,-400,'READ',NULL,'JMS_ENDPOINT');

INSERT INTO rbac_role VALUES (-450,0,'View Audit Records and Logs', null,null, 'Users assigned to the {0} role have the ability to view audit and log details in manager.');
INSERT INTO rbac_permission VALUES (-451,0,-450,'READ',NULL,'CLUSTER_INFO');
INSERT INTO rbac_permission VALUES (-452,0,-450,'READ',NULL,'AUDIT_RECORD');

INSERT INTO rbac_role VALUES (-500,0,'View Service Metrics', null,null, 'Users assigned to the {0} role have the ability to monitor service metrics in the manager.');
INSERT INTO rbac_permission VALUES (-501,0,-500,'READ',NULL,'METRICS_BIN');
INSERT INTO rbac_permission VALUES (-502,0,-500,'READ',NULL,'SERVICE');
INSERT INTO rbac_permission VALUES (-503,0,-500,'READ',NULL,'CLUSTER_INFO');
INSERT INTO rbac_permission VALUES (-504,0,-500,'READ',NULL,'SERVICE_USAGE');

INSERT INTO rbac_role VALUES (-550,0,'Manage Cluster Status', null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete cluster status information.');
INSERT INTO rbac_permission VALUES (-551,0,-550,'READ',NULL,'CLUSTER_INFO');
INSERT INTO rbac_permission VALUES (-552,0,-550,'UPDATE',NULL,'CLUSTER_INFO');
INSERT INTO rbac_permission VALUES (-553,0,-550,'DELETE',NULL,'CLUSTER_INFO');

INSERT INTO rbac_role VALUES (-600,0,'Manage Certificates (truststore)', null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete trusted certificates.');
INSERT INTO rbac_permission VALUES (-601,0,-600,'UPDATE',NULL,'TRUSTED_CERT');
INSERT INTO rbac_permission VALUES (-602,0,-600,'READ',NULL,'TRUSTED_CERT');
INSERT INTO rbac_permission VALUES (-603,0,-600,'DELETE',NULL,'TRUSTED_CERT');
INSERT INTO rbac_permission VALUES (-604,0,-600,'CREATE',NULL,'TRUSTED_CERT');

INSERT INTO rbac_role VALUES (-650,0,'Manage JMS Connections', null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete JMS connections.');
INSERT INTO rbac_permission VALUES (-651,1,-650,'READ',NULL,'JMS_CONNECTION');
INSERT INTO rbac_permission VALUES (-652,1,-650,'DELETE',NULL,'JMS_CONNECTION');
INSERT INTO rbac_permission VALUES (-653,1,-650,'CREATE',NULL,'JMS_CONNECTION');
INSERT INTO rbac_permission VALUES (-654,1,-650,'UPDATE',NULL,'JMS_CONNECTION');
INSERT INTO rbac_permission VALUES (-655,1,-650,'CREATE',NULL,'JMS_ENDPOINT');
INSERT INTO rbac_permission VALUES (-656,1,-650,'DELETE',NULL,'JMS_ENDPOINT');
INSERT INTO rbac_permission VALUES (-657,1,-650,'UPDATE',NULL,'JMS_ENDPOINT');
INSERT INTO rbac_permission VALUES (-658,1,-650,'READ',NULL,'JMS_ENDPOINT');

INSERT INTO rbac_role VALUES (-700,0,'Manage Cluster Properties', null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete cluster properties.');
INSERT INTO rbac_permission VALUES (-701,0,-700,'READ',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_permission VALUES (-702,0,-700,'CREATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_permission VALUES (-703,0,-700,'UPDATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_permission VALUES (-704,0,-700,'DELETE',NULL,'CLUSTER_PROPERTY');

-- Assign Administrator role to existing admin users
alter table rbac_assignment drop primary key;
alter table rbac_assignment change objectid objectid bigint(20) not null primary key auto_increment;
insert into rbac_assignment
    (role_oid, provider_oid, user_id) 
    select -100, -2, user_id from internal_user_group where internal_group = 2;

-- Create bogus sentinel value so INSERT ... SELECT doesn't fail
insert into internal_user_group
    (objectid, internal_group, provider_oid, user_id)
    values (-2147483648, 4, -2, -2147483648);

-- Assign Operator role to existing operator users
insert into rbac_assignment
    (role_oid, provider_oid, user_id)
    select -150, -2, user_id from internal_user_group where internal_group = 4;

-- Delete sentinel value
delete from rbac_assignment where user_id = -2147483648;

-- Get rid of temporary auto_increment
alter table rbac_assignment change objectid objectid bigint(20) not null;

--
-- Allow for longer audit detail parameters
--
alter table audit_detail_params modify column value MEDIUMTEXT NOT NULL;

--
-- Flag an upgrade task to create any needed roles on next SSG reboot
--
insert into cluster_properties
    (objectid, version, propkey, propvalue)
    values (-300600, 0, "upgrade.task.300600", "com.l7tech.server.upgrade.Upgrade35To36AddRoles");

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

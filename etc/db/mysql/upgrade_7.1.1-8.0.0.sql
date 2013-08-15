--
-- Script to update mysql ssg database from 7.1.0 to 8.0.0
--
-- Layer 7 Technologies, inc
--

--
-- Disable FK while manipulating tables
--
SET FOREIGN_KEY_CHECKS=0;

--
-- Update the version
--
UPDATE ssg_version SET current_version = '8.0.0';

-- ****************************************************************************************************************** --
-- ********************************* OID'S NO LONGER EXIST AFTER THIS POINT ***************************************** --
-- ****************************************************************************************************************** --

-- Create the toGoid function. This makes it easier to create goid's from a high and low number. This returns the goid
-- as a binary(16)
DROP FUNCTION IF EXISTS toGoid;
delimiter //
CREATE FUNCTION toGoid (prefix bigint, suffix bigint)
RETURNS binary(16) DETERMINISTIC
begin
    if suffix is null then RETURN null;
	else RETURN concat(lpad(char(prefix >> 32, prefix),8,'\0'),lpad(char(suffix >> 32, suffix),8,'\0'));
	end if;
end//
delimiter ;

DROP FUNCTION IF EXISTS goidToString;
CREATE FUNCTION goidToString(goid binary(16)) RETURNS CHAR(32) DETERMINISTIC
RETURN lower(hex(goid));

-- The dropForeignKey function will drop a foreign key constraint on a table where the constraint does not have a name.
-- The first parameter is the table name that has the constraint. The second parameter is the table that the constraint
-- references. If there are 2 or more different foreign key references to the same table all will be dropped. This only
-- works for foreign keys that are on primary keys of the foreign table. A
DROP PROCEDURE IF EXISTS dropForeignKey;
delimiter //
create procedure dropForeignKey(in tableName varchar(255), in referenceTableName varchar(255))
begin
    set @constraintName = 'PRIMARY';
	set @ssgSchema = SCHEMA();
	SELECT count(*) into @constraint_count FROM information_schema.REFERENTIAL_CONSTRAINTS
	WHERE constraint_schema = @ssgSchema AND table_name = tableName and referenced_table_name=referenceTableName and unique_constraint_name=@constraintName;

	while @constraint_count > 0 do
        SELECT constraint_name into @constraint_name FROM information_schema.REFERENTIAL_CONSTRAINTS
        WHERE constraint_schema = @ssgSchema AND table_name = tableName and referenced_table_name=referenceTableName LIMIT 1;
        SET @s = CONCAT('ALTER TABLE ', tableName, ' DROP FOREIGN KEY ', @constraint_name);
        PREPARE stmt FROM @s;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

		SELECT count(*) into @constraint_count FROM information_schema.REFERENTIAL_CONSTRAINTS
	    WHERE constraint_schema = @ssgSchema AND table_name = tableName and referenced_table_name=referenceTableName and unique_constraint_name=@constraintName;
	end while;
end//
delimiter ;

-- ****** The below function is only needed in manual upgrades.                      ****** --
-- ****** It will create a not very random goid prefix that is not a reserved prefix ****** --
-- DROP FUNCTION IF EXISTS createUnreservedPoorRandomPrefix;
-- CREATE FUNCTION createUnreservedPoorRandomPrefix()
-- RETURNS bigint DETERMINISTIC
-- return ((floor(rand()*2147483647)+1) << 32) | floor(rand()*2147483648);

--
-- Security Zones
--
CREATE TABLE security_zone (
  goid BINARY(16) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  description varchar(255) NOT NULL,
  entity_types varchar(4096) NOT NULL,
  PRIMARY KEY (goid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

CREATE TABLE rbac_predicate_security_zone (
  objectid bigint(20) NOT NULL,
  security_zone_goid BINARY(16),
  PRIMARY KEY (objectid),
  FOREIGN KEY (objectid) REFERENCES rbac_predicate (objectid) ON DELETE CASCADE,
  FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

CREATE TABLE assertion_access (
  goid binary(16) NOT NULL,
  version int(11) NOT NULL,
  name varchar(255) NOT NULL,
  security_zone_goid BINARY(16),
  PRIMARY KEY (goid),
  UNIQUE KEY i_name (name),
  CONSTRAINT assertion_access_security_zone FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

CREATE TABLE siteminder_configuration (
  goid binary(16) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  agent_name varchar(256) NOT NULL,
  address varchar(128) NOT NULL,
  secret varchar(4096) NOT NULL,
  ipcheck boolean DEFAULT false NOT NULL,
  update_sso_token boolean DEFAULT false NOT NULL,
  enabled tinyint(1) NOT NULL DEFAULT 1,
  hostname varchar(255) NOT NULL,
  fipsmode integer NOT NULL DEFAULT 0,
  host_configuration varchar(256) DEFAULT NULL,
  user_name varchar(256) DEFAULT NULL,
  password_oid bigint(20) DEFAULT NULL, 
  noncluster_failover boolean DEFAULT false NOT NULL,
  cluster_threshold integer DEFAULT 50,
  security_zone_goid binary(16),
  FOREIGN KEY (password_oid) REFERENCES secure_password (objectid),
  PRIMARY KEY (goid),
  CONSTRAINT siteminder_configuration_security_zone FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL,
  INDEX i_name (name),
  UNIQUE(name)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

CREATE TABLE siteminder_configuration_property (
  goid binary(16) NOT NULL,
  name varchar(128) NOT NULL,
  value MEDIUMTEXT NOT NULL,
  FOREIGN KEY (goid) REFERENCES siteminder_configuration (goid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;


alter table rbac_role add column entity_goid BINARY(16) after entity_oid;
CREATE INDEX i_rbacrole_egoid ON rbac_role (entity_goid);

-- create new RBAC role for SiteMinder Configuration --
INSERT INTO rbac_role (objectid, version, name, tag, entity_type, entity_oid, description, user_created) VALUES (-1500,0,'Manage SiteMinder Configuration', null, 'SITEMINDER_CONFIGURATION', null,'Users assigned to the {0} role have the ability to read, create, update and delete SiteMinder configuration.',0);
INSERT INTO rbac_permission (objectid, version, role_oid, operation_type, other_operation, entity_type) VALUES (-1501,0,-1500,'READ',NULL,'SITEMINDER_CONFIGURATION');
INSERT INTO rbac_permission (objectid, version, role_oid, operation_type, other_operation, entity_type) VALUES (-1502,0,-1500,'CREATE',NULL,'SITEMINDER_CONFIGURATION');
INSERT INTO rbac_permission (objectid, version, role_oid, operation_type, other_operation, entity_type) VALUES (-1503,0,-1500,'UPDATE',NULL,'SITEMINDER_CONFIGURATION');
INSERT INTO rbac_permission (objectid, version, role_oid, operation_type, other_operation, entity_type) VALUES (-1504,0,-1500,'DELETE',NULL,'SITEMINDER_CONFIGURATION');
INSERT INTO rbac_permission (objectid, version, role_oid, operation_type, other_operation, entity_type) VALUES (-1505,0,-1500,'READ',NULL,'SECURE_PASSWORD');

alter table policy add column security_zone_goid BINARY(16);
alter table policy add FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL;

alter table policy_alias add column security_zone_goid BINARY(16);
alter table policy_alias add FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL;

alter table published_service add column security_zone_goid BINARY(16);
alter table published_service add FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL;

alter table published_service_alias add column security_zone_goid BINARY(16);
alter table published_service_alias add FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL;

alter table folder add column security_zone_goid BINARY(16);
alter table folder add FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL;

alter table identity_provider add column security_zone_goid BINARY(16);
alter table identity_provider add FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL;

alter table jdbc_connection add column security_zone_goid BINARY(16);
alter table jdbc_connection add FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL;

alter table trusted_cert add column security_zone_goid BINARY(16);
alter table trusted_cert add FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL;

alter table sink_config add column security_zone_goid BINARY(16);
alter table sink_config add FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL;

alter table secure_password add column security_zone_goid BINARY(16);
alter table secure_password add FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL;

alter table email_listener add column security_zone_goid BINARY(16);
alter table email_listener add FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL;

alter table resource_entry add column security_zone_goid BINARY(16);
alter table resource_entry add FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL;

alter table http_configuration add column security_zone_goid BINARY(16);
alter table http_configuration add FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL;

alter table connector add column security_zone_goid BINARY(16);
alter table connector add FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL;

alter table encapsulated_assertion add column security_zone_goid BINARY(16);
alter table encapsulated_assertion add FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL;

alter table active_connector add column security_zone_goid BINARY(16);
alter table active_connector add FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL;

alter table revocation_check_policy add column security_zone_goid BINARY(16);
alter table revocation_check_policy add FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL;

alter table uddi_registries add column security_zone_goid BINARY(16);
alter table uddi_registries add FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL;
alter table uddi_proxied_service_info add column security_zone_goid BINARY(16);
alter table uddi_proxied_service_info add FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL;
alter table uddi_service_control add column security_zone_goid BINARY(16);
alter table uddi_service_control add FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL;

alter table sample_messages add column security_zone_goid BINARY(16);
alter table sample_messages add FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL;

alter table jms_endpoint add column security_zone_goid BINARY(16);
alter table jms_endpoint add FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL;
alter table jms_connection add column security_zone_goid BINARY(16);
alter table jms_connection add FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL;
--
-- RBAC for Assertions: Update "Publish Webservices" and "Manage Webservices" canned roles so they can still use policy assertions in 8.0
--
INSERT INTO rbac_permission (objectid, version, role_oid, operation_type, other_operation, entity_type) VALUES (-360,0,-350,'READ',NULL,'ASSERTION_ACCESS');
INSERT INTO rbac_permission (objectid, version, role_oid, operation_type, other_operation, entity_type) VALUES (-361,0,-350,'CREATE',NULL,'ASSERTION_ACCESS');
INSERT INTO rbac_permission (objectid, version, role_oid, operation_type, other_operation, entity_type) VALUES (-442,0,-400,'CREATE',NULL,'ASSERTION_ACCESS');
INSERT INTO rbac_permission (objectid, version, role_oid, operation_type, other_operation, entity_type) VALUES (-443,0,-400,'READ',NULL,'ASSERTION_ACCESS');

-- Increasing the length of the issuer dn to match the length of the subject dn
-- See SSG-6848, SSG-6849, SSG-6850
ALTER TABLE client_cert MODIFY COLUMN issuer_dn VARCHAR(2048);
ALTER TABLE trusted_cert MODIFY COLUMN issuer_dn VARCHAR(2048);
-- updating the client_cert index to match the index in ssg.sql
-- Note the trusted_cert table index doesn't need to be as it was already created with issuer_dn(255)
-- setting the length to 255 will use the first 255 characters of the issuer_dn to create the index.
ALTER TABLE client_cert DROP INDEX i_issuer_dn;
CREATE INDEX i_issuer_dn ON client_cert (issuer_dn(255));

--
-- Keystore private key metadata (security zones)
--
CREATE TABLE keystore_key_metadata (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL default 0,
  keystore_file_oid bigint(20) NOT NULL,
  alias varchar(255) NOT NULL,
  security_zone_goid BINARY(16),
  PRIMARY KEY (objectid),
  UNIQUE KEY i_ks_alias (keystore_file_oid, alias),
  CONSTRAINT keystore_key_metadata_keystore_file FOREIGN KEY (keystore_file_oid) REFERENCES keystore_file (objectid) ON DELETE CASCADE,
  CONSTRAINT keystore_key_metadata_security_zone FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Register upgrade task for adding Assertion Access to auto-created "Manage <Blah>" roles
--
INSERT INTO cluster_properties
    (objectid, version, propkey, propvalue, properties)
    values (-800000, 0, 'upgrade.task.800000', 'com.l7tech.server.upgrade.Upgrade71To80UpdateRoles', null);

--
-- Custom key value store
--
CREATE TABLE custom_key_value_store (
  goid binary(16) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  value mediumblob NOT NULL,
  PRIMARY KEY (goid),
  UNIQUE KEY (name)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

INSERT INTO rbac_role (objectid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (-1450,0,'Manage Custom Key Value Store', null,'CUSTOM_KEY_VALUE_STORE',null,null, 'Users assigned to the {0} role have the ability to read, create, update, and delete key values from custom key value store.',0);
INSERT INTO rbac_permission (objectid, version, role_oid, operation_type, other_operation, entity_type) VALUES (-1451,0,-1450,'CREATE',null,'CUSTOM_KEY_VALUE_STORE');
INSERT INTO rbac_permission (objectid, version, role_oid, operation_type, other_operation, entity_type) VALUES (-1452,0,-1450,'READ',null,'CUSTOM_KEY_VALUE_STORE');
INSERT INTO rbac_permission (objectid, version, role_oid, operation_type, other_operation, entity_type) VALUES (-1453,0,-1450,'UPDATE',null,'CUSTOM_KEY_VALUE_STORE');
INSERT INTO rbac_permission (objectid, version, role_oid, operation_type, other_operation, entity_type) VALUES (-1454,0,-1450,'DELETE',null,'CUSTOM_KEY_VALUE_STORE');

--
-- Goidification modification. These involve replacing the oid column with a goid column on entity tables.
--

-- JdbcConnection
ALTER TABLE jdbc_connection ADD COLUMN objectid_backup BIGINT(20);
update jdbc_connection set objectid_backup=objectid;
ALTER TABLE jdbc_connection CHANGE COLUMN objectid goid BINARY(16) NOT NULL;
-- For manual runs use: set @jdbc_prefix=createUnreservedPoorRandomPrefix();
set @jdbc_prefix=#RANDOM_LONG_NOT_RESERVED#;
update jdbc_connection set goid = toGoid(@jdbc_prefix,objectid_backup);
ALTER TABLE jdbc_connection DROP COLUMN objectid_backup;
update rbac_role set entity_goid = toGoid(@jdbc_prefix,entity_oid) where entity_oid is not null and entity_type='JDBC_CONNECTION';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = goidToString(toGoid(@jdbc_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'JDBC_CONNECTION';

-- MetricsBin, MetricsBinDetail
call dropForeignKey('service_metrics_details','service_metrics');

ALTER TABLE service_metrics ADD COLUMN objectid_backup BIGINT(20);
UPDATE service_metrics SET objectid_backup=objectid;
ALTER TABLE service_metrics CHANGE COLUMN objectid goid BINARY(16) NOT NULL;
-- For manual runs use: set @metrics_prefix=createUnreservedPoorRandomPrefix();
SET @metrics_prefix=#RANDOM_LONG_NOT_RESERVED#;
UPDATE service_metrics SET goid = toGoid(@metrics_prefix,objectid_backup);
ALTER TABLE service_metrics DROP COLUMN objectid_backup;

ALTER TABLE service_metrics_details ADD COLUMN service_metrics_oid_backup BIGINT(20);
UPDATE service_metrics_details SET service_metrics_oid_backup=service_metrics_oid;
ALTER TABLE service_metrics_details CHANGE COLUMN service_metrics_oid service_metrics_goid BINARY(16) NOT NULL;
UPDATE service_metrics_details SET service_metrics_goid = toGoid(@metrics_prefix,service_metrics_oid_backup);
ALTER TABLE service_metrics_details DROP COLUMN service_metrics_oid_backup;

ALTER TABLE service_metrics_details  ADD FOREIGN KEY (service_metrics_goid) REFERENCES service_metrics (goid) ON DELETE CASCADE;

update rbac_role set entity_goid = toGoid(@metrics_prefix,entity_oid) where entity_oid is not null and entity_type='METRICS_BIN';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = goidToString(toGoid(@metrics_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'METRICS_BIN';

-- Logon info
ALTER TABLE logon_info ADD COLUMN objectid_backup BIGINT(20);
update logon_info set objectid_backup=objectid;
ALTER TABLE logon_info CHANGE COLUMN objectid goid BINARY(16) NOT NULL;
-- For manual runs use: set @logonInfo_prefix=createUnreservedPoorRandomPrefix();
SET @logonInfo_prefix=#RANDOM_LONG_NOT_RESERVED#;
UPDATE logon_info SET goid = toGoid(@logonInfo_prefix,objectid_backup);
ALTER TABLE logon_info DROP COLUMN objectid_backup;

-- SampleMessage
ALTER TABLE sample_messages ADD COLUMN objectid_backup BIGINT(20);
update sample_messages set objectid_backup=objectid;
ALTER TABLE sample_messages CHANGE COLUMN objectid goid BINARY(16) NOT NULL;
-- For manual runs use: set @sample_messages_prefix=createUnreservedPoorRandomPrefix();
set @sample_messages_prefix=#RANDOM_LONG_NOT_RESERVED#;
update sample_messages set goid = toGoid(@sample_messages_prefix,objectid_backup);
ALTER TABLE sample_messages DROP COLUMN objectid_backup;

update rbac_role set entity_goid = toGoid(@sample_messages_prefix,entity_oid) where entity_oid is not null and entity_type='SAMPLE_MESSAGE';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = goidToString(toGoid(@sample_messages_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'SAMPLE_MESSAGE';

-- ClusterProperty
ALTER TABLE cluster_properties ADD COLUMN objectid_backup BIGINT(20);
update cluster_properties set objectid_backup=objectid;
ALTER TABLE cluster_properties CHANGE COLUMN objectid goid BINARY(16) NOT NULL;
-- For manual runs use: set @cluster_properties_prefix=createUnreservedPoorRandomPrefix();
set @cluster_properties_prefix=#RANDOM_LONG_NOT_RESERVED#;
update cluster_properties set goid = toGoid(@cluster_properties_prefix,objectid_backup);
update cluster_properties set goid = toGoid(0,objectid_backup) where propkey = 'cluster.hostname';
update cluster_properties set goid = toGoid(0,objectid_backup) where propkey like 'upgrade.task.%';
ALTER TABLE cluster_properties DROP COLUMN objectid_backup;
update rbac_role set entity_goid = toGoid(@cluster_properties_prefix,entity_oid) where entity_oid is not null and entity_type='CLUSTER_PROPERTY';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = goidToString(toGoid(@cluster_properties_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'CLUSTER_PROPERTY';

-- SecurePassword
call dropForeignKey('http_configuration','secure_password');
call dropForeignKey('siteminder_configuration','secure_password');

ALTER TABLE secure_password ADD COLUMN objectid_backup BIGINT(20);
update secure_password set objectid_backup=objectid;
ALTER TABLE secure_password CHANGE COLUMN objectid goid BINARY(16) NOT NULL;
-- For manual runs use: set @secure_password_prefix=createUnreservedPoorRandomPrefix();
set @secure_password_prefix=#RANDOM_LONG_NOT_RESERVED#;
update secure_password set goid = toGoid(@secure_password_prefix,objectid_backup);
ALTER TABLE secure_password DROP COLUMN objectid_backup;

update rbac_role set entity_goid = toGoid(@secure_password_prefix,entity_oid) where entity_oid is not null and entity_type='SECURE_PASSWORD';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = goidToString(toGoid(@secure_password_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'SECURE_PASSWORD';

ALTER TABLE http_configuration ADD COLUMN password_oid_backup BIGINT(20);
UPDATE http_configuration SET password_oid_backup=password_oid;
ALTER TABLE http_configuration CHANGE COLUMN password_oid password_goid BINARY(16) DEFAULT NULL;
UPDATE http_configuration SET password_goid = toGoid(@secure_password_prefix,password_oid_backup);
ALTER TABLE http_configuration DROP COLUMN password_oid_backup;

ALTER TABLE http_configuration ADD COLUMN proxy_password_oid_backup BIGINT(20);
UPDATE http_configuration SET proxy_password_oid_backup=proxy_password_oid;
ALTER TABLE http_configuration CHANGE COLUMN proxy_password_oid proxy_password_goid BINARY(16) DEFAULT NULL;
UPDATE http_configuration SET proxy_password_goid = toGoid(@secure_password_prefix,proxy_password_oid_backup);
ALTER TABLE http_configuration DROP COLUMN proxy_password_oid_backup;

ALTER TABLE http_configuration ADD FOREIGN KEY (password_goid) REFERENCES secure_password (goid);
ALTER TABLE http_configuration ADD FOREIGN KEY (proxy_password_goid) REFERENCES secure_password (goid);

ALTER TABLE siteminder_configuration ADD COLUMN password_oid_backup BIGINT(20);
UPDATE siteminder_configuration SET password_oid_backup=password_oid;
ALTER TABLE siteminder_configuration CHANGE COLUMN password_oid password_goid BINARY(16) DEFAULT NULL;
UPDATE siteminder_configuration SET password_goid = toGoid(@secure_password_prefix,password_oid_backup);
ALTER TABLE siteminder_configuration DROP COLUMN password_oid_backup;

ALTER TABLE siteminder_configuration ADD FOREIGN KEY (password_goid) REFERENCES secure_password (goid);

-- Resource Entry
ALTER TABLE resource_entry ADD COLUMN objectid_backup BIGINT(20);
update resource_entry set objectid_backup=objectid;
ALTER TABLE resource_entry CHANGE COLUMN objectid goid BINARY(16) NOT NULL;
-- For manual runs use: set @resource_entry_prefix=createUnreservedPoorRandomPrefix();
set @resource_entry_prefix=#RANDOM_LONG_NOT_RESERVED#;
update resource_entry set goid = toGoid(@resource_entry_prefix,objectid_backup);
update resource_entry set goid = toGoid(0,objectid_backup) where objectid_backup in (-3,-4,-5,-6,-7);
ALTER TABLE resource_entry DROP COLUMN objectid_backup;

update rbac_role set entity_goid = toGoid(@resource_entry_prefix,entity_oid) where entity_oid is not null and entity_type='RESOURCE_ENTRY' and entity_oid not in (-3,-4,-5,-6,-7);
update rbac_role set entity_goid = toGoid(0,entity_oid) where entity_oid is not null and entity_type='RESOURCE_ENTRY' and entity_oid in (-3,-4,-5,-6,-7);
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = goidToString(toGoid(@resource_entry_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'RESOURCE_ENTRY' and entity_id not in ('-3','-4','-5','-6','-7');
update rbac_predicate_oid oid1 set oid1.entity_id = goidToString(toGoid(0,oid1.entity_id)) where entity_id in ('-3','-4','-5','-6','-7');

-- EmailListener
ALTER TABLE email_listener ADD COLUMN objectid_backup BIGINT(20);
UPDATE email_listener SET objectid_backup=objectid;
ALTER TABLE email_listener CHANGE COLUMN objectid goid BINARY(16) NOT NULL;
-- For manual runs use: set @email_prefix=createUnreservedPoorRandomPrefix();
SET @email_prefix=#RANDOM_LONG_NOT_RESERVED#;
UPDATE email_listener SET goid = toGoid(@email_prefix,objectid_backup);
ALTER TABLE email_listener DROP COLUMN objectid_backup;

ALTER TABLE email_listener_state ADD COLUMN email_listener_id_backup BIGINT(20);
UPDATE email_listener_state SET email_listener_id_backup=email_listener_id;
ALTER TABLE email_listener_state CHANGE COLUMN email_listener_id email_listener_goid BINARY(16) NOT NULL;
UPDATE email_listener_state SET email_listener_goid = toGoid(@email_prefix,email_listener_id_backup);
ALTER TABLE email_listener_state DROP COLUMN email_listener_id_backup;

ALTER TABLE email_listener_state ADD COLUMN objectid_backup BIGINT(20);
UPDATE email_listener_state SET objectid_backup=objectid;
ALTER TABLE email_listener_state CHANGE COLUMN objectid goid BINARY(16) NOT NULL;
-- For manual runs use: set @emailState_prefix=createUnreservedPoorRandomPrefix();
SET @emailState_prefix=#RANDOM_LONG_NOT_RESERVED#;
UPDATE email_listener_state SET goid = toGoid(@emailState_prefix,objectid_backup);
ALTER TABLE email_listener_state DROP COLUMN objectid_backup;
                
update rbac_role set entity_goid = toGoid(@email_prefix,entity_oid) where entity_oid is not null and entity_type='EMAIL_LISTENER';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = goidToString(toGoid(@email_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'EMAIL_LISTENER';

-- GenericEntity
ALTER TABLE generic_entity ADD COLUMN objectid_backup BIGINT(20);
update generic_entity set objectid_backup=objectid;
ALTER TABLE generic_entity CHANGE COLUMN objectid goid BINARY(16) NOT NULL;
-- For manual runs use: set @generic_entity_prefix=createUnreservedPoorRandomPrefix();
set @generic_entity_prefix=#RANDOM_LONG_NOT_RESERVED#;
update generic_entity set goid = toGoid(@generic_entity_prefix,objectid_backup);
ALTER TABLE generic_entity DROP COLUMN objectid_backup;
update rbac_role set entity_goid = toGoid(@generic_entity_prefix,entity_oid) where entity_oid is not null and entity_type='GENERIC';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = goidToString(toGoid(@generic_entity_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'GENERIC';


-- Connector
call dropForeignKey('connector_property','connector');

ALTER TABLE connector ADD COLUMN objectid_backup BIGINT(20);
UPDATE connector SET objectid_backup=objectid;
ALTER TABLE connector CHANGE COLUMN objectid goid binary(16) NOT NULL;
-- For manual runs use: set @connector_prefix=createUnreservedPoorRandomPrefix();
SET @connector_prefix=#RANDOM_LONG_NOT_RESERVED#;
UPDATE connector SET goid = toGoid(@connector_prefix,objectid_backup);
ALTER TABLE connector DROP COLUMN objectid_backup;

ALTER TABLE connector_property ADD COLUMN connector_oid_backup BIGINT(20);
UPDATE connector_property SET connector_oid_backup=connector_oid;
ALTER TABLE connector_property CHANGE COLUMN connector_oid connector_goid binary(16) NOT NULL;
UPDATE connector_property SET connector_goid = toGoid(@connector_prefix,connector_oid_backup);
ALTER TABLE connector_property DROP COLUMN connector_oid_backup;
ALTER TABLE connector_property ADD FOREIGN KEY (connector_goid) REFERENCES connector (goid) ON DELETE CASCADE;

update rbac_role set entity_goid = toGoid(@connector_prefix,entity_oid) where entity_oid is not null and entity_type='SSG_CONNECTOR';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = goidToString(toGoid(@connector_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'SSG_CONNECTOR';

-- Firewall rule

call dropForeignKey('firewall_rule_property','firewall_rule');

ALTER TABLE firewall_rule ADD COLUMN objectid_backup BIGINT(20);
update firewall_rule set objectid_backup=objectid;
ALTER TABLE firewall_rule CHANGE COLUMN objectid goid binary(16) NOT NULL;
-- For manual runs use: set @firewall_prefix=createUnreservedPoorRandomPrefix();
SET @firewall_prefix=#RANDOM_LONG_NOT_RESERVED#;
update firewall_rule set goid = toGoid(@firewall_prefix,objectid_backup);
ALTER TABLE firewall_rule DROP COLUMN objectid_backup;

ALTER TABLE firewall_rule_property ADD COLUMN firewall_rule_oid_backup BIGINT(20);
UPDATE  firewall_rule_property SET firewall_rule_oid_backup = firewall_rule_oid;
ALTER TABLE firewall_rule_property CHANGE COLUMN firewall_rule_oid firewall_rule_goid binary(16) NOT NULL;
UPDATE firewall_rule_property SET firewall_rule_goid = toGoid(@firewall_prefix,firewall_rule_oid_backup);
ALTER TABLE firewall_rule_property DROP COLUMN firewall_rule_oid_backup;
ALTER TABLE firewall_rule_property  ADD FOREIGN KEY (firewall_rule_goid) REFERENCES firewall_rule (goid) ON DELETE CASCADE;

update rbac_role set entity_goid = toGoid(@firewall_prefix,entity_oid) where entity_oid is not null and entity_type='FIREWALL_RULE';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = goidToString(toGoid(@firewall_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'FIREWALL_RULE';

-- Encapsulated assertion

call dropForeignKey('encapsulated_assertion_property','encapsulated_assertion');
call dropForeignKey('encapsulated_assertion_argument','encapsulated_assertion');
call dropForeignKey('encapsulated_assertion_result','encapsulated_assertion');

ALTER TABLE encapsulated_assertion ADD COLUMN objectid_backup BIGINT(20);
update encapsulated_assertion set objectid_backup=objectid;
ALTER TABLE encapsulated_assertion CHANGE COLUMN objectid goid binary(16) NOT NULL;
-- For manual runs use: set @encapsulated_assertion_prefix=createUnreservedPoorRandomPrefix();
SET @encapsulated_assertion_prefix=#RANDOM_LONG_NOT_RESERVED#;
update encapsulated_assertion set goid = toGoid(@encapsulated_assertion_prefix,objectid_backup);
ALTER TABLE encapsulated_assertion DROP COLUMN objectid_backup;

ALTER TABLE encapsulated_assertion_property ADD COLUMN encapsulated_assertion_oid_backup BIGINT(20);
UPDATE  encapsulated_assertion_property SET encapsulated_assertion_oid_backup = encapsulated_assertion_oid;
ALTER TABLE encapsulated_assertion_property CHANGE COLUMN encapsulated_assertion_oid encapsulated_assertion_goid binary(16) NOT NULL;
UPDATE encapsulated_assertion_property SET encapsulated_assertion_goid = toGoid(@encapsulated_assertion_prefix,encapsulated_assertion_oid_backup);
ALTER TABLE encapsulated_assertion_property DROP COLUMN encapsulated_assertion_oid_backup;
ALTER TABLE encapsulated_assertion_property  ADD FOREIGN KEY (encapsulated_assertion_goid) REFERENCES encapsulated_assertion (goid) ON DELETE CASCADE;

ALTER TABLE encapsulated_assertion_argument ADD COLUMN objectid_backup BIGINT(20);
update encapsulated_assertion_argument set objectid_backup=objectid;
ALTER TABLE encapsulated_assertion_argument CHANGE COLUMN objectid goid binary(16) NOT NULL;
-- For manual runs use: set @encapsulated_assertion_argument_prefix=createUnreservedPoorRandomPrefix();
SET @encapsulated_assertion_argument_prefix=#RANDOM_LONG_NOT_RESERVED#;
update encapsulated_assertion_argument set goid = toGoid(@encapsulated_assertion_argument_prefix,objectid_backup);
ALTER TABLE encapsulated_assertion_argument DROP COLUMN objectid_backup;

ALTER TABLE encapsulated_assertion_argument ADD COLUMN encapsulated_assertion_oid_backup BIGINT(20);
UPDATE  encapsulated_assertion_argument SET encapsulated_assertion_oid_backup = encapsulated_assertion_oid;
ALTER TABLE encapsulated_assertion_argument CHANGE COLUMN encapsulated_assertion_oid encapsulated_assertion_goid binary(16) NOT NULL;
UPDATE encapsulated_assertion_argument SET encapsulated_assertion_goid = toGoid(@encapsulated_assertion_prefix,encapsulated_assertion_oid_backup);
ALTER TABLE encapsulated_assertion_argument DROP COLUMN encapsulated_assertion_oid_backup;
ALTER TABLE encapsulated_assertion_argument  ADD FOREIGN KEY (encapsulated_assertion_goid) REFERENCES encapsulated_assertion (goid) ON DELETE CASCADE;

ALTER TABLE encapsulated_assertion_result ADD COLUMN objectid_backup BIGINT(20);
update encapsulated_assertion_result set objectid_backup=objectid;
ALTER TABLE encapsulated_assertion_result CHANGE COLUMN objectid goid binary(16) NOT NULL;
-- For manual runs use: set @encapsulated_assertion_result_prefix=createUnreservedPoorRandomPrefix();
SET @encapsulated_assertion_result_prefix=#RANDOM_LONG_NOT_RESERVED#;
update encapsulated_assertion_result set goid = toGoid(@encapsulated_assertion_result_prefix,objectid_backup);
ALTER TABLE encapsulated_assertion_result DROP COLUMN objectid_backup;

ALTER TABLE encapsulated_assertion_result ADD COLUMN encapsulated_assertion_oid_backup BIGINT(20);
UPDATE  encapsulated_assertion_result SET encapsulated_assertion_oid_backup = encapsulated_assertion_oid;
ALTER TABLE encapsulated_assertion_result CHANGE COLUMN encapsulated_assertion_oid encapsulated_assertion_goid binary(16) NOT NULL;
UPDATE encapsulated_assertion_result SET encapsulated_assertion_goid = toGoid(@encapsulated_assertion_prefix,encapsulated_assertion_oid_backup);
ALTER TABLE encapsulated_assertion_result DROP COLUMN encapsulated_assertion_oid_backup;
ALTER TABLE encapsulated_assertion_result  ADD FOREIGN KEY (encapsulated_assertion_goid) REFERENCES encapsulated_assertion (goid) ON DELETE CASCADE;

update rbac_role set entity_goid = toGoid(@encapsulated_assertion_prefix,entity_oid) where entity_oid is not null and entity_type='ENCAPSULATED_ASSERTION';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = goidToString(toGoid(@encapsulated_assertion_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'ENCAPSULATED_ASSERTION';

-- jms

ALTER TABLE jms_endpoint ADD COLUMN old_objectid BIGINT(20);
update jms_endpoint set old_objectid=objectid;
ALTER TABLE jms_endpoint CHANGE COLUMN objectid goid binary(16) NOT NULL;
-- For manual runs use: set @jms_endpoint_prefix=createUnreservedPoorRandomPrefix();
SET @jms_endpoint_prefix=#RANDOM_LONG_NOT_RESERVED#;
update jms_endpoint set goid = toGoid(@jms_endpoint_prefix,old_objectid);

ALTER TABLE jms_connection ADD COLUMN objectid_backup BIGINT(20);
update jms_connection set objectid_backup=objectid;
ALTER TABLE jms_connection CHANGE COLUMN objectid goid binary(16) NOT NULL;
-- For manual runs use: set @jms_connection_prefix=createUnreservedPoorRandomPrefix();
SET @jms_connection_prefix=#RANDOM_LONG_NOT_RESERVED#;
update jms_connection set goid = toGoid(@jms_connection_prefix,objectid_backup);
ALTER TABLE jms_connection DROP COLUMN objectid_backup;

ALTER TABLE jms_endpoint ADD COLUMN connection_goid binary(16) NOT NULL;
update jms_endpoint set connection_goid = toGoid(@jms_connection_prefix,connection_oid);
ALTER TABLE jms_endpoint DROP COLUMN connection_oid;

update rbac_role set entity_goid = toGoid(@jms_connection_prefix,entity_oid) where entity_oid is not null and entity_type='JMS_CONNECTION';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = goidToString(toGoid(@jms_connection_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'JMS_CONNECTION';

update rbac_role set entity_goid = toGoid(@jms_endpoint_prefix,entity_oid) where entity_oid is not null and entity_type='JMS_ENDPOINT';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = goidToString(toGoid(@jms_endpoint_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'JMS_ENDPOINT';

-- http_configuration

ALTER TABLE http_configuration ADD COLUMN old_objectid BIGINT(20);
update http_configuration set old_objectid=objectid;
-- need to add goid as primary key, in 7.1.0 does not have object id set as the primary key
ALTER TABLE http_configuration CHANGE COLUMN objectid goid binary(16) NOT NULL PRIMARY KEY ;
-- For manual runs use: set @http_configuration_prefix=createUnreservedPoorRandomPrefix();
SET @http_configuration_prefix=#RANDOM_LONG_NOT_RESERVED#;
update http_configuration set goid = toGoid(@http_configuration_prefix,old_objectid);

update rbac_role set entity_goid = toGoid(@http_configuration_prefix,entity_oid) where entity_oid is not null and entity_type='HTTP_CONFIGURATION';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = hex(toGoid(@http_configuration_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'HTTP_CONFIGURATION';

-- active connector

call dropForeignKey('active_connector_property','active_connector');

ALTER TABLE active_connector ADD COLUMN old_objectid BIGINT(20);
update active_connector set old_objectid=objectid;
ALTER TABLE active_connector CHANGE COLUMN objectid goid binary(16) NOT NULL;
-- For manual runs use: set @active_connector_prefix=createUnreservedPoorRandomPrefix();
SET @active_connector_prefix=#RANDOM_LONG_NOT_RESERVED#;
update active_connector set goid = toGoid(@active_connector_prefix,old_objectid);

ALTER TABLE active_connector_property ADD COLUMN connector_oid_backup BIGINT(20);
UPDATE  active_connector_property SET connector_oid_backup = connector_oid;
ALTER TABLE active_connector_property CHANGE COLUMN connector_oid connector_goid binary(16) NOT NULL;
UPDATE active_connector_property SET connector_goid = toGoid(@active_connector_prefix,connector_oid_backup);
ALTER TABLE active_connector_property DROP COLUMN connector_oid_backup;
ALTER TABLE active_connector_property  ADD FOREIGN KEY (connector_goid) REFERENCES active_connector (goid) ON DELETE CASCADE;

update rbac_role set entity_goid = toGoid(@active_connector_prefix,entity_oid) where entity_oid is not null and entity_type='SSG_ACTIVE_CONNECTOR';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = goidToString(toGoid(@active_connector_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'SSG_ACTIVE_CONNECTOR';

-- client cert

ALTER TABLE client_cert ADD COLUMN old_objectid BIGINT(20);
update client_cert set old_objectid=objectid;
ALTER TABLE client_cert CHANGE COLUMN objectid goid binary(16) NOT NULL;
-- For manual runs use: set @client_cert_prefix=createUnreservedPoorRandomPrefix();
SET @client_cert_prefix=#RANDOM_LONG_NOT_RESERVED#;
update client_cert set goid = toGoid(@client_cert_prefix,old_objectid);
ALTER TABLE client_cert DROP COLUMN old_objectid;

-- trusted cert

call dropForeignKey('trusted_esm', 'trusted_cert');

ALTER TABLE trusted_cert ADD COLUMN old_objectid BIGINT(20);
update trusted_cert set old_objectid=objectid;
ALTER TABLE trusted_cert CHANGE COLUMN objectid goid binary(16) NOT NULL;
-- For manual runs use: set @trusted_cert_prefix=createUnreservedPoorRandomPrefix();
SET @trusted_cert_prefix=#RANDOM_LONG_NOT_RESERVED#;
update trusted_cert set goid = toGoid(@trusted_cert_prefix,old_objectid);
ALTER TABLE trusted_cert DROP COLUMN old_objectid;

ALTER TABLE trusted_esm ADD COLUMN trusted_cert_oid_backup BIGINT(20);
UPDATE  trusted_esm SET trusted_cert_oid_backup = trusted_cert_oid;
ALTER TABLE trusted_esm CHANGE COLUMN trusted_cert_oid trusted_cert_goid binary(16) NOT NULL;
UPDATE trusted_esm SET trusted_cert_goid = toGoid(@trusted_cert_prefix,trusted_cert_oid_backup);
ALTER TABLE trusted_esm DROP COLUMN trusted_cert_oid_backup;
ALTER TABLE trusted_esm ADD FOREIGN KEY (trusted_cert_goid) REFERENCES trusted_cert (goid);

update rbac_role set entity_goid = toGoid(@trusted_cert_prefix,entity_oid) where entity_oid is not null and entity_type='TRUSTED_CERT';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = goidToString(toGoid(@trusted_cert_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'TRUSTED_CERT';

-- revocation check policy

call dropForeignKey('trusted_cert', 'revocation_check_policy');

ALTER TABLE revocation_check_policy ADD COLUMN old_objectid BIGINT(20);
update revocation_check_policy set old_objectid=objectid;
ALTER TABLE revocation_check_policy CHANGE COLUMN objectid goid binary(16) NOT NULL;
-- For manual runs use: set @revocation_check_policy_prefix=createUnreservedPoorRandomPrefix();
SET @revocation_check_policy_prefix=#RANDOM_LONG_NOT_RESERVED#;
update revocation_check_policy set goid = toGoid(@revocation_check_policy_prefix,old_objectid);
ALTER TABLE revocation_check_policy DROP COLUMN old_objectid;

-- Note that old column name was revocation_policy_oid rather than revocation_check_policy_oid
ALTER TABLE trusted_cert ADD COLUMN revocation_check_policy_oid_backup BIGINT(20);
UPDATE  trusted_cert SET revocation_check_policy_oid_backup = revocation_policy_oid;
ALTER TABLE trusted_cert CHANGE COLUMN revocation_policy_oid revocation_check_policy_goid binary(16);
UPDATE trusted_cert SET revocation_check_policy_goid = toGoid(@revocation_check_policy_prefix,revocation_check_policy_oid_backup);
ALTER TABLE trusted_cert DROP COLUMN revocation_check_policy_oid_backup;
ALTER TABLE trusted_cert ADD FOREIGN KEY (revocation_check_policy_goid) REFERENCES revocation_check_policy (goid);

update rbac_role set entity_goid = toGoid(@revocation_check_policy_prefix,entity_oid) where entity_oid is not null and entity_type='REVOCATION_CHECK_POLICY';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = goidToString(toGoid(@revocation_check_policy_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'REVOCATION_CHECK_POLICY';

-- Service/Policy/Folder/Alias's
call dropForeignKey('folder','folder');
call dropForeignKey('published_service','folder');
call dropForeignKey('published_service_alias','folder');
call dropForeignKey('policy','folder');
call dropForeignKey('policy_alias','folder');
call dropForeignKey('rbac_predicate_folder','folder');

ALTER TABLE folder ADD COLUMN objectid_backup BIGINT(20);
update folder set objectid_backup=objectid;
DROP INDEX i_name_parent ON folder;
ALTER TABLE folder CHANGE COLUMN objectid goid binary(16) NOT NULL;
-- For manual runs use: set @folder_prefix=createUnreservedPoorRandomPrefix();
SET @folder_prefix=#RANDOM_LONG_NOT_RESERVED#;
update folder set goid = toGoid(@folder_prefix,objectid_backup);
update folder set goid = toGoid(0, -5002) where goid = toGoid(@folder_prefix, -5002);
ALTER TABLE folder DROP COLUMN objectid_backup;

ALTER TABLE folder ADD COLUMN parent_folder_oid_backup BIGINT(20);
update folder set parent_folder_oid_backup=parent_folder_oid;
ALTER TABLE folder CHANGE COLUMN parent_folder_oid parent_folder_goid binary(16);
update folder set parent_folder_goid = toGoid(@folder_prefix,parent_folder_oid_backup);
update folder set parent_folder_goid = toGoid(0, -5002) where parent_folder_goid = toGoid(@folder_prefix, -5002);
ALTER TABLE folder DROP COLUMN parent_folder_oid_backup;
ALTER TABLE folder ADD UNIQUE KEY `i_name_parent` (`name`,`parent_folder_goid`);

update rbac_role set entity_goid = toGoid(@folder_prefix,entity_oid) where entity_oid is not null and entity_type='FOLDER';
update rbac_role set entity_goid = toGoid(0, -5002) where entity_oid is not null and entity_type='FOLDER' and entity_goid = toGoid(@folder_prefix, -5002);
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = goidToString(toGoid(@folder_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'FOLDER';
update rbac_predicate_oid set entity_id = goidToString(toGoid(0, -5002)) where entity_id = goidToString(toGoid(@folder_prefix, -5002));

call dropForeignKey('policy_alias','policy');
call dropForeignKey('policy_version','policy');
call dropForeignKey('published_service','policy');
call dropForeignKey('encapsulated_assertion','policy');

ALTER TABLE policy ADD COLUMN objectid_backup BIGINT(20);
update policy set objectid_backup=objectid;
ALTER TABLE policy CHANGE COLUMN objectid goid binary(16) NOT NULL;
-- For manual runs use: set @policy_prefix=createUnreservedPoorRandomPrefix();
SET @policy_prefix=#RANDOM_LONG_NOT_RESERVED#;
update policy set goid = toGoid(@policy_prefix,objectid_backup);
ALTER TABLE policy DROP COLUMN objectid_backup;

ALTER TABLE policy ADD COLUMN folder_oid_backup BIGINT(20);
update policy set folder_oid_backup=folder_oid;
ALTER TABLE policy CHANGE COLUMN folder_oid folder_goid binary(16);
update policy set folder_goid = toGoid(@folder_prefix,folder_oid_backup);
update policy set folder_goid = toGoid(0, -5002) where folder_goid = toGoid(@folder_prefix, -5002);
ALTER TABLE policy DROP COLUMN folder_oid_backup;

update rbac_role set entity_goid = toGoid(@policy_prefix,entity_oid) where entity_oid is not null and entity_type='POLICY';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = goidToString(toGoid(@policy_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'POLICY';

ALTER TABLE policy_alias ADD COLUMN objectid_backup BIGINT(20);
update policy_alias set objectid_backup=objectid;
ALTER TABLE policy_alias CHANGE COLUMN objectid goid binary(16) NOT NULL;
-- For manual runs use: set @policy_alias_prefix=createUnreservedPoorRandomPrefix();
SET @policy_alias_prefix=#RANDOM_LONG_NOT_RESERVED#;
update policy_alias set goid = toGoid(@policy_alias_prefix,objectid_backup);
ALTER TABLE policy_alias DROP COLUMN objectid_backup;

ALTER TABLE policy_alias ADD COLUMN policy_oid_backup BIGINT(20);
update policy_alias set policy_oid_backup=policy_oid;
ALTER TABLE policy_alias CHANGE COLUMN policy_oid policy_goid binary(16) NOT NULL;
update policy_alias set policy_goid = toGoid(@policy_prefix,policy_oid_backup);
ALTER TABLE policy_alias DROP COLUMN policy_oid_backup;

ALTER TABLE policy_alias ADD COLUMN folder_oid_backup BIGINT(20);
update policy_alias set folder_oid_backup=folder_oid;
ALTER TABLE policy_alias CHANGE COLUMN folder_oid folder_goid binary(16) NOT NULL;
update policy_alias set folder_goid = toGoid(@folder_prefix,folder_oid_backup);
update policy_alias set folder_goid = toGoid(0, -5002) where folder_goid = toGoid(@folder_prefix, -5002);
ALTER TABLE policy_alias DROP COLUMN folder_oid_backup;

update rbac_role set entity_goid = toGoid(@policy_alias_prefix,entity_oid) where entity_oid is not null and entity_type='POLICY_ALIAS';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = goidToString(toGoid(@policy_alias_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'POLICY_ALIAS';

ALTER TABLE policy_version ADD COLUMN objectid_backup BIGINT(20);
update policy_version set objectid_backup=objectid;
ALTER TABLE policy_version CHANGE COLUMN objectid goid binary(16) NOT NULL;
-- For manual runs use: set @policy_version_prefix=createUnreservedPoorRandomPrefix();
SET @policy_version_prefix=#RANDOM_LONG_NOT_RESERVED#;
update policy_version set goid = toGoid(@policy_version_prefix,objectid_backup);
ALTER TABLE policy_version DROP COLUMN objectid_backup;

ALTER TABLE policy_version ADD COLUMN policy_oid_backup BIGINT(20);
update policy_version set policy_oid_backup=policy_oid;
ALTER TABLE policy_version CHANGE COLUMN policy_oid policy_goid binary(16) NOT NULL;
update policy_version set policy_goid = toGoid(@policy_prefix,policy_oid_backup);
ALTER TABLE policy_version DROP COLUMN policy_oid_backup;

update rbac_role set entity_goid = toGoid(@policy_version_prefix,entity_oid) where entity_oid is not null and entity_type='POLICY_VERSION';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = goidToString(toGoid(@policy_version_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'POLICY_VERSION';

call dropForeignKey('published_service_alias','published_service');
call dropForeignKey('sample_messages','published_service');
call dropForeignKey('service_documents','published_service');
call dropForeignKey('uddi_business_service_status','published_service');
call dropForeignKey('uddi_proxied_service_info','published_service');
call dropForeignKey('uddi_service_control','published_service');

ALTER TABLE published_service ADD COLUMN objectid_backup BIGINT(20);
update published_service set objectid_backup=objectid;
ALTER TABLE published_service CHANGE COLUMN objectid goid binary(16) NOT NULL;
-- For manual runs use: set @published_service_prefix=createUnreservedPoorRandomPrefix();
SET @published_service_prefix=#RANDOM_LONG_NOT_RESERVED#;
update published_service set goid = toGoid(@published_service_prefix,objectid_backup);
ALTER TABLE published_service DROP COLUMN objectid_backup;

ALTER TABLE published_service ADD COLUMN folder_oid_backup BIGINT(20);
update published_service set folder_oid_backup=folder_oid;
ALTER TABLE published_service CHANGE COLUMN folder_oid folder_goid binary(16);
update published_service set folder_goid = toGoid(@folder_prefix,folder_oid_backup);
update published_service set folder_goid = toGoid(0, -5002) where folder_goid = toGoid(@folder_prefix, -5002);
ALTER TABLE published_service DROP COLUMN folder_oid_backup;

ALTER TABLE published_service ADD COLUMN policy_oid_backup BIGINT(20);
update published_service set policy_oid_backup=policy_oid;
ALTER TABLE published_service CHANGE COLUMN policy_oid policy_goid binary(16);
update published_service set policy_goid = toGoid(@policy_prefix,policy_oid_backup);
ALTER TABLE published_service DROP COLUMN policy_oid_backup;

update rbac_role set entity_goid = toGoid(@published_service_prefix,entity_oid) where entity_oid is not null and entity_type='SERVICE';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = goidToString(toGoid(@published_service_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'SERVICE';

ALTER TABLE published_service_alias ADD COLUMN objectid_backup BIGINT(20);
update published_service_alias set objectid_backup=objectid;
ALTER TABLE published_service_alias CHANGE COLUMN objectid goid binary(16) NOT NULL;
-- For manual runs use: set @published_service_alias_prefix=createUnreservedPoorRandomPrefix();
SET @published_service_alias_prefix=#RANDOM_LONG_NOT_RESERVED#;
update published_service_alias set goid = toGoid(@published_service_alias_prefix,objectid_backup);
ALTER TABLE published_service_alias DROP COLUMN objectid_backup;

ALTER TABLE published_service_alias ADD COLUMN folder_oid_backup BIGINT(20);
update published_service_alias set folder_oid_backup=folder_oid;
ALTER TABLE published_service_alias CHANGE COLUMN folder_oid folder_goid binary(16) NOT NULL;
update published_service_alias set folder_goid = toGoid(@folder_prefix,folder_oid_backup);
update published_service_alias set folder_goid = toGoid(0, -5002) where folder_goid = toGoid(@folder_prefix, -5002);
ALTER TABLE published_service_alias DROP COLUMN folder_oid_backup;

ALTER TABLE published_service_alias ADD COLUMN published_service_oid_backup BIGINT(20);
update published_service_alias set published_service_oid_backup=published_service_oid;
ALTER TABLE published_service_alias CHANGE COLUMN published_service_oid published_service_goid binary(16) NOT NULL;
update published_service_alias set published_service_goid = toGoid(@published_service_prefix,published_service_oid_backup);
ALTER TABLE published_service_alias DROP COLUMN published_service_oid_backup;

update rbac_role set entity_goid = toGoid(@published_service_alias_prefix,entity_oid) where entity_oid is not null and entity_type='SERVICE_ALIAS';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = goidToString(toGoid(@published_service_alias_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'SERVICE_ALIAS';

ALTER TABLE audit_message ADD COLUMN service_oid_backup BIGINT(20);
update audit_message set service_oid_backup=service_oid;
ALTER TABLE audit_message CHANGE COLUMN service_oid service_goid binary(16);
update audit_message set service_goid = toGoid(@policy_prefix,service_oid_backup);
ALTER TABLE audit_message DROP COLUMN service_oid_backup;

ALTER TABLE active_connector ADD COLUMN hardwired_service_oid_backup BIGINT(20);
update active_connector set hardwired_service_oid_backup=hardwired_service_oid;
ALTER TABLE active_connector CHANGE COLUMN hardwired_service_oid hardwired_service_goid binary(16);
update active_connector set hardwired_service_goid = toGoid(@published_service_prefix,hardwired_service_oid_backup);
ALTER TABLE active_connector DROP COLUMN hardwired_service_oid_backup;

ALTER TABLE rbac_predicate_folder ADD COLUMN folder_oid_backup BIGINT(20);
update rbac_predicate_folder set folder_oid_backup=folder_oid;
ALTER TABLE rbac_predicate_folder CHANGE COLUMN folder_oid folder_goid binary(16) NOT NULL;
update rbac_predicate_folder set folder_goid = toGoid(@folder_prefix,folder_oid_backup);
update rbac_predicate_folder set folder_goid = toGoid(0, -5002) where folder_goid = toGoid(@folder_prefix, -5002);
ALTER TABLE rbac_predicate_folder DROP COLUMN folder_oid_backup;

ALTER TABLE sample_messages ADD COLUMN published_service_oid_backup BIGINT(20);
update sample_messages set published_service_oid_backup=published_service_oid;
ALTER TABLE sample_messages CHANGE COLUMN published_service_oid published_service_goid binary(16);
update sample_messages set published_service_goid = toGoid(@published_service_prefix,published_service_oid_backup);
ALTER TABLE sample_messages DROP COLUMN published_service_oid_backup;

ALTER TABLE service_documents ADD COLUMN objectid_backup BIGINT(20);
update service_documents set objectid_backup=objectid;
ALTER TABLE service_documents CHANGE COLUMN objectid goid binary(16) NOT NULL;
-- For manual runs use: set @published_service_alias_prefix=createUnreservedPoorRandomPrefix();
SET @service_documents_prefix=#RANDOM_LONG_NOT_RESERVED#;
update service_documents set goid = toGoid(@service_documents_prefix,objectid_backup);
ALTER TABLE service_documents DROP COLUMN objectid_backup;

update rbac_role set entity_goid = toGoid(@service_documents_prefix,entity_oid) where entity_oid is not null and entity_type='SERVICE_DOCUMENT';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = goidToString(toGoid(@service_documents_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'SERVICE_DOCUMENT';

ALTER TABLE service_documents ADD COLUMN service_oid_backup BIGINT(20);
update service_documents set service_oid_backup=service_oid;
ALTER TABLE service_documents CHANGE COLUMN service_oid service_goid binary(16) NOT NULL;
update service_documents set service_goid = toGoid(@published_service_prefix,service_oid_backup);
ALTER TABLE service_documents DROP COLUMN service_oid_backup;

ALTER TABLE service_metrics ADD COLUMN published_service_oid_backup BIGINT(20);
update service_metrics set published_service_oid_backup=published_service_oid;
ALTER TABLE service_metrics CHANGE COLUMN published_service_oid published_service_goid binary(16) NOT NULL;
update service_metrics set published_service_goid = toGoid(@published_service_prefix,published_service_oid_backup);
ALTER TABLE service_metrics DROP COLUMN published_service_oid_backup;

ALTER TABLE service_usage ADD COLUMN serviceid_backup BIGINT(20);
update service_usage set serviceid_backup=serviceid;
ALTER TABLE service_usage CHANGE COLUMN serviceid serviceid binary(16) NOT NULL;
update service_usage set serviceid = toGoid(@published_service_prefix,serviceid_backup);
ALTER TABLE service_usage DROP COLUMN serviceid_backup;

update rbac_role set entity_goid = toGoid(@published_service_prefix,entity_oid) where entity_oid is not null and entity_type='SERVICE_USAGE';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = goidToString(toGoid(@published_service_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'SERVICE_USAGE';

ALTER TABLE uddi_business_service_status ADD COLUMN published_service_oid_backup BIGINT(20);
update uddi_business_service_status set published_service_oid_backup=published_service_oid;
ALTER TABLE uddi_business_service_status CHANGE COLUMN published_service_oid published_service_goid binary(16) NOT NULL;
update uddi_business_service_status set published_service_goid = toGoid(@published_service_prefix,published_service_oid_backup);
ALTER TABLE uddi_business_service_status DROP COLUMN published_service_oid_backup;

ALTER TABLE uddi_proxied_service_info ADD COLUMN published_service_oid_backup BIGINT(20);
update uddi_proxied_service_info set published_service_oid_backup=published_service_oid;
ALTER TABLE uddi_proxied_service_info CHANGE COLUMN published_service_oid published_service_goid binary(16) NOT NULL;
update uddi_proxied_service_info set published_service_goid = toGoid(@published_service_prefix,published_service_oid_backup);
ALTER TABLE uddi_proxied_service_info DROP COLUMN published_service_oid_backup;

ALTER TABLE uddi_service_control ADD COLUMN published_service_oid_backup BIGINT(20);
update uddi_service_control set published_service_oid_backup=published_service_oid;
ALTER TABLE uddi_service_control CHANGE COLUMN published_service_oid published_service_goid binary(16) NOT NULL;
update uddi_service_control set published_service_goid = toGoid(@published_service_prefix,published_service_oid_backup);
ALTER TABLE uddi_service_control DROP COLUMN published_service_oid_backup;

ALTER TABLE wsdm_subscription ADD COLUMN published_service_oid_backup BIGINT(20);
update wsdm_subscription set published_service_oid_backup=published_service_oid;
ALTER TABLE wsdm_subscription CHANGE COLUMN published_service_oid published_service_goid binary(16) NOT NULL;
update wsdm_subscription set published_service_goid = toGoid(@published_service_prefix,published_service_oid_backup);
ALTER TABLE wsdm_subscription DROP COLUMN published_service_oid_backup;

ALTER TABLE wsdm_subscription ADD COLUMN esm_service_oid_backup BIGINT(20);
update wsdm_subscription set esm_service_oid_backup=esm_service_oid;
ALTER TABLE wsdm_subscription CHANGE COLUMN esm_service_oid esm_service_goid binary(16) NOT NULL DEFAULT X'0000000000000000FFFFFFFFFFFFFFFF';
update wsdm_subscription set esm_service_goid = toGoid(@published_service_prefix,esm_service_oid_backup);
ALTER TABLE wsdm_subscription DROP COLUMN esm_service_oid_backup;

ALTER TABLE encapsulated_assertion ADD COLUMN policy_oid_backup BIGINT(20);
update encapsulated_assertion set policy_oid_backup=policy_oid;
ALTER TABLE encapsulated_assertion CHANGE COLUMN policy_oid policy_goid binary(16) NOT NULL;
update encapsulated_assertion set policy_goid = toGoid(@policy_prefix,policy_oid_backup);
ALTER TABLE encapsulated_assertion DROP COLUMN policy_oid_backup;


ALTER TABLE folder ADD CONSTRAINT folder_parent_folder FOREIGN KEY (parent_folder_goid) REFERENCES folder (goid);
ALTER TABLE published_service ADD CONSTRAINT published_service_folder FOREIGN KEY (folder_goid) REFERENCES folder (goid);
ALTER TABLE published_service_alias ADD FOREIGN KEY (folder_goid) REFERENCES folder (goid) ON DELETE CASCADE;
ALTER TABLE policy ADD CONSTRAINT policy_folder FOREIGN KEY (folder_goid) REFERENCES folder (goid);
ALTER TABLE policy_alias ADD FOREIGN KEY (folder_goid) REFERENCES folder (goid) ON DELETE CASCADE;
ALTER TABLE rbac_predicate_folder ADD FOREIGN KEY (folder_goid) REFERENCES folder (goid) ON DELETE CASCADE;

ALTER TABLE policy_alias ADD FOREIGN KEY (policy_goid) REFERENCES policy (goid) ON DELETE CASCADE;
ALTER TABLE encapsulated_assertion ADD FOREIGN KEY (policy_goid) REFERENCES policy (goid);
ALTER TABLE published_service ADD FOREIGN KEY (policy_goid) REFERENCES policy (goid);
ALTER TABLE policy_version ADD FOREIGN KEY (policy_goid) REFERENCES policy (goid) ON DELETE CASCADE;

ALTER TABLE published_service_alias ADD FOREIGN KEY (published_service_goid) REFERENCES published_service (goid) ON DELETE CASCADE;
ALTER TABLE sample_messages ADD FOREIGN KEY (published_service_goid) REFERENCES published_service (goid) ON DELETE CASCADE;
ALTER TABLE service_documents ADD FOREIGN KEY (service_goid) REFERENCES published_service (goid) ON DELETE CASCADE;
ALTER TABLE uddi_proxied_service_info ADD FOREIGN KEY (published_service_goid) REFERENCES published_service (goid) ON DELETE CASCADE;
ALTER TABLE uddi_business_service_status ADD FOREIGN KEY (published_service_goid) REFERENCES published_service (goid) ON DELETE CASCADE;
ALTER TABLE uddi_service_control ADD FOREIGN KEY (published_service_goid) REFERENCES published_service (goid) ON DELETE CASCADE;

update rbac_predicate_attribute set value = goidToString(toGoid(@published_service_prefix, value)) where attribute='serviceOid' OR attribute='publishedServiceOid' OR attribute='serviceid';
update rbac_predicate_attribute set attribute = 'serviceGoid' where attribute='serviceOid';
update rbac_predicate_attribute set attribute = 'publishedServiceGoid' where attribute='publishedServiceOid';

update rbac_predicate_entityfolder set entity_id = goidToString(toGoid(@published_service_prefix, entity_id)) where entity_type='SERVICE';
update rbac_predicate_entityfolder set entity_id = goidToString(toGoid(@folder_prefix, entity_id)) where entity_type='FOLDER';
update rbac_predicate_entityfolder set entity_id = goidToString(toGoid(0, -5002)) where entity_type='FOLDER' and entity_id=goidToString(toGoid(@folder_prefix, -5002));
update rbac_predicate_entityfolder set entity_id = goidToString(toGoid(@policy_prefix, entity_id)) where entity_type='POLICY';

-- UDDI
call dropForeignKey('uddi_registry_subscription','uddi_registries');
call dropForeignKey('uddi_proxied_service_info','uddi_registries');
call dropForeignKey('uddi_proxied_service','uddi_proxied_service_info');
call dropForeignKey('uddi_publish_status','uddi_proxied_service_info');
call dropForeignKey('uddi_business_service_status','uddi_registries');
call dropForeignKey('uddi_service_control','uddi_registries');
call dropForeignKey('uddi_service_control_monitor_runtime','uddi_service_control');

ALTER TABLE uddi_registries ADD COLUMN objectid_backup BIGINT(20);
update uddi_registries set objectid_backup=objectid;
ALTER TABLE uddi_registries CHANGE COLUMN objectid goid binary(16) NOT NULL;
-- For manual runs use: set @uddi_registries_prefix=createUnreservedPoorRandomPrefix();
SET @uddi_registries_prefix=#RANDOM_LONG_NOT_RESERVED#;
update uddi_registries set goid = toGoid(@uddi_registries_prefix,objectid_backup);
ALTER TABLE uddi_registries DROP COLUMN objectid_backup;

update rbac_role set entity_goid = toGoid(@uddi_registries_prefix,entity_oid) where entity_oid is not null and entity_type='UDDI_REGISTRY';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = goidToString(toGoid(@uddi_registries_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'UDDI_REGISTRY';


ALTER TABLE uddi_registry_subscription ADD COLUMN objectid_backup BIGINT(20);
update uddi_registry_subscription set objectid_backup=objectid;
ALTER TABLE uddi_registry_subscription CHANGE COLUMN objectid goid binary(16) NOT NULL;
-- For manual runs use: set @uddi_registry_subscription_prefix=createUnreservedPoorRandomPrefix();
SET @uddi_registry_subscription_prefix=#RANDOM_LONG_NOT_RESERVED#;
update uddi_registry_subscription set goid = toGoid(@uddi_registry_subscription_prefix,objectid_backup);
ALTER TABLE uddi_registry_subscription DROP COLUMN objectid_backup;

ALTER TABLE uddi_registry_subscription ADD COLUMN uddi_registry_oid_backup BIGINT(20);
update uddi_registry_subscription set uddi_registry_oid_backup=uddi_registry_oid;
ALTER TABLE uddi_registry_subscription CHANGE COLUMN uddi_registry_oid uddi_registry_goid binary(16) NOT NULL;
update uddi_registry_subscription set uddi_registry_goid = toGoid(@uddi_registries_prefix,uddi_registry_oid_backup);
ALTER TABLE uddi_registry_subscription DROP COLUMN uddi_registry_oid_backup;


ALTER TABLE uddi_proxied_service_info ADD COLUMN objectid_backup BIGINT(20);
update uddi_proxied_service_info set objectid_backup=objectid;
ALTER TABLE uddi_proxied_service_info CHANGE COLUMN objectid goid binary(16) NOT NULL;
-- For manual runs use: set @uddi_proxied_service_info_prefix=createUnreservedPoorRandomPrefix();
SET @uddi_proxied_service_info_prefix=#RANDOM_LONG_NOT_RESERVED#;
update uddi_proxied_service_info set goid = toGoid(@uddi_proxied_service_info_prefix,objectid_backup);
ALTER TABLE uddi_proxied_service_info DROP COLUMN objectid_backup;

ALTER TABLE uddi_proxied_service_info ADD COLUMN uddi_registry_oid_backup BIGINT(20);
update uddi_proxied_service_info set uddi_registry_oid_backup=uddi_registry_oid;
ALTER TABLE uddi_proxied_service_info CHANGE COLUMN uddi_registry_oid uddi_registry_goid binary(16) NOT NULL;
update uddi_proxied_service_info set uddi_registry_goid = toGoid(@uddi_registries_prefix,uddi_registry_oid_backup);
ALTER TABLE uddi_proxied_service_info DROP COLUMN uddi_registry_oid_backup;

update rbac_role set entity_goid = toGoid(@uddi_proxied_service_info_prefix,entity_oid) where entity_oid is not null and entity_type='UDDI_PROXIED_SERVICE_INFO';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = goidToString(toGoid(@uddi_proxied_service_info_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'UDDI_PROXIED_SERVICE_INFO';


ALTER TABLE uddi_proxied_service ADD COLUMN objectid_backup BIGINT(20);
update uddi_proxied_service set objectid_backup=objectid;
ALTER TABLE uddi_proxied_service CHANGE COLUMN objectid goid binary(16) NOT NULL;
-- For manual runs use: set @uddi_proxied_service_prefix=createUnreservedPoorRandomPrefix();
SET @uddi_proxied_service_prefix=#RANDOM_LONG_NOT_RESERVED#;
update uddi_proxied_service set goid = toGoid(@uddi_proxied_service_prefix,objectid_backup);
ALTER TABLE uddi_proxied_service DROP COLUMN objectid_backup;

ALTER TABLE uddi_proxied_service ADD COLUMN uddi_proxied_service_info_oid_backup BIGINT(20);
update uddi_proxied_service set uddi_proxied_service_info_oid_backup=uddi_proxied_service_info_oid;
ALTER TABLE uddi_proxied_service CHANGE COLUMN uddi_proxied_service_info_oid uddi_proxied_service_info_goid binary(16) NOT NULL;
update uddi_proxied_service set uddi_proxied_service_info_goid = toGoid(@uddi_proxied_service_info_prefix,uddi_proxied_service_info_oid_backup);
ALTER TABLE uddi_proxied_service DROP COLUMN uddi_proxied_service_info_oid_backup;


ALTER TABLE uddi_publish_status ADD COLUMN objectid_backup BIGINT(20);
update uddi_publish_status set objectid_backup=objectid;
ALTER TABLE uddi_publish_status CHANGE COLUMN objectid goid binary(16) NOT NULL;
-- For manual runs use: set @uddi_publish_status_prefix=createUnreservedPoorRandomPrefix();
SET @uddi_publish_status_prefix=#RANDOM_LONG_NOT_RESERVED#;
update uddi_publish_status set goid = toGoid(@uddi_publish_status_prefix,objectid_backup);
ALTER TABLE uddi_publish_status DROP COLUMN objectid_backup;

ALTER TABLE uddi_publish_status ADD COLUMN uddi_proxied_service_info_oid_backup BIGINT(20);
update uddi_publish_status set uddi_proxied_service_info_oid_backup=uddi_proxied_service_info_oid;
ALTER TABLE uddi_publish_status CHANGE COLUMN uddi_proxied_service_info_oid uddi_proxied_service_info_goid binary(16) NOT NULL;
update uddi_publish_status set uddi_proxied_service_info_goid = toGoid(@uddi_proxied_service_info_prefix,uddi_proxied_service_info_oid_backup);
ALTER TABLE uddi_publish_status DROP COLUMN uddi_proxied_service_info_oid_backup;


ALTER TABLE uddi_business_service_status ADD COLUMN objectid_backup BIGINT(20);
update uddi_business_service_status set objectid_backup=objectid;
ALTER TABLE uddi_business_service_status CHANGE COLUMN objectid goid binary(16) NOT NULL;
-- For manual runs use: set @uddi_business_service_status_prefix=createUnreservedPoorRandomPrefix();
SET @uddi_business_service_status_prefix=#RANDOM_LONG_NOT_RESERVED#;
update uddi_business_service_status set goid = toGoid(@uddi_business_service_status_prefix,objectid_backup);
ALTER TABLE uddi_business_service_status DROP COLUMN objectid_backup;

ALTER TABLE uddi_business_service_status ADD COLUMN uddi_registry_oid_backup BIGINT(20);
update uddi_business_service_status set uddi_registry_oid_backup=uddi_registry_oid;
ALTER TABLE uddi_business_service_status CHANGE COLUMN uddi_registry_oid uddi_registry_goid binary(16) NOT NULL;
update uddi_business_service_status set uddi_registry_goid = toGoid(@uddi_registries_prefix,uddi_registry_oid_backup);
ALTER TABLE uddi_business_service_status DROP COLUMN uddi_registry_oid_backup;


ALTER TABLE uddi_service_control ADD COLUMN objectid_backup BIGINT(20);
update uddi_service_control set objectid_backup=objectid;
ALTER TABLE uddi_service_control CHANGE COLUMN objectid goid binary(16) NOT NULL;
-- For manual runs use: set @uddi_service_control_prefix=createUnreservedPoorRandomPrefix();
SET @uddi_service_control_prefix=#RANDOM_LONG_NOT_RESERVED#;
update uddi_service_control set goid = toGoid(@uddi_service_control_prefix,objectid_backup);
ALTER TABLE uddi_service_control DROP COLUMN objectid_backup;

ALTER TABLE uddi_service_control ADD COLUMN uddi_registry_oid_backup BIGINT(20);
update uddi_service_control set uddi_registry_oid_backup=uddi_registry_oid;
ALTER TABLE uddi_service_control CHANGE COLUMN uddi_registry_oid uddi_registry_goid binary(16) NOT NULL;
update uddi_service_control set uddi_registry_goid = toGoid(@uddi_registries_prefix,uddi_registry_oid_backup);
ALTER TABLE uddi_service_control DROP COLUMN uddi_registry_oid_backup;

update rbac_role set entity_goid = toGoid(@uddi_service_control_prefix,entity_oid) where entity_oid is not null and entity_type='UDDI_SERVICE_CONTROL';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = goidToString(toGoid(@uddi_service_control_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'UDDI_SERVICE_CONTROL';


ALTER TABLE uddi_service_control_monitor_runtime ADD COLUMN objectid_backup BIGINT(20);
update uddi_service_control_monitor_runtime set objectid_backup=objectid;
ALTER TABLE uddi_service_control_monitor_runtime CHANGE COLUMN objectid goid binary(16) NOT NULL;
-- For manual runs use: set @uddi_service_control_monitor_runtime_prefix=createUnreservedPoorRandomPrefix();
SET @uddi_service_control_monitor_runtime_prefix=#RANDOM_LONG_NOT_RESERVED#;
update uddi_service_control_monitor_runtime set goid = toGoid(@uddi_service_control_monitor_runtime_prefix,objectid_backup);
ALTER TABLE uddi_service_control_monitor_runtime DROP COLUMN objectid_backup;

ALTER TABLE uddi_service_control_monitor_runtime ADD COLUMN uddi_service_control_oid_backup BIGINT(20);
update uddi_service_control_monitor_runtime set uddi_service_control_oid_backup=uddi_service_control_oid;
ALTER TABLE uddi_service_control_monitor_runtime CHANGE COLUMN uddi_service_control_oid uddi_service_control_goid binary(16) NOT NULL;
update uddi_service_control_monitor_runtime set uddi_service_control_goid = toGoid(@uddi_service_control_prefix,uddi_service_control_oid_backup);
ALTER TABLE uddi_service_control_monitor_runtime DROP COLUMN uddi_service_control_oid_backup;


ALTER TABLE uddi_registry_subscription ADD FOREIGN KEY (uddi_registry_goid) REFERENCES uddi_registries (goid) ON DELETE CASCADE;
ALTER TABLE uddi_proxied_service_info ADD FOREIGN KEY (uddi_registry_goid) REFERENCES uddi_registries (goid) ON DELETE CASCADE;
ALTER TABLE uddi_proxied_service ADD FOREIGN KEY (uddi_proxied_service_info_goid) REFERENCES uddi_proxied_service_info (goid) ON DELETE CASCADE;
ALTER TABLE uddi_publish_status ADD FOREIGN KEY (uddi_proxied_service_info_goid) REFERENCES uddi_proxied_service_info (goid) ON DELETE CASCADE;
ALTER TABLE uddi_business_service_status ADD FOREIGN KEY (uddi_registry_goid) REFERENCES uddi_registries (goid) ON DELETE CASCADE;
ALTER TABLE uddi_service_control ADD FOREIGN KEY (uddi_registry_goid) REFERENCES uddi_registries (goid) ON DELETE CASCADE;
ALTER TABLE uddi_service_control_monitor_runtime ADD FOREIGN KEY (uddi_service_control_goid) REFERENCES uddi_service_control (goid) ON DELETE CASCADE;
--
-- Register upgrade task for upgrading sink configuration references to GOIDs
--
INSERT INTO cluster_properties
    (goid, version, propkey, propvalue, properties)
    values (toGoid(0,-800001), 0, 'upgrade.task.800001', 'com.l7tech.server.upgrade.Upgrade71To80SinkConfig', null),
           (toGoid(0,-800002), 0, 'upgrade.task.800002', 'com.l7tech.server.upgrade.Upgrade71To80IdReferences', null);


--
-- License documents for updated licensing model
--

CREATE TABLE license_document (
  goid binary(16) NOT NULL,
  version integer NOT NULL,
  contents mediumtext,
  PRIMARY KEY (goid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;


CREATE TABLE goid_upgrade_map (
  prefix bigint NOT NULL,
  table_name varchar(255) NOT NULL,
  PRIMARY KEY (prefix, table_name)
);

INSERT INTO goid_upgrade_map (table_name, prefix) VALUES
      ('jdbc_connection', @jdbc_prefix),
      ('logon_info', @logonInfo_prefix),
      ('service_metrics', @metrics_prefix),
      ('sample_messages', @sample_messages_prefix),
      ('cluster_properties', @cluster_properties_prefix),
      ('email_listener', @email_prefix),
      ('email_listener_state', @emailState_prefix),
      ('generic_entity', @generic_entity_prefix),
      ('connector', @connector_prefix),
      ('firewall_rule', @firewall_prefix),
      ('encapsulated_assertion', @encapsulated_assertion_prefix),
      ('encapsulated_assertion_argument', @encapsulated_assertion_argument_prefix),
      ('encapsulated_assertion_result', @encapsulated_assertion_result_prefix),
      ('jms_connection', @jms_connection_prefix),
      ('jms_endpoint', @jms_endpoint_prefix),
      ('http_configuration', @http_configuration_prefix),
      ('active_connector', @active_connector_prefix),
      ('folder', @folder_prefix),
      ('policy', @policy_prefix),
      ('policy_alias', @policy_alias_prefix),
      ('policy_version', @policy_version_prefix),
      ('published_service', @published_service_prefix),
      ('published_service_alias', @published_service_alias_prefix),
      ('service_documents', @service_documents_prefix),
      ('uddi_registries', @uddi_registries_prefix),
      ('uddi_registry_subscription', @uddi_registry_subscription_prefix),
      ('uddi_proxied_service_info', @uddi_proxied_service_info_prefix),
      ('uddi_proxied_service', @uddi_proxied_service_prefix),
      ('uddi_publish_status', @uddi_publish_status_prefix),
      ('uddi_business_service_status', @uddi_business_service_status_prefix),
      ('uddi_service_control', @uddi_service_control_prefix),
      ('uddi_service_control_monitor_runtime', @uddi_service_control_monitor_runtime_prefix),
      ('client_cert', @client_cert_prefix),
      ('trusted_cert', @trusted_cert_prefix),
      ('revocation_check_policy', @revocation_check_policy_prefix),
      ('resource_entry', @resource_entry_prefix),
      ('secure_password', @secure_password_prefix);


--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--

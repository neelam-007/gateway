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

-- The dropForeignKey function will drop a foreign key constraint on a table where the constraint does not have a name.
-- The first parameter is the table name that has the constraint. The second parameter is the table that the constraint
-- references. If there are 2 different foreign key references to the same table an error will be returned.
DROP PROCEDURE IF EXISTS dropForeignKey;
delimiter //
create procedure dropForeignKey(in tableName varchar(255), in referenceTableName varchar(255))
begin
	set @ssgSchema = SCHEMA();
	SELECT count(*) into @constraint_count FROM information_schema.REFERENTIAL_CONSTRAINTS
	WHERE constraint_schema = @ssgSchema AND table_name = tableName and referenced_table_name=referenceTableName;
    if @constraint_count > 1 then set @error_message = concat('\'',tableName, '\' table has more then one foreign key references to \'', referenceTableName,'\''); SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = @error_message; end if;
	SELECT constraint_name into @constraint_name FROM information_schema.REFERENTIAL_CONSTRAINTS
	WHERE constraint_schema = @ssgSchema AND table_name = tableName and referenced_table_name=referenceTableName;
	SET @s = CONCAT('ALTER TABLE ', tableName, ' DROP FOREIGN KEY ', @constraint_name);
	PREPARE stmt FROM @s;
	EXECUTE stmt;
	DEALLOCATE PREPARE stmt;
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
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  name varchar(255) NOT NULL,
  security_zone_goid BINARY(16),
  PRIMARY KEY (objectid),
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


-- create new RBAC role for SiteMinder Configuration --
INSERT INTO rbac_role (objectid, version, name, tag, entity_type, entity_oid, description, user_created) VALUES (-1500,0,'Manage SiteMinder Configuration', null, 'SITEMINDER_CONFIGURATION', null,'Users assigned to the {0} role have the ability to read, create, update and delete SiteMinder configuration.',0);
INSERT INTO rbac_permission VALUES (-1501,0,-1500,'READ',NULL,'SITEMINDER_CONFIGURATION');
INSERT INTO rbac_permission VALUES (-1502,0,-1500,'CREATE',NULL,'SITEMINDER_CONFIGURATION');
INSERT INTO rbac_permission VALUES (-1503,0,-1500,'UPDATE',NULL,'SITEMINDER_CONFIGURATION');
INSERT INTO rbac_permission VALUES (-1504,0,-1500,'DELETE',NULL,'SITEMINDER_CONFIGURATION');
INSERT INTO rbac_permission VALUES (-1505,0,-1500,'READ',NULL,'SECURE_PASSWORD');


alter table rbac_role add column entity_goid BINARY(16);
CREATE INDEX i_rbacrole_egoid ON rbac_role (entity_goid);

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
INSERT INTO rbac_permission VALUES (-360,0,-350,'READ',NULL,'ASSERTION_ACCESS');
INSERT INTO rbac_permission VALUES (-361,0,-350,'CREATE',NULL,'ASSERTION_ACCESS');
INSERT INTO rbac_permission VALUES (-442,0,-400,'CREATE',NULL,'ASSERTION_ACCESS');
INSERT INTO rbac_permission VALUES (-443,0,-400,'READ',NULL,'ASSERTION_ACCESS');

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
  version int(11) NOT NULL,
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

INSERT INTO rbac_role(objectid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (-1450,0,'Manage Custom Key Value Store', null,'CUSTOM_KEY_VALUE_STORE',null,null, 'Users assigned to the {0} role have the ability to read, create, update, and delete key values from custom key value store.',0);
INSERT INTO rbac_permission VALUES (-1451,0,-1450,'CREATE',null,'CUSTOM_KEY_VALUE_STORE');
INSERT INTO rbac_permission VALUES (-1452,0,-1450,'READ',null,'CUSTOM_KEY_VALUE_STORE');
INSERT INTO rbac_permission VALUES (-1453,0,-1450,'UPDATE',null,'CUSTOM_KEY_VALUE_STORE');
INSERT INTO rbac_permission VALUES (-1454,0,-1450,'DELETE',null,'CUSTOM_KEY_VALUE_STORE');

--
-- Goidification modification. These involve replacing the oid column with a goid column on entity tables.
--

-- JdbcConnection
ALTER TABLE jdbc_connection ADD COLUMN objectid_backup BIGINT(20);
update jdbc_connection set objectid_backup=objectid;
ALTER TABLE jdbc_connection CHANGE COLUMN objectid goid BINARY(16);
-- For manual runs use: set @jdbc_prefix=createUnreservedPoorRandomPrefix();
set @jdbc_prefix=#RANDOM_LONG_NOT_RESERVED#;
update jdbc_connection set goid = toGoid(@jdbc_prefix,objectid_backup);
ALTER TABLE jdbc_connection DROP COLUMN objectid_backup;
update rbac_role set entity_goid = toGoid(@jdbc_prefix,entity_oid) where entity_oid is not null and entity_type='JDBC_CONNECTION';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = hex(toGoid(@jdbc_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'JDBC_CONNECTION';

-- MetricsBin, MetricsBinDetail
call dropForeignKey('service_metrics_details','service_metrics');

ALTER TABLE service_metrics ADD COLUMN objectid_backup BIGINT(20);
UPDATE service_metrics SET objectid_backup=objectid;
ALTER TABLE service_metrics CHANGE COLUMN objectid goid BINARY(16);
-- For manual runs use: set @metrics_prefix=createUnreservedPoorRandomPrefix();
SET @metrics_prefix=#RANDOM_LONG_NOT_RESERVED#;
UPDATE service_metrics SET goid = toGoid(@metrics_prefix,objectid_backup);
ALTER TABLE service_metrics DROP COLUMN objectid_backup;

ALTER TABLE service_metrics_details ADD COLUMN service_metrics_oid_backup BIGINT(20);
UPDATE service_metrics_details SET service_metrics_oid_backup=service_metrics_oid;
ALTER TABLE service_metrics_details CHANGE COLUMN service_metrics_oid service_metrics_goid BINARY(16);
UPDATE service_metrics_details SET service_metrics_goid = toGoid(@metrics_prefix,service_metrics_oid_backup);
ALTER TABLE service_metrics_details DROP COLUMN service_metrics_oid_backup;

ALTER TABLE service_metrics_details  ADD FOREIGN KEY (service_metrics_goid) REFERENCES service_metrics (goid) ON DELETE CASCADE;

update rbac_role set entity_goid = toGoid(@metrics_prefix,entity_oid) where entity_oid is not null and entity_type='METRICS_BIN';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = hex(toGoid(@metrics_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'METRICS_BIN';

-- Logon info
ALTER TABLE logon_info ADD COLUMN objectid_backup BIGINT(20);
update logon_info set objectid_backup=objectid;
ALTER TABLE logon_info CHANGE COLUMN objectid goid BINARY(16);
-- For manual runs use: set @logonInfo_prefix=createUnreservedPoorRandomPrefix();
SET @logonInfo_prefix=#RANDOM_LONG_NOT_RESERVED#;
UPDATE logon_info SET goid = toGoid(@logonInfo_prefix,objectid_backup);
ALTER TABLE logon_info DROP COLUMN objectid_backup;

-- SampleMessage
ALTER TABLE sample_messages ADD COLUMN objectid_backup BIGINT(20);
update sample_messages set objectid_backup=objectid;
ALTER TABLE sample_messages CHANGE COLUMN objectid goid BINARY(16);
-- For manual runs use: set @sample_messages_prefix=createUnreservedPoorRandomPrefix();
set @sample_messages_prefix=#RANDOM_LONG_NOT_RESERVED#;
update sample_messages set goid = toGoid(@sample_messages_prefix,objectid_backup);
ALTER TABLE sample_messages DROP COLUMN objectid_backup;

update rbac_role set entity_goid = toGoid(@sample_messages_prefix,entity_oid) where entity_oid is not null and entity_type='SAMPLE_MESSAGE';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = hex(toGoid(@sample_messages_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'SAMPLE_MESSAGE';

-- ClusterProperty
ALTER TABLE cluster_properties ADD COLUMN objectid_backup BIGINT(20);
update cluster_properties set objectid_backup=objectid;
ALTER TABLE cluster_properties CHANGE COLUMN objectid goid BINARY(16);
-- For manual runs use: set @cluster_properties_prefix=createUnreservedPoorRandomPrefix();
set @cluster_properties_prefix=#RANDOM_LONG_NOT_RESERVED#;
update cluster_properties set goid = toGoid(@cluster_properties_prefix,objectid_backup);
update cluster_properties set goid = toGoid(0,objectid_backup) where propkey = 'cluster.hostname';
update cluster_properties set goid = toGoid(0,objectid_backup) where propkey like 'upgrade.task.%';
ALTER TABLE cluster_properties DROP COLUMN objectid_backup;
update rbac_role set entity_goid = toGoid(@cluster_properties_prefix,entity_oid) where entity_oid is not null and entity_type='CLUSTER_PROPERTY';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = hex(toGoid(@cluster_properties_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'CLUSTER_PROPERTY';

-- EmailListener
ALTER TABLE email_listener ADD COLUMN objectid_backup BIGINT(20);
UPDATE email_listener SET objectid_backup=objectid;
ALTER TABLE email_listener CHANGE COLUMN objectid goid BINARY(16);
-- For manual runs use: set @email_prefix=createUnreservedPoorRandomPrefix();
SET @email_prefix=#RANDOM_LONG_NOT_RESERVED#;
UPDATE email_listener SET goid = toGoid(@email_prefix,objectid_backup);
ALTER TABLE email_listener DROP COLUMN objectid_backup;

ALTER TABLE email_listener_state ADD COLUMN email_listener_id_backup BIGINT(20);
UPDATE email_listener_state SET email_listener_id_backup=email_listener_id;
ALTER TABLE email_listener_state CHANGE COLUMN email_listener_id email_listener_goid BINARY(16);
UPDATE email_listener_state SET email_listener_goid = toGoid(@email_prefix,email_listener_id_backup);
ALTER TABLE email_listener_state DROP COLUMN email_listener_id_backup;

ALTER TABLE email_listener_state ADD COLUMN objectid_backup BIGINT(20);
UPDATE email_listener_state SET objectid_backup=objectid;
ALTER TABLE email_listener_state CHANGE COLUMN objectid goid BINARY(16);
-- For manual runs use: set @emailState_prefix=createUnreservedPoorRandomPrefix();
SET @emailState_prefix=#RANDOM_LONG_NOT_RESERVED#;
UPDATE email_listener_state SET goid = toGoid(@emailState_prefix,objectid_backup);
ALTER TABLE email_listener_state DROP COLUMN objectid_backup;
                
update rbac_role set entity_goid = toGoid(@email_prefix,entity_oid) where entity_oid is not null and entity_type='EMAIL_LISTENER';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = hex(toGoid(@email_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'EMAIL_LISTENER';

-- GenericEntity
ALTER TABLE generic_entity ADD COLUMN objectid_backup BIGINT(20);
update generic_entity set objectid_backup=objectid;
ALTER TABLE generic_entity CHANGE COLUMN objectid goid BINARY(16);
-- For manual runs use: set @generic_entity_prefix=createUnreservedPoorRandomPrefix();
set @generic_entity_prefix=#RANDOM_LONG_NOT_RESERVED#;
update generic_entity set goid = toGoid(@generic_entity_prefix,objectid_backup);
ALTER TABLE generic_entity DROP COLUMN objectid_backup;
update rbac_role set entity_goid = toGoid(@generic_entity_prefix,entity_oid) where entity_oid is not null and entity_type='GENERIC';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = hex(toGoid(@generic_entity_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'GENERIC';


-- Connector
call dropForeignKey('connector_property','connector');

ALTER TABLE connector ADD COLUMN objectid_backup BIGINT(20);
UPDATE connector SET objectid_backup=objectid;
ALTER TABLE connector CHANGE COLUMN objectid goid binary(16);
-- For manual runs use: set @connector_prefix=createUnreservedPoorRandomPrefix();
SET @connector_prefix=#RANDOM_LONG_NOT_RESERVED#;
UPDATE connector SET goid = toGoid(@connector_prefix,objectid_backup);
ALTER TABLE connector DROP COLUMN objectid_backup;

ALTER TABLE connector_property ADD COLUMN connector_oid_backup BIGINT(20);
UPDATE connector_property SET connector_oid_backup=connector_oid;
ALTER TABLE connector_property CHANGE COLUMN connector_oid connector_goid binary(16);
UPDATE connector_property SET connector_goid = toGoid(@connector_prefix,connector_oid_backup);
ALTER TABLE connector_property DROP COLUMN connector_oid_backup;
ALTER TABLE connector_property ADD FOREIGN KEY (connector_goid) REFERENCES connector (goid) ON DELETE CASCADE;

update rbac_role set entity_goid = toGoid(@connector_prefix,entity_oid) where entity_oid is not null and entity_type='SSG_CONNECTOR';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = hex(toGoid(@connector_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'SSG_CONNECTOR';

-- Firewall rule

call dropForeignKey('firewall_rule_property','firewall_rule');

ALTER TABLE firewall_rule ADD COLUMN objectid_backup BIGINT(20);
update firewall_rule set objectid_backup=objectid;
ALTER TABLE firewall_rule CHANGE COLUMN objectid goid binary(16);
-- For manual runs use: set @firewall_prefix=createUnreservedPoorRandomPrefix();
SET @firewall_prefix=#RANDOM_LONG_NOT_RESERVED#;
update firewall_rule set goid = toGoid(@firewall_prefix,objectid_backup);
ALTER TABLE firewall_rule DROP COLUMN objectid_backup;

ALTER TABLE firewall_rule_property ADD COLUMN firewall_rule_oid_backup BIGINT(20);
UPDATE  firewall_rule_property SET firewall_rule_oid_backup = firewall_rule_oid;
ALTER TABLE firewall_rule_property CHANGE COLUMN firewall_rule_oid firewall_rule_goid binary(16);
UPDATE firewall_rule_property SET firewall_rule_goid = toGoid(@firewall_prefix,firewall_rule_oid_backup);
ALTER TABLE firewall_rule_property DROP COLUMN firewall_rule_oid_backup;
ALTER TABLE firewall_rule_property  ADD FOREIGN KEY (firewall_rule_goid) REFERENCES firewall_rule (goid) ON DELETE CASCADE;

update rbac_role set entity_goid = toGoid(@firewall_prefix,entity_oid) where entity_oid is not null and entity_type='FIREWALL_RULE';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = hex(toGoid(@firewall_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'FIREWALL_RULE';

-- Encapsulated assertion

call dropForeignKey('encapsulated_assertion_property','encapsulated_assertion');
call dropForeignKey('encapsulated_assertion_argument','encapsulated_assertion');
call dropForeignKey('encapsulated_assertion_result','encapsulated_assertion');

ALTER TABLE encapsulated_assertion ADD COLUMN objectid_backup BIGINT(20);
update encapsulated_assertion set objectid_backup=objectid;
ALTER TABLE encapsulated_assertion CHANGE COLUMN objectid goid binary(16);
-- For manual runs use: set @encapsulated_assertion_prefix=createUnreservedPoorRandomPrefix();
SET @encapsulated_assertion_prefix=#RANDOM_LONG_NOT_RESERVED#;
update encapsulated_assertion set goid = toGoid(@encapsulated_assertion_prefix,objectid_backup);
ALTER TABLE encapsulated_assertion DROP COLUMN objectid_backup;

ALTER TABLE encapsulated_assertion_property ADD COLUMN encapsulated_assertion_oid_backup BIGINT(20);
UPDATE  encapsulated_assertion_property SET encapsulated_assertion_oid_backup = encapsulated_assertion_oid;
ALTER TABLE encapsulated_assertion_property CHANGE COLUMN encapsulated_assertion_oid encapsulated_assertion_goid binary(16);
UPDATE encapsulated_assertion_property SET encapsulated_assertion_goid = toGoid(@encapsulated_assertion_prefix,encapsulated_assertion_oid_backup);
ALTER TABLE encapsulated_assertion_property DROP COLUMN encapsulated_assertion_oid_backup;
ALTER TABLE encapsulated_assertion_property  ADD FOREIGN KEY (encapsulated_assertion_goid) REFERENCES encapsulated_assertion (goid) ON DELETE CASCADE;

ALTER TABLE encapsulated_assertion_argument ADD COLUMN objectid_backup BIGINT(20);
update encapsulated_assertion_argument set objectid_backup=objectid;
ALTER TABLE encapsulated_assertion_argument CHANGE COLUMN objectid goid binary(16);
-- For manual runs use: set @encapsulated_assertion_argument_prefix=createUnreservedPoorRandomPrefix();
SET @encapsulated_assertion_argument_prefix=#RANDOM_LONG_NOT_RESERVED#;
update encapsulated_assertion_argument set goid = toGoid(@encapsulated_assertion_argument_prefix,objectid_backup);
ALTER TABLE encapsulated_assertion_argument DROP COLUMN objectid_backup;

ALTER TABLE encapsulated_assertion_argument ADD COLUMN encapsulated_assertion_oid_backup BIGINT(20);
UPDATE  encapsulated_assertion_argument SET encapsulated_assertion_oid_backup = encapsulated_assertion_oid;
ALTER TABLE encapsulated_assertion_argument CHANGE COLUMN encapsulated_assertion_oid encapsulated_assertion_goid binary(16);
UPDATE encapsulated_assertion_argument SET encapsulated_assertion_goid = toGoid(@encapsulated_assertion_prefix,encapsulated_assertion_oid_backup);
ALTER TABLE encapsulated_assertion_argument DROP COLUMN encapsulated_assertion_oid_backup;
ALTER TABLE encapsulated_assertion_argument  ADD FOREIGN KEY (encapsulated_assertion_goid) REFERENCES encapsulated_assertion (goid) ON DELETE CASCADE;

ALTER TABLE encapsulated_assertion_result ADD COLUMN objectid_backup BIGINT(20);
update encapsulated_assertion_result set objectid_backup=objectid;
ALTER TABLE encapsulated_assertion_result CHANGE COLUMN objectid goid binary(16);
-- For manual runs use: set @encapsulated_assertion_result_prefix=createUnreservedPoorRandomPrefix();
SET @encapsulated_assertion_result_prefix=#RANDOM_LONG_NOT_RESERVED#;
update encapsulated_assertion_result set goid = toGoid(@encapsulated_assertion_result_prefix,objectid_backup);
ALTER TABLE encapsulated_assertion_result DROP COLUMN objectid_backup;

ALTER TABLE encapsulated_assertion_result ADD COLUMN encapsulated_assertion_oid_backup BIGINT(20);
UPDATE  encapsulated_assertion_result SET encapsulated_assertion_oid_backup = encapsulated_assertion_oid;
ALTER TABLE encapsulated_assertion_result CHANGE COLUMN encapsulated_assertion_oid encapsulated_assertion_goid binary(16);
UPDATE encapsulated_assertion_result SET encapsulated_assertion_goid = toGoid(@encapsulated_assertion_prefix,encapsulated_assertion_oid_backup);
ALTER TABLE encapsulated_assertion_result DROP COLUMN encapsulated_assertion_oid_backup;
ALTER TABLE encapsulated_assertion_result  ADD FOREIGN KEY (encapsulated_assertion_goid) REFERENCES encapsulated_assertion (goid) ON DELETE CASCADE;

update rbac_role set entity_goid = toGoid(@encapsulated_assertion_prefix,entity_oid) where entity_oid is not null and entity_type='ENCAPSULATED_ASSERTION';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = hex(toGoid(@encapsulated_assertion_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'ENCAPSULATED_ASSERTION';

-- jms

ALTER TABLE jms_endpoint ADD COLUMN old_objectid BIGINT(20);
update jms_endpoint set old_objectid=objectid;
ALTER TABLE jms_endpoint CHANGE COLUMN objectid goid binary(16);
-- For manual runs use: set @jms_endpoint_prefix=createUnreservedPoorRandomPrefix();
SET @jms_endpoint_prefix=#RANDOM_LONG_NOT_RESERVED#;
update jms_endpoint set goid = toGoid(@jms_endpoint_prefix,old_objectid);

ALTER TABLE jms_connection ADD COLUMN objectid_backup BIGINT(20);
update jms_connection set objectid_backup=objectid;
ALTER TABLE jms_connection CHANGE COLUMN objectid goid binary(16);
-- For manual runs use: set @jms_connection_prefix=createUnreservedPoorRandomPrefix();
SET @jms_connection_prefix=#RANDOM_LONG_NOT_RESERVED#;
update jms_connection set goid = toGoid(@jms_connection_prefix,objectid_backup);
ALTER TABLE jms_connection DROP COLUMN objectid_backup;

ALTER TABLE jms_endpoint ADD COLUMN connection_goid binary(16);
update jms_endpoint set connection_goid = toGoid(@jms_connection_prefix,connection_oid);
ALTER TABLE jms_endpoint DROP COLUMN connection_oid;

update rbac_role set entity_goid = toGoid(@jms_connection_prefix,entity_oid) where entity_oid is not null and entity_type='JMS_CONNECTION';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = hex(toGoid(@jms_connection_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'JMS_CONNECTION';

update rbac_role set entity_goid = toGoid(@jms_endpoint_prefix,entity_oid) where entity_oid is not null and entity_type='JMS_ENDPOINT';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = hex(toGoid(@jms_endpoint_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'JMS_ENDPOINT';


-- active connector

call dropForeignKey('active_connector_property','active_connector');

ALTER TABLE active_connector ADD COLUMN old_objectid BIGINT(20);
update active_connector set old_objectid=objectid;
ALTER TABLE active_connector CHANGE COLUMN objectid goid binary(16);
-- For manual runs use: set @active_connector_prefix=createUnreservedPoorRandomPrefix();
SET @active_connector_prefix=#RANDOM_LONG_NOT_RESERVED#;
update active_connector set goid = toGoid(@active_connector_prefix,old_objectid);

ALTER TABLE active_connector_property ADD COLUMN connector_oid_backup BIGINT(20);
UPDATE  active_connector_property SET connector_oid_backup = connector_oid;
ALTER TABLE active_connector_property CHANGE COLUMN connector_oid connector_goid binary(16);
UPDATE active_connector_property SET connector_goid = toGoid(@active_connector_prefix,connector_oid_backup);
ALTER TABLE active_connector_property DROP COLUMN connector_oid_backup;
ALTER TABLE active_connector_property  ADD FOREIGN KEY (connector_goid) REFERENCES active_connector (goid) ON DELETE CASCADE;

update rbac_role set entity_goid = toGoid(@active_connector_prefix,entity_oid) where entity_oid is not null and entity_type='SSG_ACTIVE_CONNECTOR';
update rbac_predicate_oid oid1 left join rbac_predicate on rbac_predicate.objectid = oid1.objectid left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid set oid1.entity_id = hex(toGoid(@active_connector_prefix,oid1.entity_id)) where rbac_permission.entity_type = 'SSG_ACTIVE_CONNECTOR';

--
-- Register upgrade task for upgrading sink configuration references to GOIDs
--
INSERT INTO cluster_properties
    (goid, version, propkey, propvalue, properties)
    values (toGoid(0,-800001), 0, 'upgrade.task.800001', 'com.l7tech.server.upgrade.Upgrade71To80SinkConfig', null);


--
-- License documents for updated licensing model
--

CREATE TABLE license_document (
  objectid bigint(20) NOT NULL,
  contents mediumtext,
  PRIMARY KEY (objectid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--

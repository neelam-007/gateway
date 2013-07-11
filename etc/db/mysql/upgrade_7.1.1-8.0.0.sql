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

--
-- Security Zones
--
CREATE TABLE security_zone (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  description varchar(255) NOT NULL,
  entity_types varchar(4096) NOT NULL,
  PRIMARY KEY (objectid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

CREATE TABLE rbac_predicate_security_zone (
  objectid bigint(20) NOT NULL,
  security_zone_oid bigint(20),
  PRIMARY KEY (objectid),
  FOREIGN KEY (objectid) REFERENCES rbac_predicate (objectid) ON DELETE CASCADE,
  FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

CREATE TABLE assertion_access (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  name varchar(255) NOT NULL,
  security_zone_oid bigint(20),
  PRIMARY KEY (objectid),
  UNIQUE KEY i_name (name),
  CONSTRAINT assertion_access_security_zone FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

alter table policy add column security_zone_oid bigint(20);
alter table policy add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table policy_alias add column security_zone_oid bigint(20);
alter table policy_alias add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table published_service add column security_zone_oid bigint(20);
alter table published_service add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table published_service_alias add column security_zone_oid bigint(20);
alter table published_service_alias add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table folder add column security_zone_oid bigint(20);
alter table folder add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table identity_provider add column security_zone_oid bigint(20);
alter table identity_provider add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table jdbc_connection add column security_zone_oid bigint(20);
alter table jdbc_connection add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table trusted_cert add column security_zone_oid bigint(20);
alter table trusted_cert add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table sink_config add column security_zone_oid bigint(20);
alter table sink_config add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table secure_password add column security_zone_oid bigint(20);
alter table secure_password add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table email_listener add column security_zone_oid bigint(20);
alter table email_listener add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table resource_entry add column security_zone_oid bigint(20);
alter table resource_entry add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table http_configuration add column security_zone_oid bigint(20);
alter table http_configuration add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table connector add column security_zone_oid bigint(20);
alter table connector add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table encapsulated_assertion add column security_zone_oid bigint(20);
alter table encapsulated_assertion add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table active_connector add column security_zone_oid bigint(20);
alter table active_connector add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table revocation_check_policy add column security_zone_oid bigint(20);
alter table revocation_check_policy add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table uddi_registries add column security_zone_oid bigint(20);
alter table uddi_registries add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;
alter table uddi_proxied_service_info add column security_zone_oid bigint(20);
alter table uddi_proxied_service_info add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;
alter table uddi_service_control add column security_zone_oid bigint(20);
alter table uddi_service_control add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table sample_messages add column security_zone_oid bigint(20);
alter table sample_messages add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table jms_endpoint add column security_zone_oid bigint(20);
alter table jms_endpoint add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;
alter table jms_connection add column security_zone_oid bigint(20);
alter table jms_connection add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;
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
  security_zone_oid bigint(20),
  PRIMARY KEY (objectid),
  UNIQUE KEY i_ks_alias (keystore_file_oid, alias),
  CONSTRAINT keystore_key_metadata_keystore_file FOREIGN KEY (keystore_file_oid) REFERENCES keystore_file (objectid) ON DELETE CASCADE,
  CONSTRAINT keystore_key_metadata_security_zone FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Register upgrade task for adding Assertion Access to auto-created "Manage <Blah>" roles
--
INSERT INTO cluster_properties
    (objectid, version, propkey, propvalue, properties)
    values (-800000, 0, 'upgrade.task.800000', 'com.l7tech.server.upgrade.Upgrade71To80UpdateRoles', null);


--
-- Goidification modification. These involve replacing the oid column with a goid column on entity tables.
--

-- JdbcConnection
ALTER TABLE jdbc_connection ADD COLUMN objectid_backup BIGINT(20);
update jdbc_connection set objectid_backup=objectid;
ALTER TABLE jdbc_connection CHANGE COLUMN objectid goid VARBINARY(16);
-- For manual runs use: set @jdbc_prefix=concat(lpad(char(floor(rand()*4294967296)),4,'\0'),lpad(char(floor(rand()*4294967296)),4,'\0'));
set @jdbc_prefix=concat(lpad(char(#RANDOM_LONG#),4,'\0'),lpad(char(#RANDOM_LONG#),4,'\0'));
update jdbc_connection set goid = concat(@jdbc_prefix,lpad(char(objectid_backup),8,'\0'));
ALTER TABLE jdbc_connection DROP COLUMN objectid_backup;

-- MetricsBin, MetricsBinDetail
ALTER TABLE service_metrics_details DROP FOREIGN KEY service_metrics_details_ibfk_1;

ALTER TABLE service_metrics ADD COLUMN objectid_backup BIGINT(20);
UPDATE service_metrics SET objectid_backup=objectid;
ALTER TABLE service_metrics CHANGE COLUMN objectid goid VARBINARY(16);
-- For manual runs use: set @metrics_prefix=concat(lpad(char(floor(rand()*4294967296)),4,'\0'),lpad(char(floor(rand()*4294967296)),4,'\0'));
SET @metrics_prefix=concat(lpad(char(#RANDOM_LONG#),4,'\0'),lpad(char(#RANDOM_LONG#),4,'\0'));
UPDATE service_metrics SET goid = concat(@metrics_prefix,lpad(char(objectid_backup),8,'\0'));
ALTER TABLE service_metrics DROP COLUMN objectid_backup;

ALTER TABLE service_metrics_details ADD COLUMN service_metrics_oid_backup BIGINT(20);
UPDATE service_metrics_details SET service_metrics_oid_backup=service_metrics_oid;
ALTER TABLE service_metrics_details CHANGE COLUMN service_metrics_oid service_metrics_goid VARBINARY(16);
UPDATE service_metrics_details SET service_metrics_goid = concat(@metrics_prefix,lpad(char(service_metrics_oid_backup),8,'\0'));
ALTER TABLE service_metrics_details DROP COLUMN service_metrics_oid_backup;

ALTER TABLE service_metrics_details  ADD FOREIGN KEY (service_metrics_goid) REFERENCES service_metrics (goid) ON DELETE CASCADE;

-- Logon info
ALTER TABLE logon_info ADD COLUMN objectid_backup BIGINT(20);
update logon_info set objectid_backup=objectid;
ALTER TABLE logon_info CHANGE COLUMN objectid goid VARBINARY(16);
-- For manual runs use: set @logonInfo_prefix=concat(lpad(char(floor(rand()*4294967296)),4,'\0'),lpad(char(floor(rand()*4294967296)),4,'\0'));
SET @logonInfo_prefix=concat(lpad(char(#RANDOM_LONG#),4,'\0'),lpad(char(#RANDOM_LONG#),4,'\0'));
UPDATE logon_info SET goid = concat(@logonInfo_prefix,lpad(char(objectid_backup),8,'\0'));
ALTER TABLE logon_info DROP COLUMN objectid_backup;

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--

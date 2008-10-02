---
--- Script to update mysql ssg database from 4.6 to 4.7
---
--- Layer 7 Technologies, inc
---

--
-- Disable FK while manipulating tables
--
SET FOREIGN_KEY_CHECKS=0;

BEGIN;
ALTER TABLE rbac_assignment ADD CONSTRAINT `identity_provider_ibfk_1` FOREIGN KEY (`provider_oid`) REFERENCES `identity_provider` (`objectid`) ON DELETE CASCADE;
COMMIT;

--
-- Rename audit detail column to avoid reserved word
--

ALTER TABLE audit_detail CHANGE exception exception_message MEDIUMTEXT;

--
-- Add description to POLICY roles
--
UPDATE rbac_role set description='Users assigned to the {0} role have the ability to read, update and delete the {1} policy.' where entity_type='POLICY' and entity_oid IS NOT NULL and description is NULL;

--
-- Update rbac_assignments with new entity_type columns and user_id renamed to identity_id
--
ALTER TABLE rbac_assignment ADD COLUMN entity_type varchar(50) NOT NULL;
UPDATE rbac_assignment SET entity_type = 'User';
ALTER TABLE rbac_assignment CHANGE user_id identity_id varchar(255) NOT NULL;
ALTER TABLE rbac_assignment DROP KEY unique_assignment;
ALTER TABLE rbac_assignment ADD CONSTRAINT UNIQUE KEY unique_assignment (provider_oid,role_oid,identity_id, entity_type);

--
-- Constraint changes for trusted_cert:
--    name is no longer unique
--    subject_dn is no longer unique
--    thumbprint_sha1 is now unique
--
drop index subject_dn on trusted_cert;
create index i_subject_dn on trusted_cert (subject_dn);
drop index i_thumb on trusted_cert;
create unique index i_thumb on trusted_cert (thumbprint_sha1);
drop index name on trusted_cert;

--
-- Table structure for message_context_mapping_keys
--
DROP TABLE IF EXISTS message_context_mapping_keys;
CREATE TABLE message_context_mapping_keys (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  digested char(36) NOT NULL,
  mapping1_type varchar(36),
  mapping1_key varchar(128),
  mapping2_type varchar(36),
  mapping2_key varchar(128),
  mapping3_type varchar(36),
  mapping3_key varchar(128),
  mapping4_type varchar(36),
  mapping4_key varchar(128),
  mapping5_type varchar(36),
  mapping5_key varchar(128),
  create_time bigint(20),
  PRIMARY KEY (objectid),
  INDEX (digested)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for message_context_mapping_values
--
DROP TABLE IF EXISTS message_context_mapping_values;
CREATE TABLE message_context_mapping_values (
  objectid bigint(20) NOT NULL,
  digested char(36) NOT NULL,
  mapping_keys_oid bigint(20) NOT NULL,
  auth_user_provider_id bigint(20),
  auth_user_id varchar(255),
  auth_user_description varchar(255),
  service_operation varchar(255),
  mapping1_value varchar(255),
  mapping2_value varchar(255),
  mapping3_value varchar(255),
  mapping4_value varchar(255),
  mapping5_value varchar(255),
  create_time bigint(20),
  PRIMARY KEY  (objectid),
  FOREIGN KEY (mapping_keys_oid) REFERENCES message_context_mapping_keys (objectid),
  INDEX (digested)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Update service_metrics for context mapping changes
--
ALTER TABLE service_metrics DROP PRIMARY KEY;
ALTER TABLE service_metrics ADD COLUMN objectid bigint(20) NOT NULL, MODIFY COLUMN front_min INTEGER, MODIFY COLUMN front_max INTEGER, MODIFY COLUMN back_min INTEGER, MODIFY COLUMN back_max INTEGER;
SET @rowid = 100000;
UPDATE service_metrics set objectid = @rowid := @rowid + 1;
UPDATE service_metrics set front_min = NULL, front_max = NULL where resolution = 0 and attempted = 0;
UPDATE service_metrics set back_min = NULL, back_max = NULL where resolution = 0 and completed = 0;
ALTER TABLE service_metrics ADD CONSTRAINT PRIMARY KEY (objectid);

DROP TABLE IF EXISTS service_metrics_details;
CREATE TABLE service_metrics_details (
  service_metrics_oid BIGINT(20) NOT NULL,
  mapping_values_oid BIGINT(20) NOT NULL,
  attempted INTEGER NOT NULL,
  authorized INTEGER NOT NULL,
  completed INTEGER NOT NULL,
  back_min INTEGER,
  back_max INTEGER,
  back_sum INTEGER NOT NULL,
  front_min INTEGER,
  front_max INTEGER,
  front_sum INTEGER NOT NULL,
  PRIMARY KEY (service_metrics_oid, mapping_values_oid),
  FOREIGN KEY (service_metrics_oid) REFERENCES service_metrics (objectid) ON DELETE CASCADE,
  FOREIGN KEY (mapping_values_oid) REFERENCES message_context_mapping_values (objectid)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Add a new column, mapping_values_oid into audit_message
--
ALTER TABLE audit_message ADD COLUMN mapping_values_oid BIGINT(20);
ALTER TABLE audit_message ADD CONSTRAINT FOREIGN KEY (mapping_values_oid) REFERENCES message_context_mapping_values (objectid);

--
-- Folder changes
--
--INSERT INTO folder (objectid, name, parent_folder_oid) VALUES (-5002, 'Root Node', NULL);
UPDATE folder SET parent_folder_oid = -5002 WHERE objectid = -5001;
UPDATE folder SET parent_folder_oid = -5002 WHERE objectid = -5000;

--
-- Table structure for table 'published_service_alias'
--
CREATE TABLE published_service_alias (
  `objectid` bigint(20) NOT NULL,
  `version` int(11) NOT NULL,
  `folder_oid` bigint(20) NOT NULL,
  `published_service_oid` bigint(20) NOT NULL,
  UNIQUE KEY (folder_oid, published_service_oid),
  FOREIGN KEY (published_service_oid) REFERENCES published_service (objectid) ON DELETE CASCADE,
  FOREIGN KEY (folder_oid) REFERENCES folder (objectid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

---
--- All nodes are now assumed always to have access to the CA key
---
ALTER TABLE cluster_info DROP COLUMN ismaster, DROP COLUMN partition_name, DROP COLUMN cluster_port;

--
-- Table structure for table 'policy_alias'
--
CREATE TABLE policy_alias (
  `objectid` bigint(20) NOT NULL,
  `version` int(11) NOT NULL,
  `folder_oid` bigint(20) NOT NULL,
  `policy_oid` bigint(20) NOT NULL,
  UNIQUE KEY (folder_oid, policy_oid),
  FOREIGN KEY (policy_oid) REFERENCES policy (objectid) ON DELETE CASCADE,
  FOREIGN KEY (folder_oid) REFERENCES folder (objectid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

---
--- Now Throughput Quota Assertion allows a max value per minute.
---
ALTER TABLE counters ADD COLUMN cnt_min bigint(20) default 0;


--
-- Global schemas names must be unique
--
ALTER TABLE community_schemas ADD CONSTRAINT UNIQUE KEY csnm_idx (name);

---
--- Add description cols for fed and internal users.
---
ALTER TABLE internal_user ADD COLUMN description varchar(255) default NULL;


---
--- Add tables for EMS trust and user mapping support
---

DROP TABLE IF EXISTS trusted_ems;
CREATE TABLE trusted_ems (
  objectid bigint NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  trusted_cert_oid bigint(20) NOT NULL,
  primary key(objectid),
  FOREIGN KEY (trusted_cert_oid) REFERENCES trusted_cert (objectid) ON DELETE CASCADE
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS trusted_ems_user;
CREATE TABLE trusted_ems_user (
  objectid bigint NOT NULL,
  version integer NOT NULL,
  trusted_ems_oid bigint(20) NOT NULL,
  provider_oid bigint(20) NOT NULL,
  user_id varchar(128) NOT NULL,
  ems_user_id varchar(128) NOT NULL,
  PRIMARY KEY(objectid),
  FOREIGN KEY (trusted_ems_oid) REFERENCES trusted_ems (objectid) ON DELETE CASCADE,
  FOREIGN KEY (provider_oid) REFERENCES identity_provider (objectid) ON DELETE CASCADE
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;





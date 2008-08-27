---
--- Script to update mysql ssg database from 4.6 to 5.0
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
-- Folder changes
--
INSERT INTO folder (objectid, name, parent_folder_oid) VALUES (-5002, 'Root Node', NULL);
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


--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

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
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

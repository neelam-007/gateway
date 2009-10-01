---
--- Script to update mysql ssg database from 5.1.0 to 5.2.0
---
--- Layer 7 Technologies, inc
---

--
-- Disable FK while manipulating tables
--
SET FOREIGN_KEY_CHECKS=0;

--
-- Update the version
--
UPDATE ssg_version SET current_version = '5.2.0';

--
-- Convert DN columns to allow larger values
--
ALTER TABLE client_cert MODIFY COLUMN subject_dn VARCHAR(500), MODIFY COLUMN issuer_dn VARCHAR(500);
ALTER TABLE trusted_cert MODIFY COLUMN subject_dn VARCHAR(500) NOT NULL, MODIFY COLUMN issuer_dn VARCHAR(500) NOT NULL;

--
-- Upgrade task for DN canonicalization
--
INSERT INTO cluster_properties
    (objectid, version, propkey, propvalue)
    values (-500200, 0, "upgrade.task.500200", "com.l7tech.server.upgrade.Upgrade51To52CanonicalizeDNs");

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--

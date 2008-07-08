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
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

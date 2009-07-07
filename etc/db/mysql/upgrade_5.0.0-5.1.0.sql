---
--- Script to update mysql ssg database from 4.6.5 to 5.0
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
UPDATE ssg_version SET current_version = '5.1.0';

--
-- Insert Luna placeholder row
--
insert into keystore_file values (3, 0, "SafeNet HSM", "luna", null, null);

--
-- Add column for keeping track of subscription endpoint reference parameters
--
ALTER TABLE wsdm_subscription ADD COLUMN reference_parameters MEDIUMTEXT;

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--

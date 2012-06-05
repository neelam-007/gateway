--
-- Script to update mysql ssg database from 6.2.0 to 6.3.0
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
UPDATE ssg_version SET current_version = '7.0.0';

-- External audits enhancements
ALTER TABLE audit_message DROP FOREIGN KEY message_context_mapping;
ALTER TABLE audit_message DROP COLUMN mapping_values_oid ;

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--

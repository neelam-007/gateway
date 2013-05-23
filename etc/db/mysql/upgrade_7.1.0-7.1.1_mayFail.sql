--
-- Script to update mysql ssg database from 7.1.0 to 7.1.1
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
UPDATE ssg_version SET current_version = '7.1.1';


--
-- Restore message audit link to mapping values
--

ALTER TABLE audit_message ADD COLUMN mapping_values_oid bigint(20) ;
ALTER TABLE audit_message ADD CONSTRAINT message_context_mapping FOREIGN KEY (mapping_values_oid) REFERENCES message_context_mapping_values (objectid);


--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--

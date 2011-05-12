--
-- Script to update mysql ssg database from 6.0.0 to 6.1.0
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
UPDATE ssg_version SET current_version = '6.1.0';

--
-- Cluster info updated to support ESM IP
--
ALTER TABLE cluster_info ADD COLUMN esm_address varchar(39) NOT NULL DEFAULT '' AFTER address;
UPDATE cluster_info set esm_address = address;
ALTER TABLE cluster_info ALTER COLUMN esm_address DROP DEFAULT;

--
--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--


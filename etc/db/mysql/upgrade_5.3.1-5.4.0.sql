--
-- Script to update mysql ssg database from 5.3.1 to 5.4.0
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

UPDATE ssg_version SET current_version = '5.4.0';

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- bug 9071  allow larger JDBC URL
--
ALTER TABLE jdbc_connection MODIFY COLUMN jdbc_url varchar(4096) ;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--


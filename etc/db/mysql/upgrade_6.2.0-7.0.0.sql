--
-- Script to update mysql ssg database from 6.2.0 to 7.0.0
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

--
-- Add the SSPC log/sink entry
--
INSERT INTO sink_config VALUES (-811,0,'sspc','Process Controller Log','FILE',0,'FINEST','SSPC','<java version="1.6.0" class="java.beans.XMLDecoder"><object class="java.util.HashMap"><void method="put"><string>file.maxSize</string><string>20000</string></void><void method="put"><string>file.format</string><string>STANDARD</string></void><void method="put"><string>file.logCount</string><string>10</string></void></object></java>');


-- RBAC enhancements
INSERT INTO rbac_permission VALUES (-440,0,-400,'CREATE',NULL,'POLICY');
ALTER TABLE rbac_predicate_attribute ADD COLUMN mode varchar(255);
ALTER TABLE rbac_role ADD COLUMN user_created tinyint(1) NOT NULL default 0;

--
-- Register upgrade task to canonicalize federated user subject DNs
--
INSERT INTO cluster_properties
    (objectid, version, propkey, propvalue)
    values (-700000, 0, "upgrade.task.700000", "com.l7tech.server.upgrade.Upgrade62To70CanonicalizeFedUserSubjectDNs");

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--

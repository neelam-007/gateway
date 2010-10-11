--
-- Script to update mysql ssg database from 5.3.0 to 5.3.1
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

UPDATE ssg_version SET current_version = '5.3.1';


--
-- bug 8147
--
ALTER TABLE uddi_proxied_service_info ADD COLUMN properties mediumtext;

--
-- In 5.3 only the hostname of the publishing cluster / gateway was persisted. This was then used to find and delete any
-- gateway endpoints in an original service in UDDI. In 5.3.1 we track the full url, for all urls published. This statement
-- populates the new properties field and inserts the value from 'published_hostname' into the list of published URLs.
-- the protocol info in this update statement (http://) is not used and is just to satisfy the constrains inside EndpointPair
--
UPDATE uddi_proxied_service_info SET properties = CONCAT('<?xml version="1.0" encoding="UTF-8"?><java version="1.6.0_05" class="java.beans.XMLDecoder"><object class="java.util.HashMap"><void method="put"><string>ALL_ENDPOINT_PAIRS_KEY</string><object class="java.util.HashSet"><void method="add"><object class="com.l7tech.uddi.EndpointPair"><void property="endPointUrl"><string>http://', published_hostname ,'</string></void></object></void></object></void></object></java>');

ALTER TABLE uddi_proxied_service_info DROP published_hostname;


-- serialized NcipherKeyStoreData for an nCipher keystore
insert into keystore_file values (4, 0, "nCipher HSM", "hsm.NcipherKeyStoreData", null, null);

-- Add permission to read published services to the default "Manage Listen Port" role.
UPDATE rbac_role SET DESCRIPTION='Users assigned to the {0} role have the ability to read, create, update and delete Gateway listen ports (HTTP(S) and FTP(S)) and to list published services.' WHERE objectid=-750;
INSERT INTO rbac_permission VALUES (-755,0,-750,'READ',NULL,'SERVICE');

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--

--
-- Script to update mysql ssg database from 5.4.1 to 6.0.0
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

UPDATE ssg_version SET current_version = '6.0.0';

ALTER TABLE jms_endpoint ADD COLUMN request_max_size bigint NOT NULL default -1 AFTER use_message_id_for_correlation;

-- Per-host outbound TLS cipher suite configuration (enhancement #9939)
ALTER TABLE http_configuration ADD COLUMN tls_cipher_suites varchar(4096) DEFAULT NULL;


--
-- password policy changes
--

DROP TABLE IF EXISTS password_policy;
CREATE TABLE password_policy (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  properties mediumtext,
  internal_identity_provider_oid bigint(20),
  PRIMARY KEY (objectid),
  UNIQUE KEY  (internal_identity_provider_oid),
  FOREIGN KEY (internal_identity_provider_oid) REFERENCES identity_provider (objectid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;
INSERT INTO password_policy (objectid, version, properties, internal_identity_provider_oid) VALUES (-2, 0, '<?xml version="1.0" encoding="UTF-8"?><java version="1.6.0_21" class="java.beans.XMLDecoder"> <object class="java.util.TreeMap">  <void method="put">   <string>allowableChangesPerDay</string>   <boolean>true</boolean>  </void>  <void method="put">   <string>charDiffMinimum</string>   <int>4</int>  </void>  <void method="put">   <string>forcePasswordChangeNewUser</string>   <boolean>true</boolean>  </void>  <void method="put">   <string>lowerMinimum</string>   <int>1</int>  </void>  <void method="put">   <string>maxPasswordLength</string>   <int>32</int>  </void>  <void method="put">   <string>minPasswordLength</string>   <int>8</int>  </void>  <void method="put">   <string>noRepeatingCharacters</string>   <boolean>true</boolean>  </void>  <void method="put">   <string>numberMinimum</string>   <int>1</int>  </void>  <void method="put">   <string>passwordExpiry</string>   <int>90</int>  </void>  <void method="put">   <string>repeatFrequency</string>   <int>10</int>  </void>  <void method="put">   <string>symbolMinimum</string>   <int>1</int>  </void>  <void method="put">   <string>upperMinimum</string>   <int>1</int>  </void> </object></java>', -2);

INSERT INTO rbac_role VALUES (-1150,0,'Manage Password Policies', null,null,null, 'Users assigned to the {0} role have the ability to read and update any stored password policy.');
INSERT INTO rbac_permission VALUES (-1151,0,-1150,'READ',NULL,'PASSWORD_POLICY');
-- INSERT INTO rbac_permission VALUES (-1052,0,-1050,'CREATE',NULL,'PASSWORD_POLICY');
INSERT INTO rbac_permission VALUES (-1153,0,-1150,'UPDATE',NULL,'PASSWORD_POLICY');
-- INSERT INTO rbac_permission VALUES (-1054,0,-1050,'DELETE',NULL,'PASSWORD_POLICY');


--
--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--


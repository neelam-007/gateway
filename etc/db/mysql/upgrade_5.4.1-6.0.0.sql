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


--
-- password policy role
--

INSERT INTO rbac_role VALUES (-1150,0,'Manage Password Policies', null,null,null, 'Users assigned to the {0} role have the ability to read and update any stored password policy.');
INSERT INTO rbac_permission VALUES (-1151,0,-1150,'READ',NULL,'PASSWORD_POLICY');
-- INSERT INTO rbac_permission VALUES (-1052,0,-1050,'CREATE',NULL,'PASSWORD_POLICY');
INSERT INTO rbac_permission VALUES (-1153,0,-1150,'UPDATE',NULL,'PASSWORD_POLICY');
-- INSERT INTO rbac_permission VALUES (-1054,0,-1050,'DELETE',NULL,'PASSWORD_POLICY');

--
-- New role to invoke the audit viewer policy. Requires READ on audits to be able to open the audit viewer.
--
INSERT INTO rbac_role VALUES (-1200,0,'Invoke Audit Viewer Policy', null,null,null, 'Allow the INTERNAL audit-viewer policy to be invoked for an audited message (request / response or detail)');
INSERT INTO rbac_permission VALUES (-1201,0,-1200,'OTHER','audit-viewer policy', 'AUDIT_RECORD');
INSERT INTO rbac_permission VALUES (-1202,0,-1200,'READ',NULL,'AUDIT_RECORD');

--
-- logon info changes
--
ALTER TABLE logon_info ADD COLUMN last_activity bigint(20) NOT NULL;
ALTER TABLE logon_info ADD COLUMN state int(11) NOT NULL DEFAULT 0;


--
-- internal user changes
--
ALTER TABLE internal_user ADD COLUMN enabled boolean DEFAULT TRUE;


--
-- manage admin accounts config role
--
INSERT INTO rbac_role VALUES (-1200,0,'Manage Administrative Accounts Configuration', null,null,null, 'Users assigned to the {0} role have the ability edit administrative accounts configurations.');
INSERT INTO rbac_permission VALUES (-1201,0,-1200,'READ',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1202,0,-1201);
INSERT INTO rbac_predicate_attribute VALUES (-1202,'name','logon.maxAllowableAttempts');
INSERT INTO rbac_permission VALUES (-1203,0,-1200,'READ',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1204,0,-1203);
INSERT INTO rbac_predicate_attribute VALUES (-1204,'name','logon.lockoutTime');
INSERT INTO rbac_permission VALUES (-1205,0,-1200,'READ',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1206,0,-1205);
INSERT INTO rbac_predicate_attribute VALUES (-1206,'name','logon.sessionExpiry');
INSERT INTO rbac_permission VALUES (-1207,0,-1200,'READ',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1208,0,-1207);
INSERT INTO rbac_predicate_attribute VALUES (-1208,'name','logon.inactivityPeriod');
INSERT INTO rbac_permission VALUES (-1209,0,-1200,'UPDATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1210,0,-1209);
INSERT INTO rbac_predicate_attribute VALUES (-1210,'name','logon.maxAllowableAttempts');
INSERT INTO rbac_permission VALUES (-1211,0,-1200,'UPDATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1212,0,-1211);
INSERT INTO rbac_predicate_attribute VALUES (-1212,'name','logon.lockoutTime');
INSERT INTO rbac_permission VALUES (-1213,0,-1200,'UPDATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1214,0,-1213);
INSERT INTO rbac_predicate_attribute VALUES (-1214,'name','logon.sessionExpiry');
INSERT INTO rbac_permission VALUES (-1215,0,-1200,'UPDATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1216,0,-1215);
INSERT INTO rbac_predicate_attribute VALUES (-1216,'name','logon.inactivityPeriod');
INSERT INTO rbac_permission VALUES (-1217,0,-1200,'CREATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1218,0,-1217);
INSERT INTO rbac_predicate_attribute VALUES (-1218,'name','logon.maxAllowableAttempts');
INSERT INTO rbac_permission VALUES (-1219,0,-1200,'CREATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1220,0,-1219);
INSERT INTO rbac_predicate_attribute VALUES (-1220,'name','logon.lockoutTime');
INSERT INTO rbac_permission VALUES (-1221,0,-1200,'CREATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1222,0,-1221);
INSERT INTO rbac_predicate_attribute VALUES (-1222,'name','logon.sessionExpiry');
INSERT INTO rbac_permission VALUES (-1223,0,-1200,'CREATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1224,0,-1223);
INSERT INTO rbac_predicate_attribute VALUES (-1224,'name','logon.inactivityPeriod');


--
--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--


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

INSERT INTO rbac_role VALUES (-1150,0,'Manage Password Policies', null,null,null, 'Users assigned to the {0} role have the ability to read and update any stored password policy and view the identity providers.');
INSERT INTO rbac_permission VALUES (-1151,0,-1150,'READ',NULL,'PASSWORD_POLICY');
-- INSERT INTO rbac_permission VALUES (-1052,0,-1050,'CREATE',NULL,'PASSWORD_POLICY');
INSERT INTO rbac_permission VALUES (-1153,0,-1150,'UPDATE',NULL,'PASSWORD_POLICY');
-- INSERT INTO rbac_permission VALUES (-1054,0,-1050,'DELETE',NULL,'PASSWORD_POLICY');
INSERT INTO rbac_permission VALUES (-1155,0,-1150,'READ',NULL,'ID_PROVIDER_CONFIG');

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
ALTER TABLE logon_info ADD COLUMN state varchar(32) DEFAULT 'ACTIVE';


--
-- internal user changes
--
ALTER TABLE internal_user ADD COLUMN enabled boolean DEFAULT TRUE;


--
-- manage admin accounts config role
--
INSERT INTO rbac_role VALUES (-1250,0,'Manage Administrative Accounts Configuration', null,null,null, 'Users assigned to the {0} role have the ability edit administrative accounts configurations.');
INSERT INTO rbac_permission VALUES (-1251,0,-1250,'READ',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1252,0,-1251);
INSERT INTO rbac_predicate_attribute VALUES (-1252,'name','logon.maxAllowableAttempts');
INSERT INTO rbac_permission VALUES (-1253,0,-1250,'READ',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1254,0,-1253);
INSERT INTO rbac_predicate_attribute VALUES (-1254,'name','logon.lockoutTime');
INSERT INTO rbac_permission VALUES (-1255,0,-1250,'READ',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1256,0,-1255);
INSERT INTO rbac_predicate_attribute VALUES (-1256,'name','logon.sessionExpiry');
INSERT INTO rbac_permission VALUES (-1257,0,-1250,'READ',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1258,0,-1257);
INSERT INTO rbac_predicate_attribute VALUES (-1258,'name','logon.inactivityPeriod');
INSERT INTO rbac_permission VALUES (-1259,0,-1250,'UPDATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1260,0,-1259);
INSERT INTO rbac_predicate_attribute VALUES (-1260,'name','logon.maxAllowableAttempts');
INSERT INTO rbac_permission VALUES (-1261,0,-1250,'UPDATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1262,0,-1261);
INSERT INTO rbac_predicate_attribute VALUES (-1262,'name','logon.lockoutTime');
INSERT INTO rbac_permission VALUES (-1263,0,-1250,'UPDATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1264,0,-1263);
INSERT INTO rbac_predicate_attribute VALUES (-1264,'name','logon.sessionExpiry');
INSERT INTO rbac_permission VALUES (-1265,0,-1250,'UPDATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1266,0,-1265);
INSERT INTO rbac_predicate_attribute VALUES (-1266,'name','logon.inactivityPeriod');
INSERT INTO rbac_permission VALUES (-1267,0,-1250,'CREATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1268,0,-1267);
INSERT INTO rbac_predicate_attribute VALUES (-1268,'name','logon.maxAllowableAttempts');
INSERT INTO rbac_permission VALUES (-1269,0,-1250,'CREATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1270,0,-1269);
INSERT INTO rbac_predicate_attribute VALUES (-1270,'name','logon.lockoutTime');
INSERT INTO rbac_permission VALUES (-1271,0,-1250,'CREATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1272,0,-1271);
INSERT INTO rbac_predicate_attribute VALUES (-1272,'name','logon.sessionExpiry');
INSERT INTO rbac_permission VALUES (-1273,0,-1250,'CREATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-1274,0,-1273);
INSERT INTO rbac_predicate_attribute VALUES (-1274,'name','logon.inactivityPeriod');

--
-- Manage private keys role gains ability to manage the audit viewer decryption key
--
INSERT INTO `rbac_permission` VALUES
    (-1115,0,-1100,'UPDATE',NULL,'CLUSTER_PROPERTY'),
    (-1116,0,-1100,'DELETE',NULL,'CLUSTER_PROPERTY'),
    (-1117,0,-1100,'CREATE',NULL,'CLUSTER_PROPERTY'),
    (-1118,0,-1100,'READ',NULL,'CLUSTER_PROPERTY');
INSERT INTO `rbac_predicate` VALUES
    (-1115,0,-1115),
    (-1116,0,-1116),
    (-1117,0,-1117),
    (-1118,0,-1118);
INSERT INTO `rbac_predicate_attribute` VALUES
    (-1115,'name','keyStore.auditViewer.alias'),
    (-1116,'name','keyStore.auditViewer.alias'),
    (-1117,'name','keyStore.auditViewer.alias'),
    (-1118,'name','keyStore.auditViewer.alias');

--
--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--


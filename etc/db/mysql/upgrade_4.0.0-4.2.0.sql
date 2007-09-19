---
--- Script to update mysql ssg database from 4.0 to 4.2 (no changes in 4.1)
---
--- Layer 7 Technologies, inc
---

--
-- Disable FK while manipulating tables
--
SET FOREIGN_KEY_CHECKS=0;

--
-- Table structure for revocation checking policies
--

CREATE TABLE revocation_check_policy (
  objectid bigint(20) NOT NULL,
  version int(11) default NULL,
  name varchar(128) NOT NULL,
  revocation_policy_xml mediumtext,
  default_policy tinyint default '0',
  default_success tinyint default '0',
  PRIMARY KEY  (objectid),
  UNIQUE KEY rcp_name_idx (name)  
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Add RBAC permissions to the 'Manage Certificates (truststore)' role
--
UPDATE rbac_role SET description = 'Users assigned to the {0} role have the ability to read, create, update and delete trusted certificates and policies for revocation checking.' WHERE objectid = -600;
DELETE FROM rbac_permission WHERE objectid in (-605, -606, -607, -608);
INSERT INTO rbac_permission VALUES (-605,0,-600,'UPDATE',NULL,'REVOCATION_CHECK_POLICY');
INSERT INTO rbac_permission VALUES (-606,0,-600,'READ',NULL,'REVOCATION_CHECK_POLICY');
INSERT INTO rbac_permission VALUES (-607,0,-600,'DELETE',NULL,'REVOCATION_CHECK_POLICY');
INSERT INTO rbac_permission VALUES (-608,0,-600,'CREATE',NULL,'REVOCATION_CHECK_POLICY');

--
-- Update TrustedCert table
--
ALTER TABLE trusted_cert ADD COLUMN trust_anchor tinyint default 1,
                         ADD COLUMN revocation_type varchar(128) NOT NULL DEFAULT 'USE_DEFAULT',
                         ADD COLUMN revocation_policy_oid bigint(20),
                         ADD FOREIGN KEY (revocation_policy_oid) REFERENCES revocation_check_policy (objectid);

--
-- Add DB entry for internal provider (configuration now editable)
--
INSERT INTO identity_provider (objectid,name,description,type,properties,version) VALUES (-2,'Internal Identity Provider','Internal Identity Provider',1,'<java version="1.6.0_01" class="java.beans.XMLDecoder"><object class="java.util.HashMap"><void method="put"><string>adminEnabled</string><boolean>true</boolean></void></object></java>',0);

ALTER TABLE audit_main ADD COLUMN signature varchar(175);

--
-- Add new JMS configuration options
--
ALTER TABLE jms_endpoint ADD COLUMN failure_destination_name varchar(128),
                         ADD COLUMN acknowledgement_type varchar(128);

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

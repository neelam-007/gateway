--
-- Script to update mysql ssg database from 7.0.0 to 7.1.0
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
UPDATE ssg_version SET current_version = '7.1.0';


--
-- Encapsulated Assertions
--
CREATE TABLE encapsulated_assertion (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  guid varchar(255) NOT NULL,
  policy_oid bigint(20) NOT NULL,
  FOREIGN KEY (policy_oid) REFERENCES policy (objectid),
  PRIMARY KEY (objectid),
  UNIQUE KEY i_guid (guid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

CREATE TABLE encapsulated_assertion_property (
  encapsulated_assertion_oid bigint(20) NOT NULL,
  name varchar(128) NOT NULL,
  value MEDIUMTEXT NOT NULL,
  FOREIGN KEY (encapsulated_assertion_oid) REFERENCES encapsulated_assertion (objectid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

CREATE TABLE encapsulated_assertion_argument (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  encapsulated_assertion_oid bigint(20) NOT NULL,
  argument_name varchar(128) NOT NULL,
  argument_type varchar(128) NOT NULL,
  gui_prompt tinyint(1) NOT NULL,
  gui_label varchar(255),
  ordinal int(20) NOT NULL,
  FOREIGN KEY (encapsulated_assertion_oid) REFERENCES encapsulated_assertion (objectid) ON DELETE CASCADE,
  PRIMARY KEY (objectid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

CREATE TABLE encapsulated_assertion_result (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  encapsulated_assertion_oid bigint(20) NOT NULL,
  result_name varchar(128) NOT NULL,
  result_type varchar(128) NOT NULL,
  FOREIGN KEY (encapsulated_assertion_oid) REFERENCES encapsulated_assertion (objectid) ON DELETE CASCADE,
  PRIMARY KEY (objectid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

INSERT INTO rbac_role VALUES (-1350,0,'Manage Encapsulated Assertions', null,'ENCAPSULATED_ASSERTION',null, 'Users assigned to the {0} role have the ability to create/read/update/delete encapsulated assertions.',0);
INSERT INTO rbac_permission VALUES (-1351,0,-1350,'CREATE',null,'ENCAPSULATED_ASSERTION');
INSERT INTO rbac_permission VALUES (-1352,0,-1350,'READ',NULL,'ENCAPSULATED_ASSERTION');
INSERT INTO rbac_permission VALUES (-1353,0,-1350,'UPDATE',null, 'ENCAPSULATED_ASSERTION');
INSERT INTO rbac_permission VALUES (-1354,0,-1350,'DELETE',NULL,'ENCAPSULATED_ASSERTION');
INSERT INTO rbac_permission VALUES (-1355,0,-1350,'READ',NULL,'POLICY');
INSERT INTO rbac_predicate VALUES (-1356,0,-1355);
INSERT INTO rbac_predicate_attribute VALUES (-1356,'type','Included Policy Fragment','eq');

INSERT INTO rbac_permission VALUES (-441,0,-400,'READ',NULL,'ENCAPSULATED_ASSERTION');
INSERT INTO rbac_permission VALUES (-359,0,-350,'READ',NULL,'ENCAPSULATED_ASSERTION');

ALTER TABLE cluster_properties ADD COLUMN properties MEDIUMTEXT NULL AFTER propvalue;

ALTER TABLE published_service MODIFY COLUMN wsdl_url VARCHAR(4096);

-- Firewall Rules table structure --
DROP TABLE IF EXISTS firewall_rule;
CREATE TABLE firewall_rule (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  ordinal integer NOT NULL,
  name varchar(128) NOT NULL,
  enabled tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (objectid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS firewall_rule_property;
CREATE TABLE firewall_rule_property (
  firewall_rule_oid bigint(20) NOT NULL,
  name varchar(128) NOT NULL,
  value MEDIUMTEXT NOT NULL,
  FOREIGN KEY (firewall_rule_oid) REFERENCES firewall_rule (objectid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

-- create new RBAC role for Manage Firewall Rules --
INSERT INTO rbac_role (objectid, version, name, entity_type, description, user_created) VALUES (-1400, 0, 'Manage Firewall Rules', 'FIREWALL_RULE', 'Users assigned to the {0} role have the ability to read, create, update and delete Firewall rules.', 0);
INSERT INTO rbac_permission VALUES (-1275,0,-1400,'CREATE',NULL,'FIREWALL_RULE');
INSERT INTO rbac_permission VALUES (-1276,0,-1400,'READ',NULL,'FIREWALL_RULE');
INSERT INTO rbac_permission VALUES (-1277,0,-1400,'UPDATE',NULL,'FIREWALL_RULE');
INSERT INTO rbac_permission VALUES (-1278,0,-1400,'DELETE',NULL,'FIREWALL_RULE');

-- Ensure dynamically-created manage service and manage policy roles get updated to support use of encapsulated assertions
INSERT INTO cluster_properties
    (objectid, version, propkey, propvalue, properties)
    values (-700100, 0, 'upgrade.task.700100', 'com.l7tech.server.upgrade.Upgrade70To71UpdateRoles', null);

--
-- Register upgrade task for Gateway Management internal service WSDL upgrades
--
INSERT INTO cluster_properties
    (objectid, version, propkey, propvalue, properties)
    values (-700101, 0, 'upgrade.task.700101', 'com.l7tech.server.upgrade.Upgrade70to71UpdateGatewayManagementWsdl', null);

-- Increasing the length of the subject dn to allow for certificates with longer subject dn's
-- See SSG-6774
ALTER TABLE client_cert MODIFY COLUMN subject_dn VARCHAR(2048);
ALTER TABLE trusted_cert MODIFY COLUMN subject_dn VARCHAR(2048);
-- updating the client_cert index to match the index in ssg.sql
-- setting the length to 255 will use the first 255 characters of the subject_dn to create the index.
ALTER TABLE client_cert DROP INDEX i_subject_dn;
CREATE INDEX i_subject_dn ON client_cert (subject_dn(255));

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--

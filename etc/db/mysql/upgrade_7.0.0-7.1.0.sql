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

ALTER TABLE cluster_properties ADD COLUMN properties MEDIUMTEXT NULL AFTER propvalue;

ALTER TABLE published_service MODIFY COLUMN wsdl_url VARCHAR(4096);

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--

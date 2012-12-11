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
  policy_oid bigint(20) NOT NULL,
  FOREIGN KEY (policy_oid) REFERENCES policy (objectid),
  PRIMARY KEY (objectid)
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
  default_value mediumtext,
  gui_prompt tinyint(1) NOT NULL,
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

ALTER TABLE cluster_properties ADD COLUMN properties MEDIUMTEXT NULL AFTER propvalue;

ALTER TABLE published_service MODIFY COLUMN wsdl_url VARCHAR(4096);

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--

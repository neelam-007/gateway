--
-- Script to update mysql ssg database from 6.1.5 to 6.2.0
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
UPDATE ssg_version SET current_version = '6.2.0';


--
-- Table for generic (runtime) entity types
--
CREATE TABLE generic_entity (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  name varchar(255),
  description mediumtext,
  classname varchar(255) NOT NULL,
  enabled boolean DEFAULT TRUE,
  value_xml mediumtext,
  PRIMARY KEY (objectid),
  UNIQUE KEY i_classname_name (classname, name)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;


--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--

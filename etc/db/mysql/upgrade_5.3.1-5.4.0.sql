--
-- Script to update mysql ssg database from 5.3.1 to 5.4.0
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

UPDATE ssg_version SET current_version = '5.4.0';

--
-- bug 9071  allow larger JDBC URL
--
ALTER TABLE jdbc_connection MODIFY COLUMN jdbc_url varchar(4096) ;

--
-- Secure password storage facility
--
DROP TABLE IF EXISTS secure_password;
CREATE TABLE secure_password (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  description varchar(256),
  usage_from_variable tinyint(1) NOT NULL DEFAULT 0,
  encoded_password varchar(256) NOT NULL,
  last_update bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (objectid),
  UNIQUE(name)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--


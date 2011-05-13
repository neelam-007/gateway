--
-- Script to update mysql ssg database from 6.0.0 to 6.1.0
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
UPDATE ssg_version SET current_version = '6.1.0';

--
-- Create "sequence" function for next_hi value
--
-- NOTE that the function is safe when either row based or statement based replication is in use.
--
delimiter //
CREATE FUNCTION next_hi() RETURNS bigint NOT DETERMINISTIC MODIFIES SQL DATA SQL SECURITY INVOKER
BEGIN
    UPDATE hibernate_unique_key SET next_hi=last_insert_id(next_hi)+IF(@@global.server_id=0,1,2);
    RETURN IF((last_insert_id()%2=0 and @@global.server_id=1) or (last_insert_id()%2=1 and @@global.server_id=2),last_insert_id()+1,last_insert_id());
END
//
delimiter ;

--
-- Cluster info updated to support ESM IP
--
ALTER TABLE cluster_info ADD COLUMN esm_address varchar(39) NOT NULL DEFAULT '' AFTER address;
UPDATE cluster_info set esm_address = address;
ALTER TABLE cluster_info ALTER COLUMN esm_address DROP DEFAULT;

--
-- Table for replication monitoring
--
DROP TABLE IF EXISTS replication_status;
CREATE TABLE replication_status (
  objectid bigint(20) NOT NULL,
  sequence bigint(20) NOT NULL DEFAULT 1,
  updated bigint(20) NOT NULL DEFAULT 0,
  nodeid varchar(32),
  delay bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (objectid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

INSERT INTO replication_status (objectid, nodeid) values (1, null);

--
--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--


--
-- Gateway database creation script for core tables on MySQL
--

SET FOREIGN_KEY_CHECKS = 0;

--
-- Table structure for table 'hibernate_unique_key'
--

DROP TABLE IF EXISTS hibernate_unique_key;
CREATE TABLE hibernate_unique_key (
  next_hi int(11) default NULL
) ENGINE=MyISAM DEFAULT CHARACTER SET utf8;

--
-- Dumping data for table 'hibernate_unique_key'
--

INSERT INTO hibernate_unique_key VALUES (1);

--
-- Create "sequence" function for next_hi value
--
-- NOTE that the function is safe when either row based or statement based replication is in use.
--
DROP FUNCTION IF EXISTS next_hi;
delimiter //
CREATE FUNCTION next_hi() RETURNS bigint NOT DETERMINISTIC MODIFIES SQL DATA SQL SECURITY INVOKER
BEGIN
    UPDATE hibernate_unique_key SET next_hi=last_insert_id(next_hi)+IF(@@global.server_id=0,1,2);
    RETURN IF((last_insert_id()%2=0 and @@global.server_id=1) or (last_insert_id()%2=1 and @@global.server_id=2),last_insert_id()+1,last_insert_id());
END
//
delimiter ;

DROP TABLE IF EXISTS ssg_version;
CREATE TABLE ssg_version (
   current_version char(10) NOT NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

INSERT INTO ssg_version (current_version) VALUES ('6.2.0');

SET FOREIGN_KEY_CHECKS = 1;

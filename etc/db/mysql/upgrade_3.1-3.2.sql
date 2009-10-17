--
-- Script to update mysql ssg database from 3.1 to 3.2
--
-- Layer 7 Technologies, inc

ALTER TABLE audit_message ADD COLUMN operation_name varchar(255);
ALTER TABLE audit_message ADD COLUMN response_status int(11);
ALTER TABLE audit_message ADD COLUMN routing_latency int(11);

DROP TABLE IF EXISTS counters;
CREATE TABLE counters (
  counterid bigint(20) NOT NULL,
  userid varchar(128),
  providerid bigint(20) NOT NULL,
  countername varchar(128) NOT NULL,
  cnt_sec bigint(20) default 0,
  cnt_hr bigint(20) default 0,
  cnt_day bigint(20) default 0,
  cnt_mnt bigint(20) default 0,
  last_update bigint(20) default 0,
  unique(userid, providerid, countername),
  PRIMARY KEY (counterid)
) TYPE=InnoDB;

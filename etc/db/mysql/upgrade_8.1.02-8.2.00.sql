--
-- Script to update mysql ssg database from 8.1.02 to 8.2.00
--
-- Layer 7 Technologies, inc
--

--
-- Disable FK while manipulating tables
--
SET FOREIGN_KEY_CHECKS=0;

--
-- Update the version at the very end, safe to start gateway
--
UPDATE ssg_version SET current_version = '8.2.00';

ALTER TABLE jms_endpoint ADD COLUMN is_passthrough_message_rules tinyint NOT NULL DEFAULT 1;

DROP TABLE IF EXISTS jms_endpoint_message_rule;
CREATE TABLE jms_endpoint_message_rule(
  goid binary(16) NOT NULL,
  version int NOT NULL,
  jms_endpoint_goid binary(16) NOT NULL,
  rule_name varchar(256) NOT NULL,
  is_passthrough bit NOT NULL,
  custom_pattern varchar(4096),
  FOREIGN KEY (jms_endpoint_goid) REFERENCES jms_endpoint (goid) ON DELETE CASCADE,
  PRIMARY KEY (goid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

-- Modify the fed user table subject_dn index so that is is smaller then 767 byts. SSG-8229
ALTER TABLE fed_user DROP INDEX i_subject_dn;
ALTER TABLE fed_user ADD INDEX i_subject_dn(subject_dn(255));

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--

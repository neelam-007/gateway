--
-- Script to update mysql ssg database from 6.1.0 to 6.2.0
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
-- Table for stored secure conversation sessions
--
DROP TABLE IF EXISTS wssc_session;
CREATE TABLE wssc_session (
  objectid bigint(20) NOT NULL,
  session_key_hash varchar(128),
  inbound tinyint(1) NOT NULL DEFAULT 0,
  identifier varchar(4096) NOT NULL,
  service_url varchar(4096),
  encrypted_key varchar(4096),
  created bigint(20) NOT NULL,
  expires bigint(20) NOT NULL,
  provider_id bigint NOT NULL,
  user_id varchar(255) NOT NULL,
  user_login varchar(255) NOT NULL,
  namespace varchar(4096),
  token mediumtext,
  UNIQUE KEY (session_key_hash),
  PRIMARY KEY (objectid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Bug 10662: Enforce unique ordinals per policy for policy versions
--
ALTER IGNORE TABLE policy_version ADD UNIQUE KEY i_policy_ordinal (policy_oid, ordinal);

--
-- Register upgrade task for Gateway Management internal service WSDL upgrades
--
INSERT INTO cluster_properties
    (objectid, version, propkey, propvalue)
    values (-600200, 0, "upgrade.task.600200", "com.l7tech.server.upgrade.Upgrade61to62UpdateGatewayManagementWsdl");

--
--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--

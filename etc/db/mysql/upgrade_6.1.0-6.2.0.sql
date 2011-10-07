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
-- Bug 6407: Specify max xml size for messages going into gateway
--
ALTER TABLE jms_endpoint ADD COLUMN request_max_size bigint NOT NULL default -1 AFTER use_message_id_for_correlation;
--
-- Register upgrade task for Gateway Management internal service WSDL upgrades
--
INSERT INTO cluster_properties
    (objectid, version, propkey, propvalue)
    values (-600200, 0, "upgrade.task.600200", "com.l7tech.server.upgrade.Upgrade61to62UpdateGatewayManagementWsdl");

--
-- Add generic "properties" field for a persistent identity
--
ALTER TABLE internal_user ADD COLUMN properties mediumtext default NULL;



-- TODO [steve] update admin/operator roles for log viewing INSERT INTO rbac_permission VALUES (-105, 0, -100, 'OTHER', 'log-viewer', 'LOG_SINK');

--
-- New role for viewing the default log (oid = -810)
--
INSERT INTO rbac_role VALUES (-1300,0,'View Default Gateway Logs', null,null,null, 'Users assigned to the {0} role have the ability to view the default gateway logs.');
INSERT INTO rbac_permission VALUES (-1301,0,-1300,'READ',NULL,'CLUSTER_INFO');
INSERT INTO rbac_permission VALUES (-1302,0,-1300,'READ',NULL,'LOG_SINK');
INSERT INTO rbac_predicate VALUES (-1303,0,-1302);
INSERT INTO rbac_predicate_attribute VALUES (-1303,'oid','-810');
INSERT INTO rbac_permission VALUES (-1304,0,-1300,'OTHER','log-viewer', 'LOG_SINK');

--
-- Register upgrade task for Log view role upgrades
--
INSERT INTO cluster_properties
    (objectid, version, propkey, propvalue)
    values (-600300, 0, "upgrade.task.600300", "com.l7tech.server.upgrade.Upgrade61To62AddRoles");  //TODO [steve] renumber


--
-- Bug 9860: "Throughput Quota Enhancement" Feature Request
--
ALTER TABLE counters MODIFY countername varchar(255) NOT NULL;
UPDATE counters SET countername = CONCAT(countername, '-', userid, '-', providerid) WHERE userid != '*' AND providerid != -1;
ALTER TABLE counters DROP userid, DROP providerid;

--
-- Bug 10943: Prevent deletion of non-empty folder
--
ALTER TABLE published_service DROP FOREIGN KEY published_service_ibfk_2;
ALTER TABLE policy DROP FOREIGN KEY policy_ibfk_1;
ALTER TABLE folder DROP FOREIGN KEY folder_ibfk_1;
ALTER TABLE published_service ADD CONSTRAINT published_service_folder FOREIGN KEY (folder_oid) REFERENCES folder (objectid);
ALTER TABLE policy ADD CONSTRAINT policy_folder FOREIGN KEY (folder_oid) REFERENCES folder (objectid);
ALTER TABLE folder ADD CONSTRAINT folder_parent_folder FOREIGN KEY (parent_folder_oid) REFERENCES folder (objectid);

--
--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--

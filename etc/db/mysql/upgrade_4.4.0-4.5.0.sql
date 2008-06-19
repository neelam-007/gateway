---
--- Script to update mysql ssg database from 4.4 to 4.5
---
--- Layer 7 Technologies, inc
---

--
-- Disable FK while manipulating tables
--
SET FOREIGN_KEY_CHECKS=0;
                                                                                                                                               
-- Added new column for Revocation policy (Bug 4950)
BEGIN;
ALTER TABLE revocation_check_policy ADD continue_server_unavailable tinyint default '0';
COMMIT;

-- New columns for Policy GUIDS
ALTER TABLE policy ADD guid char(36) NOT NULL;
ALTER TABLE policy ADD internal_tag VARCHAR(64);

--New flag for published services to indicate whether or not they are an internal service
ALTER TABLE published_service ADD internal TINYINT(1) NOT NULL DEFAULT 0;

INSERT INTO cluster_properties
    (objectid, version, propkey, propvalue)
    values (-400500, 0, "upgrade.task.400500", "com.l7tech.server.upgrade.Upgrade44To45SwitchPolicyIncludesToGuids");

--New permissions for retrieving service templates (only needed for manage or publish services)
INSERT INTO rbac_permission VALUES (-355,0,-350,'READ',NULL,'SERVICE_TEMPLATE');
INSERT INTO rbac_permission VALUES (-414,0,-400,'READ',NULL,'SERVICE_TEMPLATE');
INSERT INTO rbac_permission VALUES (-415,0,-400,'READ',NULL,'POLICY');
INSERT INTO rbac_permission VALUES (-416,0,-400,'UPDATE',NULL,'POLICY');

-- Add Service State to metrics
ALTER TABLE service_metrics ADD service_state VARCHAR(16);

-- Create WSDM Subscription table
CREATE TABLE wsdm_subscription (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  uuid varchar(36) NOT NULL,
  callback_url varchar(255) NOT NULL,
  published_service_oid bigint(20) NOT NULL,
  termination_time bigint(20) NOT NULL,
  topic int(11) NOT NULL,
  notification_policy_guid CHAR(36),
  last_notification bigint(20),
  owner_node_id varchar(36),
  PRIMARY KEY  (objectid),
  UNIQUE KEY uuid (uuid)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;


--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- Script to update mysql ssg database from 4.5 to 4.6
--
-- Layer 7 Technologies, inc
--

--
-- Disable FK while manipulating tables
--
SET FOREIGN_KEY_CHECKS=0;

-- Create EMAIL_LISTENER table
CREATE TABLE email_listener (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  host varchar(128) NOT NULL,
  port int(8) NOT NULL,
  server_type varchar(4) NOT NULL,
  use_ssl tinyint(1) NOT NULL,
  delete_on_receive tinyint(1) NOT NULL,
  username varchar(255) NOT NULL,
  password varchar(32) NOT NULL,
  folder varchar(255) NOT NULL,
  poll_interval int(8) NOT NULL,
  active tinyint(1) NOT NULL default 1,
  owner_node_id varchar(36),
  last_poll_time bigint(20),
  last_message_id bigint(20),
  PRIMARY KEY  (objectid)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

-- Add new column for password expiry time
ALTER TABLE internal_user ADD COLUMN password_expiry BIGINT(20) DEFAULT 0;

-- Add new column for change password
ALTER TABLE internal_user ADD COLUMN change_password boolean DEFAULT FALSE;

-- This one was missed in upgrade_4.4.0-4.5.0.sql
ALTER TABLE policy ADD KEY i2_guid (guid);

-- Add foreign key constraing for wsdm_subscriptions
ALTER TABLE wsdm_subscription ADD
  FOREIGN KEY (notification_policy_guid) REFERENCES policy (guid);

-- New table to track the login attempts
CREATE TABLE logon_info (
  version int(11) NOT NULL,
  provider_oid bigint(20) NOT NULL,
  login varchar(255) NOT NULL,
  fail_count int(11) NOT NULL DEFAULT 0,
  last_attempted bigint(20) NOT NULL,
  PRIMARY KEY (provider_oid, login),
  FOREIGN KEY (provider_oid) REFERENCES identity_provider(objectid) ON DELETE CASCADE
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

-- New table to record the password changes history
CREATE TABLE password_history (
  objectid bigint(20) NOT NULL,
  internal_user_oid bigint(20) NOT NULL,
  last_changed bigint(20) NOT NULL,
  order_id int(31) UNSIGNED,
  prev_password varchar(32),
  PRIMARY KEY (objectid),
  FOREIGN KEY (internal_user_oid) REFERENCES internal_user (objectid)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

-- add task to upgrade existing internal users with new passwory expiry date
INSERT INTO cluster_properties(objectid, version, propkey, propvalue)
    VALUES (-400600, 0, "upgrade.task.400600", "com.l7tech.server.upgrade.Upgrade45To46AddPasswordExpiry");

CREATE TABLE folder (
  objectid bigint(20) NOT NULL,
  name varchar(128) NOT NULL,
  parent_folder_oid bigint(20),
  PRIMARY KEY  (objectid),
  UNIQUE KEY `i_name_parent` (`name`,`parent_folder_oid`)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

ALTER TABLE folder ADD
  FOREIGN KEY (parent_folder_oid) REFERENCES folder (objectid) ON DELETE SET NULL;

INSERT INTO folder VALUES (-5000, 'Services', NULL);
INSERT INTO folder VALUES (-5001, 'Policies', NULL);


ALTER TABLE published_service ADD
  folder_oid bigint(20);

ALTER TABLE policy ADD
  folder_oid bigint(20);

UPDATE published_service SET folder_oid = -5000;
UPDATE policy SET folder_oid = -5001;

ALTER TABLE published_service ADD FOREIGN KEY (folder_oid) REFERENCES folder (objectid) ON DELETE SET NULL;

ALTER TABLE policy ADD FOREIGN KEY (folder_oid) REFERENCES folder (objectid) ON DELETE SET NULL;



-- Manage Webservices users can CRUD each and every possible folder
INSERT INTO rbac_permission VALUES (-417, 0, -400, 'CREATE', null, 'FOLDER');
INSERT INTO rbac_permission VALUES (-418, 0, -400, 'READ',   null, 'FOLDER');
INSERT INTO rbac_permission VALUES (-419, 0, -400, 'UPDATE', null, 'FOLDER');
INSERT INTO rbac_permission VALUES (-420, 0, -400, 'DELETE', null, 'FOLDER');

INSERT INTO rbac_permission VALUES (-554,0,-550,'READ',NULL,'METRICS_BIN');

INSERT INTO rbac_role VALUES (-850,0,'Gateway Maintenance', null,null, 'Users assigned to the {0} role have the ability to perform Gateway maintenance tasks.');
INSERT INTO rbac_permission VALUES (-851,0,-850,'READ',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-852,0,-851);
INSERT INTO rbac_predicate_attribute VALUES (-852,'name','audit.archiver.ftp.config');
INSERT INTO rbac_permission VALUES (-853,0,-850,'UPDATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-854,0,-853);
INSERT INTO rbac_predicate_attribute VALUES (-854,'name','audit.archiver.ftp.config');
INSERT INTO rbac_permission VALUES (-855,0,-850,'CREATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate VALUES (-856,0,-855);
INSERT INTO rbac_predicate_attribute VALUES (-856,'name','audit.archiver.ftp.config');
INSERT INTO rbac_permission VALUES (-857,0,-850,'DELETE',NULL,'AUDIT_RECORD');

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

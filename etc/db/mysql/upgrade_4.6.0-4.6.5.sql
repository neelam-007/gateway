--
-- Script to update mysql ssg database from 4.6 to 4.6.5
--
-- Layer 7 Technologies, inc
--

--
-- Disable FK while manipulating tables
--
SET FOREIGN_KEY_CHECKS=0;


-- Increase size of shared_keys
ALTER TABLE shared_keys CHANGE COLUMN b64edval b64edval varchar(2048) NOT NULL;

ALTER TABLE email_listener ADD COLUMN properties mediumtext;

-- add in the email listeners role since it's not present in the upgrade from anything pre 4.6.5
-- use the magic mysql syntax to not fail if it already exists
INSERT INTO rbac_role VALUES (-900,0,'Manage Email Listeners', null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete email listeners.') on duplicate key update objectid=LAST_INSERT_ID(objectid);
INSERT INTO rbac_permission VALUES (-901,0,-900,'READ',NULL,'EMAIL_LISTENER') on duplicate key update objectid=LAST_INSERT_ID(objectid);
INSERT INTO rbac_permission VALUES (-902,0,-900,'CREATE',NULL,'EMAIL_LISTENER') on duplicate key update objectid=LAST_INSERT_ID(objectid);
INSERT INTO rbac_permission VALUES (-903,0,-900,'UPDATE',NULL,'EMAIL_LISTENER') on duplicate key update objectid=LAST_INSERT_ID(objectid);
INSERT INTO rbac_permission VALUES (-904,0,-900,'DELETE',NULL,'EMAIL_LISTENER') on duplicate key update objectid=LAST_INSERT_ID(objectid);

-- add in the right permissions to allow the manage email listeners role to view published services
INSERT INTO rbac_permission VALUES (-905,0,-900,'READ',NULL,'SERVICE') on duplicate key update objectid=LAST_INSERT_ID(objectid);

-- add in the right permissions to allow the Manage JMS connector role to view published services
INSERT INTO rbac_permission VALUES (-659,1,-650,'READ',NULL,'SERVICE') on duplicate key update objectid=LAST_INSERT_ID(objectid);
--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- Script to update derby ssg database from 8.1.02 to 8.2.00
--
-- Layer 7 Technologies, inc
--

-- Update the version at the very end, safe to start gateway
--
UPDATE ssg_version SET current_version = '8.2.00';

-- updating the http_configuration table to match the one in mysql.
alter table http_configuration alter column tls_keystore_goid NOT NULL;
alter table http_configuration
    add constraint FK_HTTP_CONFIG_PROXY_PASSWORD
    foreign key (proxy_password_goid)
    references secure_password;
alter table http_configuration
    add constraint FK_HTTP_CONFIG_PASSWORD
    foreign key (password_goid)
    references secure_password;

-- updating the siteminder table to match the one in mysql
alter table siteminder_configuration
    add constraint FK_SITEMINDER_CONFIG_PASSWORD
    foreign key (password_goid)
    references secure_password;

-- RABC for "debugger" other permission: Update "Administrator" canned role --
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -106), 0, toGoid(0, -100), 'OTHER', 'debugger', 'POLICY');

-- RABC for "debugger" other permission: Update "Publish Webservices" canned role --
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -443),0, toGoid(0, -400),'OTHER','debugger','POLICY');

-- adding guid unique key constraint to policy table for derby. this matched the mysql schema.
alter table policy
    add constraint i_guid
    unique (guid);

--
-- Register upgrade task for adding "debugger" other permission to auto-created "Manage <Blah>" roles
--
INSERT INTO cluster_properties
    (goid, version, propkey, propvalue, properties)
    VALUES (toGoid(0,-800200),0,'upgrade.task.800200','com.l7tech.server.upgrade.Upgrade8102To8200UpdateRoles',null),
           (toGoid(0,-800201),0, 'upgrade.task.800201', 'com.l7tech.server.upgrade.Upgrade8102to8200UpdateGatewayManagementWsdl', null);
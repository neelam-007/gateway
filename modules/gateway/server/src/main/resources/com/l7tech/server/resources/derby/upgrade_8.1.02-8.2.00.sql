--
-- Script to update derby ssg database from 8.1.02 to 8.2.00
--
-- Layer 7 Technologies, inc
--

-- Update the version at the very end, safe to start gateway
--
UPDATE ssg_version SET current_version = '8.2.00';

alter table jms_endpoint add column is_passthrough_message_rules smallint default 1;

create table jms_endpoint_message_rule (
  goid CHAR(16) FOR BIT DATA not null,
  version integer,
  jms_endpoint_goid CHAR(16) FOR BIT DATA not null REFERENCES jms_endpoint (goid) ON DELETE CASCADE,
  rule_name varchar(256),
  is_passthrough smallint,
  custom_pattern varchar(4096),
  PRIMARY KEY (goid)
);

-- updating the http_configuration table to match the one in mysql.
alter table http_configuration alter column tls_keystore_goid NOT NULL;
alter table http_configuration
    add constraint FK_PROXY_PASSWORD
    foreign key (proxy_password_goid)
    references secure_password
    on delete cascade;
alter table http_configuration
    add constraint FK_PASSWORD
    foreign key (password_goid)
    references secure_password
    on delete cascade;

-- RABC for "debugger" other permission: Update "Publish Webservices" canned role --
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -443),0, toGoid(0, -400),'OTHER','debugger','SERVICE');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -444),0, toGoid(0, -400),'OTHER','debugger','POLICY');

--
-- Register upgrade task for adding "debugger" other permission to auto-created "Manage <Blah>" roles
--
INSERT INTO cluster_properties
    (goid, version, propkey, propvalue, properties)
    VALUES (toGoid(0,-800200),0,'upgrade.task.800200','com.l7tech.server.upgrade.Upgrade8102To8200UpdateRoles',null);
--
-- Script to update derby ssg database from 7.1.0 to 8.0.0
--
-- Layer 7 Technologies, inc
--

UPDATE ssg_version SET current_version = '8.0.0';

-- ****************************************************************************************************************** --
-- ********************************* OID'S NO LONGER EXIST AFTER THIS POINT ***************************************** --
-- ****************************************************************************************************************** --

-- TODO fix FR-473 by renumbering rbac_permission -440 to -441 and inserting CREATE ANY POLICY as new -440

--
-- Security Zones
--
create table security_zone (
  goid CHAR(16) FOR BIT DATA not null,
  version integer not null,
  name varchar(128) not null unique,
  description varchar(255) not null,
  entity_types varchar(4096) not null,
  primary key (goid)
);

create table rbac_predicate_security_zone (
  objectid bigint not null references rbac_predicate(objectid) on delete cascade,
  security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete cascade,
  primary key (objectid)
);

create table assertion_access (
  goid CHAR(16) FOR BIT DATA not null,
  version integer,
  name varchar(255) not null unique,
  security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
  primary key (goid)
);

create table siteminder_configuration (
  goid CHAR(16) FOR BIT DATA not null,
  version integer,
  name varchar(128) not null,
  agent_name varchar(256) not null,
  address varchar(128) not null,
  secret varchar(4096) not null,
  ipcheck smallint default 0,
  update_sso_token smallint default 0,
  enabled smallint,
  hostname varchar(255) not null,
  fipsmode integer not null default 0,
  host_configuration varchar(256),
  user_name varchar(256),
  password_oid bigint,
  noncluster_failover smallint default 0,
  cluster_threshold integer DEFAULT 50,
  security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
  primary key (goid)
);

CREATE TABLE siteminder_configuration_property (
  goid CHAR(16) FOR BIT DATA references siteminder_configuration(goid) on delete cascade,
  name varchar(128) not null,
  value varchar(32672) not null,
  primary key (goid,name)
);

alter table rbac_role add column entity_goid CHAR(16) FOR BIT DATA;

-- create new RBAC role for Manage SiteMinder Configuration --
INSERT INTO rbac_role (objectid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (-1500,0,'Manage SiteMinder Configuration', null, 'SITEMINDER_CONFIGURATION', null, null, 'Users assigned to the {0} role have the ability to read, create, update and delete SiteMinder configuration.',0);
INSERT INTO rbac_permission (objectid, version, role_oid, operation_type, other_operation, entity_type) VALUES (-1501,0,-1500,'READ',NULL,'SITEMINDER_CONFIGURATION');
INSERT INTO rbac_permission (objectid, version, role_oid, operation_type, other_operation, entity_type) VALUES (-1502,0,-1500,'CREATE',NULL,'SITEMINDER_CONFIGURATION');
INSERT INTO rbac_permission (objectid, version, role_oid, operation_type, other_operation, entity_type) VALUES (-1503,0,-1500,'UPDATE',NULL,'SITEMINDER_CONFIGURATION');
INSERT INTO rbac_permission (objectid, version, role_oid, operation_type, other_operation, entity_type) VALUES (-1504,0,-1500,'DELETE',NULL,'SITEMINDER_CONFIGURATION');
INSERT INTO rbac_permission (objectid, version, role_oid, operation_type, other_operation, entity_type) VALUES (-1505,0,-1500,'READ',NULL,'SECURE_PASSWORD');

alter table policy add column security_zone_goid CHAR(16) FOR BIT DATA;
alter table policy add foreign key (security_zone_goid) references security_zone (goid) on delete set null;

alter table policy_alias add column security_zone_goid CHAR(16) FOR BIT DATA;
alter table policy_alias add foreign key (security_zone_goid) references security_zone (goid) on delete set null;

alter table published_service add column security_zone_goid CHAR(16) FOR BIT DATA;
alter table published_service add foreign key (security_zone_goid) references security_zone (goid) on delete set null;

alter table published_service_alias add column security_zone_goid CHAR(16) FOR BIT DATA;
alter table published_service_alias add foreign key (security_zone_goid) references security_zone (goid) on delete set null;

alter table folder add column security_zone_goid CHAR(16) FOR BIT DATA;
alter table folder add foreign key (security_zone_goid) references security_zone (goid) on delete set null;

alter table identity_provider add column security_zone_goid CHAR(16) FOR BIT DATA;
alter table identity_provider add foreign key (security_zone_goid) references security_zone (goid) on delete set null;

alter table jdbc_connection add column security_zone_goid CHAR(16) FOR BIT DATA;
alter table jdbc_connection add foreign key (security_zone_goid) references security_zone (goid) on delete set null;

alter table trusted_cert add column security_zone_goid CHAR(16) FOR BIT DATA;
alter table trusted_cert add foreign key (security_zone_goid) references security_zone (goid) on delete set null;

alter table sink_config add column security_zone_goid CHAR(16) FOR BIT DATA;
alter table sink_config add foreign key (security_zone_goid) references security_zone (goid) on delete set null;

alter table secure_password add column security_zone_goid CHAR(16) FOR BIT DATA;
alter table secure_password add foreign key (security_zone_goid) references security_zone (goid) on delete set null;

alter table email_listener add column security_zone_goid CHAR(16) FOR BIT DATA;
alter table email_listener add foreign key (security_zone_goid) references security_zone (goid) on delete set null;

alter table resource_entry add column security_zone_goid CHAR(16) FOR BIT DATA;
alter table resource_entry add foreign key (security_zone_goid) references security_zone (goid) on delete set null;

alter table http_configuration add column security_zone_goid CHAR(16) FOR BIT DATA;
alter table http_configuration add foreign key (security_zone_goid) references security_zone (goid) on delete set null;

alter table connector add column security_zone_goid CHAR(16) FOR BIT DATA;
alter table connector add foreign key (security_zone_goid) references security_zone (goid) on delete set null;

alter table encapsulated_assertion add column security_zone_goid CHAR(16) FOR BIT DATA;
alter table encapsulated_assertion add foreign key (security_zone_goid) references security_zone (goid) on delete set null;

alter table active_connector add column security_zone_goid CHAR(16) FOR BIT DATA;
alter table active_connector add foreign key (security_zone_goid) references security_zone (goid) on delete set null;

alter table revocation_check_policy add column security_zone_goid CHAR(16) FOR BIT DATA;
alter table revocation_check_policy add foreign key (security_zone_goid) references security_zone (goid) on delete set null;

alter table uddi_registries add column security_zone_goid CHAR(16) FOR BIT DATA;
alter table uddi_registries add foreign key (security_zone_goid) references security_zone (goid) on delete set null;
alter table uddi_proxied_service_info add column security_zone_goid CHAR(16) FOR BIT DATA;
alter table uddi_proxied_service_info add foreign key (security_zone_goid) references security_zone (goid) on delete set null;
alter table uddi_service_control add column security_zone_goid CHAR(16) FOR BIT DATA;
alter table uddi_service_control add foreign key (security_zone_goid) references security_zone (goid) on delete set null;

alter table sample_messages add column security_zone_goid CHAR(16) FOR BIT DATA;
alter table sample_messages add foreign key (security_zone_goid) references security_zone (goid) on delete set null;

alter table jms_endpoint add column security_zone_goid CHAR(16) FOR BIT DATA;
alter table jms_endpoint add foreign key (security_zone_goid) references security_zone (goid) on delete set null;
alter table jms_connection add column security_zone_goid CHAR(16) FOR BIT DATA;
alter table jms_connection add foreign key (security_zone_goid) references security_zone (goid) on delete set null;
--
-- RBAC for Assertions: Update "Publish Webservices" and "Manage Webservices" canned roles so they can still use policy assertions in 8.0
--
INSERT INTO rbac_permission (objectid, version, role_oid, operation_type, other_operation, entity_type) VALUES (-360,0,-350,'READ',NULL,'ASSERTION_ACCESS');
INSERT INTO rbac_permission (objectid, version, role_oid, operation_type, other_operation, entity_type) VALUES (-361,0,-350,'CREATE',NULL,'ASSERTION_ACCESS');
INSERT INTO rbac_permission (objectid, version, role_oid, operation_type, other_operation, entity_type) VALUES (-442,0,-400,'CREATE',NULL,'ASSERTION_ACCESS');
INSERT INTO rbac_permission (objectid, version, role_oid, operation_type, other_operation, entity_type) VALUES (-443,0,-400,'READ',NULL,'ASSERTION_ACCESS');

-- Increasing the length of the issuer dn to match the length of the subject dn
-- See SSG-6848, SSG-6849, SSG-6850
ALTER TABLE client_cert ALTER COLUMN issuer_dn SET DATA TYPE VARCHAR(2048);
ALTER TABLE trusted_cert ALTER COLUMN issuer_dn SET DATA TYPE VARCHAR(2048);

--
-- Keystore private key metadata (security zones)
--
create table keystore_key_metadata (
  objectid bigint not null,
  version integer,
  keystore_file_oid bigint not null references keystore_file(objectid) on delete cascade,
  alias varchar(255) not null,
  security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
  primary key (objectid),
  unique (keystore_file_oid, alias)
);

--
-- Register upgrade task for adding Assertion Access to auto-created "Manage <Blah>" roles
--
INSERT INTO cluster_properties
    (objectid, version, propkey, propvalue, properties)
    values (-800000, 0, 'upgrade.task.800000', 'com.l7tech.server.upgrade.Upgrade71To80UpdateRoles', null);

--
-- Custom key value store
--
CREATE TABLE custom_key_value_store (
  goid CHAR(16) FOR BIT DATA NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  value blob(2147483647) NOT NULL,
  PRIMARY KEY (goid),
  UNIQUE (name)
);

INSERT INTO rbac_role (objectid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (-1450,0,'Manage Custom Key Value Store', null,'CUSTOM_KEY_VALUE_STORE',null,null, 'Users assigned to the {0} role have the ability to read, create, update, and delete key values from custom key value store.',0);
INSERT INTO rbac_permission (objectid, version, role_oid, operation_type, other_operation, entity_type) VALUES (-1451,0,-1450,'CREATE',null,'CUSTOM_KEY_VALUE_STORE');
INSERT INTO rbac_permission (objectid, version, role_oid, operation_type, other_operation, entity_type) VALUES (-1452,0,-1450,'READ',null,'CUSTOM_KEY_VALUE_STORE');
INSERT INTO rbac_permission (objectid, version, role_oid, operation_type, other_operation, entity_type) VALUES (-1453,0,-1450,'UPDATE',null,'CUSTOM_KEY_VALUE_STORE');
INSERT INTO rbac_permission (objectid, version, role_oid, operation_type, other_operation, entity_type) VALUES (-1454,0,-1450,'DELETE',null,'CUSTOM_KEY_VALUE_STORE');

--
-- Goidification modification. These involve replacing the oid column with a goid column on entity tables.
--

-- adding in helper functions for derby:
CREATE FUNCTION toGoid(high bigint, low bigint) RETURNS char(16) for bit data
    PARAMETER STYLE JAVA NO SQL LANGUAGE JAVA
    EXTERNAL NAME 'com.l7tech.server.upgrade.DerbyFunctions.toGoid';

CREATE FUNCTION goidToString(bytes char(16) for bit data) RETURNS CHAR(32)
    PARAMETER STYLE JAVA NO SQL LANGUAGE JAVA
    EXTERNAL NAME 'com.l7tech.server.upgrade.DerbyFunctions.goidToString';

CREATE FUNCTION ifNull(v1 VARCHAR(128), v2 VARCHAR(128)) RETURNS VARCHAR(128)
    PARAMETER STYLE JAVA NO SQL LANGUAGE JAVA
    EXTERNAL NAME 'com.l7tech.server.upgrade.DerbyFunctions.ifNull';

CREATE FUNCTION randomLong() RETURNS bigint
    PARAMETER STYLE JAVA NO SQL LANGUAGE JAVA
    EXTERNAL NAME 'com.l7tech.server.upgrade.DerbyFunctions.randomLong';

CREATE FUNCTION randomLongNotReserved() RETURNS bigint
    PARAMETER STYLE JAVA NO SQL LANGUAGE JAVA
    EXTERNAL NAME 'com.l7tech.server.upgrade.DerbyFunctions.randomLongNotReserved';

CREATE procedure setVariable(keyParam CHAR(128), valueParam CHAR(128))
    PARAMETER STYLE JAVA NO SQL LANGUAGE JAVA
    EXTERNAL NAME 'com.l7tech.server.upgrade.DerbyFunctions.setVariable';

CREATE FUNCTION getVariable(keyParam CHAR(128)) RETURNS CHAR(128)
    PARAMETER STYLE JAVA NO SQL LANGUAGE JAVA
    EXTERNAL NAME 'com.l7tech.server.upgrade.DerbyFunctions.getVariable';

-- JdbcConnection
ALTER TABLE jdbc_connection ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('jdbc_connection_prefix', cast(randomLongNotReserved() as char(21)));
update jdbc_connection set goid = toGoid(cast(getVariable('jdbc_connection_prefix') as bigint), objectid);
ALTER TABLE jdbc_connection ALTER COLUMN goid NOT NULL;
ALTER TABLE jdbc_connection DROP PRIMARY KEY;
ALTER TABLE jdbc_connection DROP COLUMN objectid;
ALTER TABLE jdbc_connection ADD PRIMARY KEY (goid);
update rbac_role set entity_goid = toGoid(cast(getVariable('jdbc_connection_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='JDBC_CONNECTION';
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('jdbc_connection_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'SERVICE'), oid1.entity_id);

-- MetricsBin, MetricsBinDetail  - metrics are not enabled for derby, no data to upgrade
ALTER TABLE service_metrics DROP PRIMARY KEY;
ALTER TABLE service_metrics ADD COLUMN goid CHAR(16) FOR BIT DATA;
ALTER TABLE service_metrics ALTER COLUMN goid NOT NULL;
ALTER TABLE service_metrics DROP COLUMN objectid;
ALTER TABLE service_metrics ADD PRIMARY KEY (goid);

ALTER TABLE service_metrics_details DROP PRIMARY KEY;
ALTER TABLE service_metrics_details ADD COLUMN service_metrics_goid CHAR(16) FOR BIT DATA;
ALTER TABLE service_metrics_details ALTER COLUMN service_metrics_goid NOT NULL;
ALTER TABLE service_metrics_details DROP COLUMN service_metrics_oid;
ALTER TABLE service_metrics_details ADD PRIMARY KEY (service_metrics_goid, mapping_values_oid);

-- LogonInfo
ALTER TABLE logon_info ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('logon_info_prefix', cast(randomLongNotReserved() as char(21)));
update logon_info set goid = toGoid(cast(getVariable('logon_info_prefix') as bigint), objectid);
ALTER TABLE logon_info ALTER COLUMN goid NOT NULL;
ALTER TABLE logon_info DROP PRIMARY KEY;
ALTER TABLE logon_info DROP COLUMN objectid;
ALTER TABLE logon_info ADD PRIMARY KEY (goid);

-- SampleMessage
ALTER TABLE sample_messages ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('sample_messages_prefix', cast(randomLongNotReserved() as char(21)));
update sample_messages set goid = toGoid(cast(getVariable('sample_messages_prefix') as bigint), objectid);
ALTER TABLE sample_messages ALTER COLUMN goid NOT NULL;
ALTER TABLE sample_messages DROP PRIMARY KEY;
ALTER TABLE sample_messages DROP COLUMN objectid;
ALTER TABLE sample_messages ADD PRIMARY KEY (goid);

update rbac_role set entity_goid = toGoid(cast(getVariable('sample_messages_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='SAMPLE_MESSAGE';
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('sample_messages_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'SAMPLE_MESSAGE'), oid1.entity_id);

-- ClusterProperty
ALTER TABLE cluster_properties ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('cluster_properties_prefix', cast(randomLongNotReserved() as char(21)));
update cluster_properties set goid = toGoid(cast(getVariable('cluster_properties_prefix') as bigint), objectid);
update cluster_properties set goid = toGoid(0, objectid) where propkey = 'cluster.hostname';
update cluster_properties set goid = toGoid(0, objectid) where propkey like 'upgrade.task.%';
ALTER TABLE cluster_properties ALTER COLUMN goid NOT NULL;
ALTER TABLE cluster_properties DROP PRIMARY KEY;
ALTER TABLE cluster_properties DROP COLUMN objectid;
ALTER TABLE cluster_properties ADD PRIMARY KEY (goid);

update rbac_role set entity_goid = toGoid(cast(getVariable('cluster_properties_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='CLUSTER_PROPERTY';
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('cluster_properties_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'CLUSTER_PROPERTY'), oid1.entity_id);

-- ClusterProperty
ALTER TABLE secure_password ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('secure_password_prefix', cast(randomLongNotReserved() as char(21)));
update secure_password set goid = toGoid(cast(getVariable('secure_password_prefix') as bigint), objectid);
ALTER TABLE secure_password ALTER COLUMN goid NOT NULL;
ALTER TABLE secure_password DROP PRIMARY KEY;
ALTER TABLE secure_password DROP COLUMN objectid;
ALTER TABLE secure_password ADD PRIMARY KEY (goid);

update rbac_role set entity_goid = toGoid(cast(getVariable('secure_password_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='SECURE_PASSWORD';
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('secure_password_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'SECURE_PASSWORD'), oid1.entity_id);

ALTER TABLE http_configuration ADD COLUMN password_goid CHAR(16) FOR BIT DATA;
update http_configuration set password_goid = toGoid(cast(getVariable('secure_password_prefix') as bigint), password_oid);
ALTER TABLE http_configuration DROP COLUMN password_oid;

ALTER TABLE http_configuration ADD COLUMN proxy_password_goid CHAR(16) FOR BIT DATA;
update http_configuration set proxy_password_goid = toGoid(cast(getVariable('secure_password_prefix') as bigint), proxy_password_oid);
ALTER TABLE http_configuration DROP COLUMN proxy_password_oid;

ALTER TABLE siteminder_configuration ADD COLUMN password_goid CHAR(16) FOR BIT DATA;
update siteminder_configuration set password_goid = toGoid(cast(getVariable('secure_password_prefix') as bigint), password_oid);
ALTER TABLE siteminder_configuration DROP COLUMN password_oid;

-- Resource Entry
ALTER TABLE resource_entry ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('resource_entry_prefix', cast(randomLongNotReserved() as char(21)));
update resource_entry set goid = toGoid(cast(getVariable('resource_entry_prefix') as bigint), objectid);
update resource_entry set goid = toGoid(0, objectid) where objectid in (-3,-4,-5,-6,-7);
ALTER TABLE resource_entry ALTER COLUMN goid NOT NULL;
ALTER TABLE resource_entry DROP PRIMARY KEY;
ALTER TABLE resource_entry DROP COLUMN objectid;
ALTER TABLE resource_entry ADD PRIMARY KEY (goid);

update rbac_role set entity_goid = toGoid(cast(getVariable('resource_entry_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='RESOURCE_ENTRY' and entity_oid not in (-3,-4,-5,-6,-7);
update rbac_role set entity_goid = toGoid(0,entity_oid) where entity_oid is not null and entity_type='RESOURCE_ENTRY' and entity_oid in (-3,-4,-5,-6,-7);
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('resource_entry_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'RESOURCE_ENTRY'), oid1.entity_id) where entity_id not in ('-3','-4','-5','-6','-7');
update rbac_predicate_oid oid1 set oid1.entity_id = goidToString(toGoid(0,cast(oid1.entity_id as bigint))) where entity_id in ('-3','-4','-5','-6','-7');

-- EmailListener
ALTER TABLE email_listener_state DROP CONSTRAINT FK5A708C492FC43EC3;
ALTER TABLE email_listener ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('email_listener_prefix', cast(randomLongNotReserved() as char(21)));
update email_listener set goid = toGoid(cast(getVariable('email_listener_prefix') as bigint), objectid);
ALTER TABLE email_listener ALTER COLUMN goid NOT NULL;
ALTER TABLE email_listener DROP PRIMARY KEY;
ALTER TABLE email_listener DROP COLUMN objectid;
ALTER TABLE email_listener ADD PRIMARY KEY (goid);

ALTER TABLE email_listener_state ADD COLUMN email_listener_goid CHAR(16) FOR BIT DATA;
update email_listener_state set email_listener_goid = toGoid(cast(getVariable('email_listener_prefix') as bigint), email_listener_id);
ALTER TABLE email_listener_state ALTER COLUMN email_listener_goid NOT NULL;
ALTER TABLE email_listener_state DROP COLUMN email_listener_id;
ALTER TABLE email_listener_state ADD UNIQUE (email_listener_goid);

ALTER TABLE email_listener_state ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('email_listener_state_prefix', cast(randomLongNotReserved() as char(21)));
update email_listener_state set goid = toGoid(cast(getVariable('email_listener_state_prefix') as bigint), objectid);
ALTER TABLE email_listener_state ALTER COLUMN goid NOT NULL;
ALTER TABLE email_listener_state DROP PRIMARY KEY;
ALTER TABLE email_listener_state DROP COLUMN objectid;
ALTER TABLE email_listener_state ADD PRIMARY KEY (goid);
alter table email_listener_state ADD CONSTRAINT FK5A708C492FC43EC3 foreign key (email_listener_goid) references email_listener on delete cascade;

update rbac_role set entity_goid = toGoid(cast(getVariable('email_listener_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='EMAIL_LISTENER';
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('email_listener_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'EMAIL_LISTENER'), oid1.entity_id);

-- GenericEntity
ALTER TABLE generic_entity ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('generic_entity_prefix', cast(randomLongNotReserved() as char(21)));
update generic_entity set goid = toGoid(cast(getVariable('generic_entity_prefix') as bigint), objectid);
ALTER TABLE generic_entity ALTER COLUMN goid NOT NULL;
ALTER TABLE generic_entity DROP PRIMARY KEY;
ALTER TABLE generic_entity DROP COLUMN objectid;
ALTER TABLE generic_entity ADD PRIMARY KEY (goid);

update rbac_role set entity_goid = toGoid(cast(getVariable('generic_entity_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='GENERIC';
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('generic_entity_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'GENERIC'), oid1.entity_id);

-- Connector
ALTER TABLE connector_property DROP CONSTRAINT FK7EC2A187BA66EE5C;

ALTER TABLE connector ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('connector_prefix', cast(randomLongNotReserved() as char(21)));
update connector set goid = toGoid(cast(getVariable('connector_prefix') as bigint), objectid);
ALTER TABLE connector ALTER COLUMN goid NOT NULL;
ALTER TABLE connector DROP PRIMARY KEY;
ALTER TABLE connector DROP COLUMN objectid;
ALTER TABLE connector ADD PRIMARY KEY (goid);

ALTER TABLE connector_property ADD COLUMN connector_goid CHAR(16) FOR BIT DATA;
update connector_property set connector_goid = toGoid(cast(getVariable('connector_prefix') as bigint), connector_oid);
ALTER TABLE connector_property ALTER COLUMN connector_goid NOT NULL;
ALTER TABLE connector_property DROP COLUMN connector_oid;
ALTER TABLE connector_property add constraint FK7EC2A187BA66EE5C foreign key (connector_goid) references connector on delete cascade;
ALTER TABLE connector_property ADD PRIMARY KEY (connector_goid, name);

update rbac_role set entity_goid = toGoid(cast(getVariable('connector_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='SSG_CONNECTOR';
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('connector_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'SSG_CONNECTOR'), oid1.entity_id);


-- identity provider, user, groups

call setVariable('identity_provider_prefix', cast(randomLongNotReserved() as char(21)));
call setVariable('internal_group_prefix', cast(randomLongNotReserved() as char(21)));
call setVariable('internal_user_prefix', cast(randomLongNotReserved() as char(21)));
call setVariable('internal_user_group_prefix', cast(randomLongNotReserved() as char(21)));
call setVariable('fed_user_prefix', cast(randomLongNotReserved() as char(21)));
call setVariable('fed_group_prefix', cast(randomLongNotReserved() as char(21)));
call setVariable('fed_user_group_prefix', cast(randomLongNotReserved() as char(21)));

ALTER TABLE audit_main ADD COLUMN provider_oid_backup bigint;
update audit_main set provider_oid_backup = provider_oid;
ALTER TABLE audit_main DROP COLUMN provider_oid;
ALTER TABLE audit_main ADD COLUMN provider_goid CHAR(16) FOR BIT DATA;
update audit_main set provider_goid = toGoid(cast(getVariable('identity_provider_prefix') as bigint), provider_oid_backup);
update audit_main set provider_goid = toGoid(0,-2) where provider_oid_backup = -2;
UPDATE audit_main SET provider_goid = toGoid(cast(getVariable('identity_provider_prefix') as bigint), provider_oid_backup) where provider_oid_backup<>-1;
ALTER TABLE audit_main DROP COLUMN provider_oid_backup;

ALTER TABLE message_context_mapping_values ADD COLUMN auth_user_provider_id_backup bigint;
update message_context_mapping_values set auth_user_provider_id_backup = auth_user_provider_id;
ALTER TABLE message_context_mapping_values DROP COLUMN auth_user_provider_id;
ALTER TABLE message_context_mapping_values ADD COLUMN auth_user_provider_id CHAR(16) FOR BIT DATA;
update message_context_mapping_values set auth_user_provider_id = toGoid(cast(getVariable('identity_provider_prefix') as bigint), auth_user_provider_id_backup);
update message_context_mapping_values set auth_user_provider_id = toGoid(0,-2) where auth_user_provider_id_backup = -2;
ALTER TABLE message_context_mapping_values DROP COLUMN auth_user_provider_id_backup;

ALTER TABLE client_cert ADD COLUMN provider_backup bigint;
update client_cert set provider_backup = provider;
ALTER TABLE client_cert DROP COLUMN provider;
ALTER TABLE client_cert ADD COLUMN provider CHAR(16) FOR BIT DATA;
update client_cert set provider = toGoid(cast(getVariable('identity_provider_prefix') as bigint), provider_backup);
update client_cert set provider = toGoid(0,-2) where provider_backup = -2;
ALTER TABLE client_cert DROP COLUMN provider_backup;
ALTER TABLE client_cert ALTER COLUMN provider NOT NULL;

ALTER TABLE fed_group ADD COLUMN objectid_backup bigint;
update fed_group set objectid_backup = objectid;
ALTER TABLE fed_group DROP COLUMN objectid;
ALTER TABLE fed_group ADD COLUMN goid CHAR(16) FOR BIT DATA;
update fed_group set goid = toGoid(cast(getVariable('fed_group_prefix') as bigint), objectid_backup);
ALTER TABLE fed_group ALTER COLUMN goid NOT NULL;
ALTER TABLE fed_group DROP COLUMN objectid_backup;
ALTER TABLE fed_group ADD PRIMARY KEY (goid);

ALTER TABLE fed_group ADD COLUMN provider_oid_backup bigint;
update fed_group set provider_oid_backup = provider_oid;
ALTER TABLE fed_group DROP COLUMN provider_oid;
ALTER TABLE fed_group ADD COLUMN provider_goid CHAR(16) FOR BIT DATA;
update fed_group set provider_goid = toGoid(cast(getVariable('identity_provider_prefix') as bigint), provider_oid_backup);
update fed_group set provider_goid = toGoid(0,-2) where provider_oid_backup = -2;
ALTER TABLE fed_group DROP COLUMN provider_oid_backup;
ALTER TABLE fed_group ALTER COLUMN provider_goid NOT NULL;

ALTER TABLE fed_group_virtual ADD COLUMN objectid_backup bigint;
update fed_group_virtual set objectid_backup = objectid;
ALTER TABLE fed_group_virtual DROP COLUMN objectid;
ALTER TABLE fed_group_virtual ADD COLUMN goid CHAR(16) FOR BIT DATA;
update fed_group_virtual set goid = toGoid(cast(getVariable('fed_group_prefix') as bigint), objectid_backup);
ALTER TABLE fed_group_virtual ALTER COLUMN goid NOT NULL;
ALTER TABLE fed_group_virtual DROP COLUMN objectid_backup;
ALTER TABLE fed_group_virtual ADD PRIMARY KEY (goid);

ALTER TABLE fed_group_virtual ADD COLUMN provider_oid_backup bigint;
update fed_group_virtual set provider_oid_backup = provider_oid;
ALTER TABLE fed_group_virtual DROP COLUMN provider_oid;
ALTER TABLE fed_group_virtual ADD COLUMN provider_goid CHAR(16) FOR BIT DATA;
update fed_group_virtual set provider_goid = toGoid(cast(getVariable('identity_provider_prefix') as bigint), provider_oid_backup);
update fed_group_virtual set provider_goid = toGoid(0,-2) where provider_oid_backup = -2;
ALTER TABLE fed_group_virtual DROP COLUMN provider_oid_backup;
ALTER TABLE fed_group_virtual ALTER COLUMN provider_goid NOT NULL;

ALTER TABLE fed_user ADD COLUMN objectid_backup bigint;
update fed_user set objectid_backup = objectid;
ALTER TABLE fed_user DROP COLUMN objectid;
ALTER TABLE fed_user ADD COLUMN goid CHAR(16) FOR BIT DATA;
update fed_user set goid = toGoid(cast(getVariable('fed_user_prefix') as bigint), objectid_backup);
ALTER TABLE fed_user ALTER COLUMN goid NOT NULL;
ALTER TABLE fed_user DROP COLUMN objectid_backup;
ALTER TABLE fed_user ADD PRIMARY KEY (goid);

ALTER TABLE fed_user ADD COLUMN provider_oid_backup bigint;
update fed_user set provider_oid_backup = provider_oid;
ALTER TABLE fed_user DROP COLUMN provider_oid;
ALTER TABLE fed_user ADD COLUMN provider_goid CHAR(16) FOR BIT DATA;
update fed_user set provider_goid = toGoid(cast(getVariable('identity_provider_prefix') as bigint), provider_oid_backup);
update fed_user set provider_goid = toGoid(0,-2) where provider_oid_backup = -2;
ALTER TABLE fed_user DROP COLUMN provider_oid_backup;

ALTER TABLE fed_user_group ADD COLUMN provider_oid_backup bigint;
update fed_user_group set provider_oid_backup = provider_oid;
ALTER TABLE fed_user_group DROP COLUMN provider_oid;
ALTER TABLE fed_user_group ADD COLUMN provider_goid CHAR(16) FOR BIT DATA;
update fed_user_group set provider_goid = toGoid(cast(getVariable('identity_provider_prefix') as bigint), provider_oid_backup);
update fed_user_group set provider_goid = toGoid(0,-2) where provider_oid_backup = -2;
ALTER TABLE fed_user_group DROP COLUMN provider_oid_backup;
ALTER TABLE fed_user_group ALTER COLUMN provider_goid NOT NULL;

ALTER TABLE fed_user_group ADD COLUMN fed_group_oid_backup bigint;
update fed_user_group set fed_group_oid_backup = fed_group_oid;
ALTER TABLE fed_user_group DROP COLUMN fed_group_oid;
ALTER TABLE fed_user_group ADD COLUMN fed_group_goid CHAR(16) FOR BIT DATA;
update fed_user_group set fed_group_goid = toGoid(cast(getVariable('fed_group_prefix') as bigint), fed_group_oid_backup);
ALTER TABLE fed_user_group DROP COLUMN fed_group_oid_backup;
ALTER TABLE fed_user_group ALTER COLUMN fed_group_goid NOT NULL;

ALTER TABLE fed_user_group ADD COLUMN fed_user_oid_backup bigint;
update fed_user_group set fed_user_oid_backup = fed_user_oid;
ALTER TABLE fed_user_group DROP COLUMN fed_user_oid;
ALTER TABLE fed_user_group ADD COLUMN fed_user_goid CHAR(16) FOR BIT DATA;
update fed_user_group set fed_user_goid = toGoid(cast(getVariable('fed_user_prefix') as bigint), fed_user_oid_backup);
ALTER TABLE fed_user_group DROP COLUMN fed_user_oid_backup;
ALTER TABLE fed_user_group ALTER COLUMN fed_user_goid NOT NULL;
ALTER TABLE fed_user_group ADD PRIMARY KEY (provider_goid, fed_group_goid, fed_user_goid);

ALTER TABLE identity_provider ADD COLUMN objectid_backup bigint;
update identity_provider set objectid_backup = objectid;
ALTER TABLE identity_provider DROP COLUMN objectid;
ALTER TABLE identity_provider ADD COLUMN goid CHAR(16) FOR BIT DATA;
update identity_provider set goid = toGoid(cast(getVariable('identity_provider_prefix') as bigint), objectid_backup);
update identity_provider set goid = toGoid(0,-2) where objectid_backup = -2;
ALTER TABLE identity_provider ALTER COLUMN goid NOT NULL;
ALTER TABLE identity_provider DROP COLUMN objectid_backup;
ALTER TABLE identity_provider ADD PRIMARY KEY (goid);

update rbac_role set entity_goid = toGoid(cast(getVariable('identity_provider_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='ID_PROVIDER_CONFIG';
update rbac_role set entity_goid = toGoid(0,-2) where entity_oid is not null and entity_type='ID_PROVIDER_CONFIG' and entity_goid=toGoid(cast(getVariable('identity_provider_prefix') as bigint),-2);
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('identity_provider_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'ID_PROVIDER_CONFIG'), oid1.entity_id);
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(0,-2)) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'ID_PROVIDER_CONFIG' and oid1.entity_id=goidToString(toGoid(0,-2))), oid1.entity_id);
update rbac_predicate_attribute set value = goidToString(toGoid(cast(getVariable('identity_provider_prefix') as bigint),cast(value as bigint))) where attribute='providerId';
update rbac_predicate_attribute set value = goidToString(toGoid(0,-2)) where attribute='providerId' and value=goidToString(toGoid(cast(getVariable('identity_provider_prefix') as bigint),-2));

ALTER TABLE internal_group ADD COLUMN objectid_backup bigint;
update internal_group set objectid_backup = objectid;
ALTER TABLE internal_group DROP COLUMN objectid;
ALTER TABLE internal_group ADD COLUMN goid CHAR(16) FOR BIT DATA;
update internal_group set goid = toGoid(cast(getVariable('internal_group_prefix') as bigint), objectid_backup);
ALTER TABLE internal_group ALTER COLUMN goid NOT NULL;
ALTER TABLE internal_group DROP COLUMN objectid_backup;
ALTER TABLE internal_group ADD PRIMARY KEY (goid);

ALTER TABLE internal_user ADD COLUMN objectid_backup bigint;
update internal_user set objectid_backup = objectid;
ALTER TABLE internal_user DROP COLUMN objectid;
ALTER TABLE internal_user ADD COLUMN goid CHAR(16) FOR BIT DATA;
update internal_user set goid = toGoid(cast(getVariable('internal_user_prefix') as bigint), objectid_backup);
ALTER TABLE internal_user ALTER COLUMN goid NOT NULL;
ALTER TABLE internal_user DROP COLUMN objectid_backup;
ALTER TABLE internal_user ADD PRIMARY KEY (goid);

ALTER TABLE internal_user_group ADD COLUMN objectid_backup bigint;
update internal_user_group set objectid_backup = objectid;
ALTER TABLE internal_user_group DROP COLUMN objectid;
ALTER TABLE internal_user_group ADD COLUMN goid CHAR(16) FOR BIT DATA;
update internal_user_group set goid = toGoid(cast(getVariable('internal_user_group_prefix') as bigint), objectid_backup);
ALTER TABLE internal_user_group ALTER COLUMN goid NOT NULL;
ALTER TABLE internal_user_group DROP COLUMN objectid_backup;
ALTER TABLE internal_user_group ADD PRIMARY KEY (goid);

ALTER TABLE internal_user_group ADD COLUMN provider_oid_backup bigint;
update internal_user_group set provider_oid_backup = provider_oid;
ALTER TABLE internal_user_group DROP COLUMN provider_oid;
ALTER TABLE internal_user_group ADD COLUMN provider_goid CHAR(16) FOR BIT DATA;
update internal_user_group set provider_goid = toGoid(cast(getVariable('identity_provider_prefix') as bigint), provider_oid_backup);
update internal_user_group set provider_goid = toGoid(0,-2) where provider_oid_backup = -2;
ALTER TABLE internal_user_group DROP COLUMN provider_oid_backup;
ALTER TABLE internal_user_group ALTER COLUMN provider_goid NOT NULL;

ALTER TABLE internal_user_group ADD COLUMN user_id_backup bigint;
update internal_user_group set user_id_backup = user_id;
ALTER TABLE internal_user_group DROP COLUMN user_id;
ALTER TABLE internal_user_group ADD COLUMN user_goid CHAR(16) FOR BIT DATA;
update internal_user_group set user_goid = toGoid(cast(getVariable('internal_user_prefix') as bigint), user_id_backup);
ALTER TABLE internal_user_group DROP COLUMN user_id_backup;
ALTER TABLE internal_user_group ALTER COLUMN user_goid NOT NULL;


ALTER TABLE internal_user_group ADD COLUMN internal_group_backup bigint;
update internal_user_group set internal_group_backup = internal_group;
ALTER TABLE internal_user_group DROP COLUMN internal_group;
ALTER TABLE internal_user_group ADD COLUMN internal_group CHAR(16) FOR BIT DATA;
update internal_user_group set internal_group = toGoid(cast(getVariable('internal_group_prefix') as bigint), internal_group_backup);
ALTER TABLE internal_user_group DROP COLUMN internal_group_backup;
ALTER TABLE internal_user_group ALTER COLUMN internal_group NOT NULL;


ALTER TABLE logon_info ADD COLUMN provider_oid_backup bigint;
update logon_info set provider_oid_backup = provider_oid;
ALTER TABLE logon_info DROP COLUMN provider_oid;
ALTER TABLE logon_info ADD COLUMN provider_goid CHAR(16) FOR BIT DATA;
update logon_info set provider_goid = toGoid(cast(getVariable('identity_provider_prefix') as bigint), provider_oid_backup);
update logon_info set provider_goid = toGoid(0,-2) where provider_oid_backup = -2;
ALTER TABLE logon_info DROP COLUMN provider_oid_backup;

ALTER TABLE password_history ADD COLUMN internal_user_oid_backup bigint;
update password_history set internal_user_oid_backup = internal_user_oid;
ALTER TABLE password_history DROP COLUMN internal_user_oid;
ALTER TABLE password_history ADD COLUMN internal_user_goid CHAR(16) FOR BIT DATA;
update password_history set internal_user_goid = toGoid(cast(getVariable('internal_user_prefix') as bigint), internal_user_oid_backup);
update password_history set internal_user_goid = toGoid(0,-2) where internal_user_oid_backup = -2;
ALTER TABLE password_history DROP COLUMN internal_user_oid_backup;
ALTER TABLE password_history ALTER COLUMN internal_user_goid NOT NULL;
alter table password_history add constraint FKF16E7AF0C9B8DFC1 foreign key (internal_user_goid) references internal_user;

ALTER TABLE password_policy ADD COLUMN internal_identity_provider_oid_backup bigint;
update password_policy set internal_identity_provider_oid_backup = internal_identity_provider_oid;
ALTER TABLE password_policy DROP COLUMN internal_identity_provider_oid;
ALTER TABLE password_policy ADD COLUMN internal_identity_provider_goid CHAR(16) FOR BIT DATA;
update password_policy set internal_identity_provider_goid = toGoid(cast(getVariable('identity_provider_prefix') as bigint), internal_identity_provider_oid_backup);
update password_policy set internal_identity_provider_goid = toGoid(0,-2) where internal_identity_provider_oid_backup = -2;
ALTER TABLE password_policy DROP COLUMN internal_identity_provider_oid_backup;

ALTER TABLE policy_version ADD COLUMN user_provider_oid_backup bigint;
update policy_version set user_provider_oid_backup = user_provider_oid;
ALTER TABLE policy_version DROP COLUMN user_provider_oid;
ALTER TABLE policy_version ADD COLUMN user_provider_goid CHAR(16) FOR BIT DATA;
update policy_version set user_provider_goid = toGoid(cast(getVariable('identity_provider_prefix') as bigint), user_provider_oid_backup);
update policy_version set user_provider_goid = toGoid(0,-2) where user_provider_oid_backup = -2;
ALTER TABLE policy_version DROP COLUMN user_provider_oid_backup;

ALTER TABLE rbac_assignment ADD COLUMN provider_oid_backup bigint;
update rbac_assignment set provider_oid_backup = provider_oid;
ALTER TABLE rbac_assignment DROP COLUMN provider_oid;
ALTER TABLE rbac_assignment ADD COLUMN provider_goid CHAR(16) FOR BIT DATA;
update rbac_assignment set provider_goid = toGoid(cast(getVariable('identity_provider_prefix') as bigint), provider_oid_backup);
update rbac_assignment set provider_goid = toGoid(0,-2) where provider_oid_backup = -2;
ALTER TABLE rbac_assignment DROP COLUMN provider_oid_backup;
ALTER TABLE rbac_assignment ALTER COLUMN provider_goid NOT NULL;
ALTER TABLE rbac_assignment ADD UNIQUE (provider_goid, role_oid, identity_id, entity_type);

ALTER TABLE trusted_esm_user ADD COLUMN provider_oid_backup bigint;
update trusted_esm_user set provider_oid_backup = provider_oid;
ALTER TABLE trusted_esm_user DROP COLUMN provider_oid;
ALTER TABLE trusted_esm_user ADD COLUMN provider_goid CHAR(16) FOR BIT DATA;
update trusted_esm_user set provider_goid = toGoid(cast(getVariable('identity_provider_prefix') as bigint), provider_oid_backup);
update trusted_esm_user set provider_goid = toGoid(0,-2) where provider_oid_backup = -2;
ALTER TABLE trusted_esm_user DROP COLUMN provider_oid_backup;
ALTER TABLE trusted_esm_user ALTER COLUMN provider_goid NOT NULL;

ALTER TABLE wssc_session ADD COLUMN provider_oid_backup bigint;
update wssc_session set provider_oid_backup = provider_id;
ALTER TABLE wssc_session DROP COLUMN provider_id;
ALTER TABLE wssc_session ADD COLUMN provider_goid CHAR(16) FOR BIT DATA;
update wssc_session set provider_goid = toGoid(cast(getVariable('identity_provider_prefix') as bigint), provider_oid_backup);
update wssc_session set provider_goid = toGoid(0,-2) where provider_oid_backup = -2;
ALTER TABLE wssc_session DROP COLUMN provider_oid_backup;

-- Firewall rule

ALTER TABLE firewall_rule ADD COLUMN objectid_backup bigint;
update firewall_rule set objectid_backup = objectid;
ALTER TABLE firewall_rule DROP COLUMN objectid;
ALTER TABLE firewall_rule ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('firewall_rule_prefix', cast(randomLongNotReserved() as char(21)));
update firewall_rule set goid = toGoid(cast(getVariable('firewall_rule_prefix') as bigint), objectid_backup);
ALTER TABLE firewall_rule ALTER COLUMN goid NOT NULL;
ALTER TABLE firewall_rule DROP COLUMN objectid_backup;
ALTER TABLE firewall_rule ADD PRIMARY KEY (goid);

ALTER TABLE firewall_rule_property ADD COLUMN firewall_rule_goid CHAR(16) FOR BIT DATA;
update firewall_rule_property set firewall_rule_goid = toGoid(cast(getVariable('firewall_rule_prefix') as bigint), firewall_rule_oid);
ALTER TABLE firewall_rule_property ALTER COLUMN firewall_rule_goid NOT NULL;
ALTER TABLE firewall_rule_property DROP COLUMN firewall_rule_oid;
ALTER TABLE firewall_rule_property add constraint FK_FIREWALL_PROPERTY_GOID foreign key (firewall_rule_goid) references firewall_rule on delete cascade;

update rbac_role set entity_goid = toGoid(cast(getVariable('firewall_rule_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='FIREWALL_RULE';
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('firewall_rule_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'FIREWALL_RULE'), oid1.entity_id);

-- Encapsulated Assertion
ALTER TABLE encapsulated_assertion ADD COLUMN objectid_backup bigint;
update encapsulated_assertion set objectid_backup = objectid;
ALTER TABLE encapsulated_assertion DROP COLUMN objectid;
ALTER TABLE encapsulated_assertion ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('encass_prefix', cast(randomLongNotReserved() as char(21)));
update encapsulated_assertion set goid = toGoid(cast(getVariable('encass_prefix') as bigint), objectid_backup);
ALTER TABLE encapsulated_assertion ALTER COLUMN goid NOT NULL;
ALTER TABLE encapsulated_assertion DROP COLUMN objectid_backup;
ALTER TABLE encapsulated_assertion ADD PRIMARY KEY (goid);

ALTER TABLE encapsulated_assertion_property ADD COLUMN encapsulated_assertion_goid CHAR(16) FOR BIT DATA;
update encapsulated_assertion_property set encapsulated_assertion_goid = toGoid(cast(getVariable('encass_prefix') as bigint), encapsulated_assertion_oid);
ALTER TABLE encapsulated_assertion_property ALTER COLUMN encapsulated_assertion_goid NOT NULL;
ALTER TABLE encapsulated_assertion_property DROP COLUMN encapsulated_assertion_oid;
ALTER TABLE encapsulated_assertion_property add constraint FK_ENCASSPROP_ENCASS foreign key (encapsulated_assertion_goid) references encapsulated_assertion on delete cascade;

ALTER TABLE encapsulated_assertion_argument ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('encass_argument_prefix', cast(randomLongNotReserved() as char(21)));
update encapsulated_assertion_argument set goid = toGoid(cast(getVariable('encass_argument_prefix') as bigint), objectid);
ALTER TABLE encapsulated_assertion_argument ALTER COLUMN goid NOT NULL;
ALTER TABLE encapsulated_assertion_argument DROP COLUMN objectid;
ALTER TABLE encapsulated_assertion_argument ADD PRIMARY KEY (goid);

ALTER TABLE encapsulated_assertion_argument ADD COLUMN encapsulated_assertion_goid CHAR(16) FOR BIT DATA;
update encapsulated_assertion_argument set encapsulated_assertion_goid = toGoid(cast(getVariable('encass_prefix') as bigint), encapsulated_assertion_oid);
ALTER TABLE encapsulated_assertion_argument ALTER COLUMN encapsulated_assertion_goid NOT NULL;
ALTER TABLE encapsulated_assertion_argument DROP COLUMN encapsulated_assertion_oid;
ALTER TABLE encapsulated_assertion_argument add constraint FK_ENCASSARG_ENCASS foreign key (encapsulated_assertion_goid) references encapsulated_assertion on delete cascade;

ALTER TABLE encapsulated_assertion_result ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('encass_result_prefix', cast(randomLongNotReserved() as char(21)));
update encapsulated_assertion_result set goid = toGoid(cast(getVariable('encass_result_prefix') as bigint), objectid);
ALTER TABLE encapsulated_assertion_result ALTER COLUMN goid NOT NULL;
ALTER TABLE encapsulated_assertion_result DROP COLUMN objectid;
ALTER TABLE encapsulated_assertion_result ADD PRIMARY KEY (goid);

ALTER TABLE encapsulated_assertion_result ADD COLUMN encapsulated_assertion_goid CHAR(16) FOR BIT DATA;
update encapsulated_assertion_result set encapsulated_assertion_goid = toGoid(cast(getVariable('encass_prefix') as bigint), encapsulated_assertion_oid);
ALTER TABLE encapsulated_assertion_result ALTER COLUMN encapsulated_assertion_goid NOT NULL;
ALTER TABLE encapsulated_assertion_result DROP COLUMN encapsulated_assertion_oid;
ALTER TABLE encapsulated_assertion_result add constraint FK_ENCASSRES_ENCASS foreign key (encapsulated_assertion_goid) references encapsulated_assertion on delete cascade;

update rbac_role set entity_goid = toGoid(cast(getVariable('encass_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='ENCAPSULATED_ASSERTION';
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('encass_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'ENCAPSULATED_ASSERTION'), oid1.entity_id);

-- jms
ALTER TABLE jms_endpoint ADD COLUMN old_objectid bigint;
UPDATE  jms_endpoint SET old_objectid = objectid;
ALTER TABLE jms_endpoint DROP COLUMN objectid;
ALTER TABLE jms_endpoint ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('jms_endpoint_prefix', cast(randomLongNotReserved() as char(21)));
update jms_endpoint set goid = toGoid(cast(getVariable('jms_endpoint_prefix') as bigint), old_objectid);
ALTER TABLE jms_endpoint ALTER COLUMN goid NOT NULL;
ALTER TABLE jms_endpoint ADD PRIMARY KEY (goid);

ALTER TABLE jms_connection ADD COLUMN objectid_backup bigint;
update jms_connection set objectid_backup = objectid;
ALTER TABLE jms_connection DROP COLUMN objectid;
ALTER TABLE jms_connection ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('jms_connection_prefix', cast(randomLongNotReserved() as char(21)));
update jms_connection set goid = toGoid(cast(getVariable('jms_connection_prefix') as bigint), objectid_backup);
ALTER TABLE jms_connection ALTER COLUMN goid NOT NULL;
ALTER TABLE jms_connection DROP COLUMN objectid_backup;
ALTER TABLE jms_connection ADD PRIMARY KEY (goid);

ALTER TABLE jms_endpoint ADD COLUMN connection_goid CHAR(16) FOR BIT DATA;
update jms_endpoint set connection_goid = toGoid(cast(getVariable('jms_connection_prefix') as bigint), connection_oid);
ALTER TABLE jms_endpoint DROP COLUMN connection_oid;
ALTER TABLE jms_endpoint ALTER COLUMN connection_goid NOT NULL;

update rbac_role set entity_goid = toGoid(cast(getVariable('jms_connection_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='JMS_CONNECTION';
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('jms_connection_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'JMS_CONNECTION'), oid1.entity_id);

update rbac_role set entity_goid = toGoid(cast(getVariable('jms_endpoint_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='JMS_ENDPOINT';
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('jms_endpoint_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'JMS_ENDPOINT'), oid1.entity_id);

-- http configuration

ALTER TABLE http_configuration ADD COLUMN old_objectid bigint;
UPDATE  http_configuration SET old_objectid = objectid;
ALTER TABLE http_configuration DROP COLUMN objectid;
ALTER TABLE http_configuration ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('http_configuration_prefix', cast(randomLongNotReserved() as char(21)));
update http_configuration set goid = toGoid(cast(getVariable('http_configuration_prefix') as bigint), old_objectid);
ALTER TABLE http_configuration ALTER COLUMN goid NOT NULL;
ALTER TABLE http_configuration ADD PRIMARY KEY (goid);

update rbac_role set entity_goid = toGoid(cast(getVariable('http_configuration_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='HTTP_CONFIGURATION';
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('http_configuration_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'HTTP_CONFIGURATION'), oid1.entity_id);

-- active connectors

ALTER TABLE active_connector ADD COLUMN old_objectid bigint;
update active_connector set old_objectid = objectid;
ALTER TABLE active_connector DROP COLUMN objectid;
ALTER TABLE active_connector ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('active_connector_prefix', cast(randomLongNotReserved() as char(21)));
update active_connector set goid = toGoid(cast(getVariable('active_connector_prefix') as bigint), old_objectid);
ALTER TABLE active_connector ALTER COLUMN goid NOT NULL;
ALTER TABLE active_connector ADD PRIMARY KEY (goid);

ALTER TABLE active_connector_property ADD COLUMN connector_goid CHAR(16) FOR BIT DATA;
update active_connector_property set connector_goid = toGoid(cast(getVariable('active_connector_prefix') as bigint), connector_oid);
ALTER TABLE active_connector_property ALTER COLUMN connector_goid NOT NULL;
ALTER TABLE active_connector_property DROP COLUMN connector_oid;
ALTER TABLE active_connector_property add constraint FK58920F603AEA90B6 foreign key (connector_goid) references active_connector on delete cascade;
ALTER TABLE active_connector_property ADD PRIMARY KEY (connector_goid, name);

update rbac_role set entity_goid = toGoid(cast(getVariable('active_connector_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='SSG_ACTIVE_CONNECTOR';
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('active_connector_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'SSG_ACTIVE_CONNECTOR'), oid1.entity_id);

-- client_cert

ALTER TABLE client_cert ADD COLUMN old_objectid bigint;
update client_cert set old_objectid = objectid;
ALTER TABLE client_cert DROP COLUMN objectid;
ALTER TABLE client_cert ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('client_cert_prefix', cast(randomLongNotReserved() as char(21)));
update client_cert set goid = toGoid(cast(getVariable('client_cert_prefix') as bigint), old_objectid);
ALTER TABLE client_cert ALTER COLUMN goid NOT NULL;
ALTER TABLE client_cert ADD PRIMARY KEY (goid);
ALTER TABLE client_cert DROP COLUMN old_objectid;

-- trusted cert

ALTER TABLE trusted_cert ADD COLUMN old_objectid bigint;
update trusted_cert set old_objectid = objectid;
ALTER TABLE trusted_cert DROP COLUMN objectid;
ALTER TABLE trusted_cert ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('trusted_cert_prefix', cast(randomLongNotReserved() as char(21)));
update trusted_cert set goid = toGoid(cast(getVariable('trusted_cert_prefix') as bigint), old_objectid);
ALTER TABLE trusted_cert ALTER COLUMN goid NOT NULL;
ALTER TABLE trusted_cert ADD PRIMARY KEY (goid);
ALTER TABLE trusted_cert DROP COLUMN old_objectid;

ALTER TABLE trusted_esm ADD COLUMN trusted_cert_goid CHAR(16) FOR BIT DATA;
update trusted_esm set trusted_cert_goid = toGoid(cast(getVariable('trusted_cert_prefix') as bigint), trusted_cert_oid);
ALTER TABLE trusted_esm ALTER COLUMN trusted_cert_goid NOT NULL;
ALTER TABLE trusted_esm DROP COLUMN trusted_cert_oid;
ALTER TABLE trusted_esm add constraint FK_trusted_esm_trusted_cert foreign key (trusted_cert_goid) references trusted_cert;

update rbac_role set entity_goid = toGoid(cast(getVariable('trusted_cert_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='TRUSTED_CERT';
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('trusted_cert_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'TRUSTED_CERT'), oid1.entity_id);

-- revocation check policy

ALTER TABLE revocation_check_policy ADD COLUMN old_objectid bigint;
update revocation_check_policy set old_objectid = objectid;
ALTER TABLE revocation_check_policy DROP COLUMN objectid;
ALTER TABLE revocation_check_policy ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('revocation_check_policy_prefix', cast(randomLongNotReserved() as char(21)));
update revocation_check_policy set goid = toGoid(cast(getVariable('revocation_check_policy_prefix') as bigint), old_objectid);
ALTER TABLE revocation_check_policy ALTER COLUMN goid NOT NULL;
ALTER TABLE revocation_check_policy ADD PRIMARY KEY (goid);
ALTER TABLE revocation_check_policy DROP COLUMN old_objectid;

-- Note that old column name was revocation_policy_oid rather than revocation_check_policy_oid
ALTER TABLE trusted_cert ADD COLUMN revocation_check_policy_goid CHAR(16) FOR BIT DATA;
update trusted_cert set revocation_check_policy_goid = toGoid(cast(getVariable('revocation_check_policy_prefix') as bigint), revocation_policy_oid);
ALTER TABLE trusted_cert DROP COLUMN revocation_policy_oid;
ALTER TABLE trusted_cert add constraint FK_trusted_cert_revocation_check_policy foreign key (revocation_check_policy_goid) references revocation_check_policy;

update rbac_role set entity_goid = toGoid(cast(getVariable('revocation_check_policy_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='REVOCATION_CHECK_POLICY';
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('revocation_check_policy_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'REVOCATION_CHECK_POLICY'), oid1.entity_id);

-- services/policies/folders
ALTER TABLE folder ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('folder_prefix', cast(randomLongNotReserved() as char(21)));
update folder set goid = toGoid(cast(getVariable('folder_prefix') as bigint), objectid);
update folder set goid = toGoid(0, -5002) where goid = toGoid(cast(getVariable('folder_prefix') as bigint), -5002);
ALTER TABLE folder ALTER COLUMN goid NOT NULL;
ALTER TABLE folder DROP COLUMN objectid;
ALTER TABLE folder ADD PRIMARY KEY (goid);

ALTER TABLE folder ADD COLUMN parent_folder_goid CHAR(16) FOR BIT DATA;
update folder set parent_folder_goid = toGoid(cast(getVariable('folder_prefix') as bigint), parent_folder_oid);
update folder set parent_folder_goid = toGoid(0, -5002) where parent_folder_goid = toGoid(cast(getVariable('folder_prefix') as bigint), -5002);
ALTER TABLE folder DROP COLUMN parent_folder_oid;
ALTER TABLE folder add constraint FKB45D1C6EF8097918 foreign key (parent_folder_goid) references folder;

update rbac_role set entity_goid = toGoid(cast(getVariable('folder_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='FOLDER';
update rbac_role set entity_goid = toGoid(0, -5002) where entity_oid is not null and entity_type='FOLDER' and entity_goid = toGoid(cast(getVariable('folder_prefix') as bigint), -5002);
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('folder_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'FOLDER'), oid1.entity_id);
update rbac_predicate_oid set entity_id = goidToString(toGoid(0, -5002)) where entity_id = goidToString(toGoid(cast(getVariable('folder_prefix') as bigint), -5002));

ALTER TABLE policy ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('policy_prefix', cast(randomLongNotReserved() as char(21)));
update policy set goid = toGoid(cast(getVariable('policy_prefix') as bigint), objectid);
ALTER TABLE policy ALTER COLUMN goid NOT NULL;
ALTER TABLE policy DROP COLUMN objectid;
ALTER TABLE policy ADD PRIMARY KEY (goid);

ALTER TABLE policy ADD COLUMN folder_goid CHAR(16) FOR BIT DATA;
update policy set folder_goid = toGoid(cast(getVariable('folder_prefix') as bigint), folder_oid);
update policy set folder_goid = toGoid(0, -5002) where folder_goid = toGoid(cast(getVariable('folder_prefix') as bigint), -5002);
ALTER TABLE policy DROP COLUMN folder_oid;
ALTER TABLE policy add constraint FKC56DA532DB935A63 foreign key (folder_goid) references folder;

update rbac_role set entity_goid = toGoid(cast(getVariable('policy_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='POLICY';
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('policy_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'POLICY'), oid1.entity_id);

ALTER TABLE policy_alias ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('policy_alias_prefix', cast(randomLongNotReserved() as char(21)));
update policy_alias set goid = toGoid(cast(getVariable('policy_alias_prefix') as bigint), objectid);
ALTER TABLE policy_alias ALTER COLUMN goid NOT NULL;
ALTER TABLE policy_alias DROP COLUMN objectid;
ALTER TABLE policy_alias ADD PRIMARY KEY (goid);

ALTER TABLE policy_alias ADD COLUMN folder_goid CHAR(16) FOR BIT DATA;
update policy_alias set folder_goid = toGoid(cast(getVariable('folder_prefix') as bigint), folder_oid);
update policy_alias set folder_goid = toGoid(0, -5002) where folder_goid = toGoid(cast(getVariable('folder_prefix') as bigint), -5002);
ALTER TABLE policy_alias DROP COLUMN folder_oid;
ALTER TABLE policy_alias add constraint FKA07B7103DB935A63 foreign key (folder_goid) references folder on delete cascade;

ALTER TABLE policy_alias ADD COLUMN policy_goid CHAR(16) FOR BIT DATA;
update policy_alias set policy_goid = toGoid(cast(getVariable('policy_prefix') as bigint), policy_oid);
ALTER TABLE policy_alias DROP COLUMN policy_oid;

update rbac_role set entity_goid = toGoid(cast(getVariable('policy_alias_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='POLICY_ALIAS';
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('policy_alias_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'POLICY_ALIAS'), oid1.entity_id);

ALTER TABLE policy_version ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('policy_version_prefix', cast(randomLongNotReserved() as char(21)));
update policy_version set goid = toGoid(cast(getVariable('policy_version_prefix') as bigint), objectid);
ALTER TABLE policy_version ALTER COLUMN goid NOT NULL;
ALTER TABLE policy_version DROP COLUMN objectid;
ALTER TABLE policy_version ADD PRIMARY KEY (goid);

ALTER TABLE policy_version ADD COLUMN policy_goid CHAR(16) FOR BIT DATA;
update policy_version set policy_goid = toGoid(cast(getVariable('policy_prefix') as bigint), policy_oid);
ALTER TABLE policy_version DROP COLUMN policy_oid;

update rbac_role set entity_goid = toGoid(cast(getVariable('policy_version_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='POLICY_VERSION';
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('policy_version_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'POLICY_VERSION'), oid1.entity_id);

ALTER TABLE published_service ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('published_service_prefix', cast(randomLongNotReserved() as char(21)));
update published_service set goid = toGoid(cast(getVariable('published_service_prefix') as bigint), objectid);
ALTER TABLE published_service ALTER COLUMN goid NOT NULL;
ALTER TABLE published_service DROP COLUMN objectid;
ALTER TABLE published_service ADD PRIMARY KEY (goid);

ALTER TABLE published_service ADD COLUMN folder_goid CHAR(16) FOR BIT DATA;
update published_service set folder_goid = toGoid(cast(getVariable('folder_prefix') as bigint), folder_oid);
update published_service set folder_goid = toGoid(0, -5002) where folder_goid = toGoid(cast(getVariable('folder_prefix') as bigint), -5002);
ALTER TABLE published_service DROP COLUMN folder_oid;
ALTER TABLE published_service add constraint FK25874164DB935A63 foreign key (folder_goid) references folder;

ALTER TABLE published_service ADD COLUMN policy_goid CHAR(16) FOR BIT DATA;
update published_service set policy_goid = toGoid(cast(getVariable('policy_prefix') as bigint), policy_oid);
ALTER TABLE published_service DROP COLUMN policy_oid;
ALTER TABLE published_service add constraint FK25874164DAFA444B foreign key (policy_goid) references policy;

update rbac_role set entity_goid = toGoid(cast(getVariable('published_service_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='SERVICE';
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('published_service_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'SERVICE'), oid1.entity_id);

ALTER TABLE published_service_alias ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('published_service_alias_prefix', cast(randomLongNotReserved() as char(21)));
update published_service_alias set goid = toGoid(cast(getVariable('published_service_alias_prefix') as bigint), objectid);
ALTER TABLE published_service_alias ALTER COLUMN goid NOT NULL;
ALTER TABLE published_service_alias DROP COLUMN objectid;
ALTER TABLE published_service_alias ADD PRIMARY KEY (goid);

ALTER TABLE published_service_alias ADD COLUMN folder_goid CHAR(16) FOR BIT DATA;
update published_service_alias set folder_goid = toGoid(cast(getVariable('folder_prefix') as bigint), folder_oid);
update published_service_alias set folder_goid = toGoid(0, -5002) where folder_goid = toGoid(cast(getVariable('folder_prefix') as bigint), -5002);
ALTER TABLE published_service_alias DROP COLUMN folder_oid;
ALTER TABLE published_service_alias add constraint FK6AE79FB5DB935A63 foreign key (folder_goid) references folder on delete cascade;

ALTER TABLE published_service_alias ADD COLUMN published_service_goid CHAR(16) FOR BIT DATA;
update published_service_alias set published_service_goid = toGoid(cast(getVariable('published_service_prefix') as bigint), published_service_oid);
ALTER TABLE published_service_alias DROP COLUMN published_service_oid;

update rbac_role set entity_goid = toGoid(cast(getVariable('published_service_alias_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='SERVICE_ALIAS';
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('published_service_alias_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'SERVICE_ALIAS'), oid1.entity_id);

ALTER TABLE audit_message ADD COLUMN service_goid CHAR(16) FOR BIT DATA;
update audit_message set service_goid = toGoid(cast(getVariable('published_service_prefix') as bigint), service_oid);
ALTER TABLE audit_message DROP COLUMN service_oid;

ALTER TABLE active_connector ADD COLUMN hardwired_service_goid CHAR(16) FOR BIT DATA;
update active_connector set hardwired_service_goid = toGoid(cast(getVariable('published_service_prefix') as bigint), hardwired_service_oid);
ALTER TABLE active_connector DROP COLUMN hardwired_service_oid;

ALTER TABLE rbac_predicate_folder ADD COLUMN folder_goid CHAR(16) FOR BIT DATA;
update rbac_predicate_folder set folder_goid = toGoid(cast(getVariable('folder_prefix') as bigint), folder_oid);
update rbac_predicate_folder set folder_goid = toGoid(0, -5002) where folder_goid = toGoid(cast(getVariable('folder_prefix') as bigint), -5002);
ALTER TABLE rbac_predicate_folder ALTER COLUMN folder_goid NOT NULL;
alter table rbac_predicate_folder add constraint FKF111A643DB935A63 foreign key (folder_goid) references folder on delete cascade;
ALTER TABLE rbac_predicate_folder DROP COLUMN folder_oid;

ALTER TABLE sample_messages ADD COLUMN published_service_goid CHAR(16) FOR BIT DATA;
update sample_messages set published_service_goid = toGoid(cast(getVariable('published_service_prefix') as bigint), published_service_oid);
ALTER TABLE sample_messages DROP COLUMN published_service_oid;

ALTER TABLE service_documents ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('service_documents_prefix', cast(randomLongNotReserved() as char(21)));
update service_documents set goid = toGoid(cast(getVariable('service_documents_prefix') as bigint), objectid);
ALTER TABLE service_documents ALTER COLUMN goid NOT NULL;
ALTER TABLE service_documents DROP COLUMN objectid;
ALTER TABLE service_documents ADD PRIMARY KEY (goid);

update rbac_role set entity_goid = toGoid(cast(getVariable('service_documents_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='SERVICE_DOCUMENT';
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('service_documents_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'SERVICE_DOCUMENT'), oid1.entity_id);

ALTER TABLE service_documents ADD COLUMN service_goid CHAR(16) FOR BIT DATA;
update service_documents set service_goid = toGoid(cast(getVariable('published_service_prefix') as bigint), service_oid);
ALTER TABLE service_documents DROP COLUMN service_oid;

ALTER TABLE service_metrics ADD COLUMN published_service_goid CHAR(16) FOR BIT DATA;
update service_metrics set published_service_goid = toGoid(cast(getVariable('published_service_prefix') as bigint), published_service_oid);
ALTER TABLE service_metrics DROP COLUMN published_service_oid;

ALTER TABLE service_usage ADD COLUMN serviceid_backup bigint;
update service_usage set serviceid_backup = serviceid;
ALTER TABLE service_usage DROP COLUMN serviceid;
ALTER TABLE service_usage ADD COLUMN serviceid CHAR(16) FOR BIT DATA;
update service_usage set serviceid = toGoid(cast(getVariable('published_service_prefix') as bigint), serviceid_backup);
ALTER TABLE service_usage ALTER COLUMN serviceid NOT NULL;
ALTER TABLE service_usage DROP COLUMN serviceid_backup;
ALTER TABLE service_usage ADD PRIMARY KEY (serviceid, nodeid);

update rbac_role set entity_goid = toGoid(cast(getVariable('published_service_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='SERVICE_USAGE';
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('published_service_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'SERVICE_USAGE'), oid1.entity_id);

ALTER TABLE uddi_business_service_status ADD COLUMN published_service_goid CHAR(16) FOR BIT DATA;
update uddi_business_service_status set published_service_goid = toGoid(cast(getVariable('published_service_prefix') as bigint), published_service_oid);
ALTER TABLE uddi_business_service_status DROP COLUMN published_service_oid;

ALTER TABLE uddi_proxied_service_info ADD COLUMN published_service_goid CHAR(16) FOR BIT DATA;
update uddi_proxied_service_info set published_service_goid = toGoid(cast(getVariable('published_service_prefix') as bigint), published_service_oid);
ALTER TABLE uddi_proxied_service_info DROP COLUMN published_service_oid;

ALTER TABLE uddi_service_control ADD COLUMN published_service_goid CHAR(16) FOR BIT DATA;
update uddi_service_control set published_service_goid = toGoid(cast(getVariable('published_service_prefix') as bigint), published_service_oid);
ALTER TABLE uddi_service_control DROP COLUMN published_service_oid;

ALTER TABLE wsdm_subscription ADD COLUMN published_service_goid CHAR(16) FOR BIT DATA;
update wsdm_subscription set published_service_goid = toGoid(cast(getVariable('published_service_prefix') as bigint), published_service_oid);
ALTER TABLE wsdm_subscription ALTER COLUMN published_service_goid NOT NULL;
ALTER TABLE wsdm_subscription DROP COLUMN published_service_oid;

ALTER TABLE wsdm_subscription ADD COLUMN esm_service_goid CHAR(16) FOR BIT DATA;
update wsdm_subscription set esm_service_goid = toGoid(cast(getVariable('published_service_prefix') as bigint), esm_service_oid);
ALTER TABLE wsdm_subscription ALTER COLUMN esm_service_goid NOT NULL;
ALTER TABLE wsdm_subscription DROP COLUMN esm_service_oid;

ALTER TABLE encapsulated_assertion ADD COLUMN policy_goid CHAR(16) FOR BIT DATA;
update encapsulated_assertion set policy_goid = toGoid(cast(getVariable('policy_prefix') as bigint), policy_oid);
ALTER TABLE encapsulated_assertion ALTER COLUMN policy_goid NOT NULL;
ALTER TABLE encapsulated_assertion DROP COLUMN policy_oid;
alter table encapsulated_assertion add constraint FK_ENCASS_POL foreign key (policy_goid) references policy;

update rbac_predicate_attribute set value = goidToString(toGoid(cast(getVariable('published_service_prefix') as bigint), cast(value as bigint))) where attribute='serviceOid' OR attribute='publishedServiceOid' OR attribute='serviceid';
update rbac_predicate_attribute set attribute = 'serviceGoid' where attribute='serviceOid';
update rbac_predicate_attribute set attribute = 'publishedServiceGoid' where attribute='publishedServiceOid';

update rbac_predicate_entityfolder set entity_id = goidToString(toGoid(cast(getVariable('published_service_prefix') as bigint), cast(entity_id as bigint))) where entity_type='SERVICE';
update rbac_predicate_entityfolder set entity_id = goidToString(toGoid(cast(getVariable('folder_prefix') as bigint), cast(entity_id as bigint))) where entity_type='FOLDER';
update rbac_predicate_entityfolder set entity_id = goidToString(toGoid(0, -5002)) where entity_type='FOLDER' and entity_id=goidToString(toGoid(cast(getVariable('folder_prefix') as bigint), -5002));
update rbac_predicate_entityfolder set entity_id = goidToString(toGoid(cast(getVariable('policy_prefix') as bigint), cast(entity_id as bigint))) where entity_type='POLICY';

-- UDDI

ALTER TABLE uddi_registries ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('uddi_registries_prefix', cast(randomLongNotReserved() as char(21)));
update uddi_registries set goid = toGoid(cast(getVariable('uddi_registries_prefix') as bigint), objectid);
ALTER TABLE uddi_registries ALTER COLUMN goid NOT NULL;
ALTER TABLE uddi_registries DROP COLUMN objectid;
ALTER TABLE uddi_registries ADD PRIMARY KEY (goid);

update rbac_role set entity_goid = toGoid(cast(getVariable('uddi_registries_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='UDDI_REGISTRY';
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('uddi_registries_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'UDDI_REGISTRY'), oid1.entity_id);


ALTER TABLE uddi_registry_subscription ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('uddi_registry_subscription_prefix', cast(randomLongNotReserved() as char(21)));
update uddi_registry_subscription set goid = toGoid(cast(getVariable('uddi_registry_subscription_prefix') as bigint), objectid);
ALTER TABLE uddi_registry_subscription ALTER COLUMN goid NOT NULL;
ALTER TABLE uddi_registry_subscription DROP COLUMN objectid;
ALTER TABLE uddi_registry_subscription ADD PRIMARY KEY (goid);

ALTER TABLE uddi_registry_subscription ADD COLUMN uddi_registry_goid CHAR(16) FOR BIT DATA;
update uddi_registry_subscription set uddi_registry_goid = toGoid(cast(getVariable('uddi_registries_prefix') as bigint), uddi_registry_oid);
ALTER TABLE uddi_registry_subscription DROP COLUMN uddi_registry_oid;


ALTER TABLE uddi_proxied_service_info ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('uddi_proxied_service_info_prefix', cast(randomLongNotReserved() as char(21)));
update uddi_proxied_service_info set goid = toGoid(cast(getVariable('uddi_proxied_service_info_prefix') as bigint), objectid);
ALTER TABLE uddi_proxied_service_info ALTER COLUMN goid NOT NULL;
ALTER TABLE uddi_proxied_service_info DROP COLUMN objectid;
ALTER TABLE uddi_proxied_service_info ADD PRIMARY KEY (goid);

ALTER TABLE uddi_proxied_service_info ADD COLUMN uddi_registry_goid CHAR(16) FOR BIT DATA;
update uddi_proxied_service_info set uddi_registry_goid = toGoid(cast(getVariable('uddi_registries_prefix') as bigint), uddi_registry_oid);
ALTER TABLE uddi_proxied_service_info DROP COLUMN uddi_registry_oid;

update rbac_role set entity_goid = toGoid(cast(getVariable('uddi_proxied_service_info_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='UDDI_PROXIED_SERVICE_INFO';
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('uddi_proxied_service_info_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'UDDI_PROXIED_SERVICE_INFO'), oid1.entity_id);


ALTER TABLE uddi_proxied_service ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('uddi_proxied_service_prefix', cast(randomLongNotReserved() as char(21)));
update uddi_proxied_service set goid = toGoid(cast(getVariable('uddi_proxied_service_prefix') as bigint), objectid);
ALTER TABLE uddi_proxied_service ALTER COLUMN goid NOT NULL;
ALTER TABLE uddi_proxied_service DROP COLUMN objectid;
ALTER TABLE uddi_proxied_service ADD PRIMARY KEY (goid);

ALTER TABLE uddi_proxied_service ADD COLUMN uddi_proxied_service_info_goid CHAR(16) FOR BIT DATA;
update uddi_proxied_service set uddi_proxied_service_info_goid = toGoid(cast(getVariable('uddi_proxied_service_info_prefix') as bigint), uddi_proxied_service_info_oid);
ALTER TABLE uddi_proxied_service ALTER COLUMN uddi_proxied_service_info_goid NOT NULL;
ALTER TABLE uddi_proxied_service DROP COLUMN uddi_proxied_service_info_oid;
ALTER TABLE uddi_proxied_service add constraint FK127C390874249C8B foreign key (uddi_proxied_service_info_goid) references uddi_proxied_service_info on delete cascade;


ALTER TABLE uddi_publish_status ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('uddi_publish_status_prefix', cast(randomLongNotReserved() as char(21)));
update uddi_publish_status set goid = toGoid(cast(getVariable('uddi_publish_status_prefix') as bigint), objectid);
ALTER TABLE uddi_publish_status ALTER COLUMN goid NOT NULL;
ALTER TABLE uddi_publish_status DROP COLUMN objectid;
ALTER TABLE uddi_publish_status ADD PRIMARY KEY (goid);

ALTER TABLE uddi_publish_status ADD COLUMN uddi_proxied_service_info_goid CHAR(16) FOR BIT DATA;
update uddi_publish_status set uddi_proxied_service_info_goid = toGoid(cast(getVariable('uddi_proxied_service_info_prefix') as bigint), uddi_proxied_service_info_oid);
ALTER TABLE uddi_publish_status ALTER COLUMN uddi_proxied_service_info_goid NOT NULL;
ALTER TABLE uddi_publish_status DROP COLUMN uddi_proxied_service_info_oid;


ALTER TABLE uddi_business_service_status ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('uddi_business_service_status_prefix', cast(randomLongNotReserved() as char(21)));
update uddi_business_service_status set goid = toGoid(cast(getVariable('uddi_business_service_status_prefix') as bigint), objectid);
ALTER TABLE uddi_business_service_status ALTER COLUMN goid NOT NULL;
ALTER TABLE uddi_business_service_status DROP COLUMN objectid;
ALTER TABLE uddi_business_service_status ADD PRIMARY KEY (goid);

ALTER TABLE uddi_business_service_status ADD COLUMN uddi_registry_goid CHAR(16) FOR BIT DATA;
update uddi_business_service_status set uddi_registry_goid = toGoid(cast(getVariable('uddi_registries_prefix') as bigint), uddi_registry_oid);
ALTER TABLE uddi_business_service_status DROP COLUMN uddi_registry_oid;


ALTER TABLE uddi_service_control ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('uddi_service_control_prefix', cast(randomLongNotReserved() as char(21)));
update uddi_service_control set goid = toGoid(cast(getVariable('uddi_service_control_prefix') as bigint), objectid);
ALTER TABLE uddi_service_control ALTER COLUMN goid NOT NULL;
ALTER TABLE uddi_service_control DROP COLUMN objectid;
ALTER TABLE uddi_service_control ADD PRIMARY KEY (goid);

ALTER TABLE uddi_service_control ADD COLUMN uddi_registry_goid CHAR(16) FOR BIT DATA;
update uddi_service_control set uddi_registry_goid = toGoid(cast(getVariable('uddi_registries_prefix') as bigint), uddi_registry_oid);
ALTER TABLE uddi_service_control DROP COLUMN uddi_registry_oid;

update rbac_role set entity_goid = toGoid(cast(getVariable('uddi_service_control_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='UDDI_SERVICE_CONTROL';
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('uddi_service_control_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'UDDI_SERVICE_CONTROL'), oid1.entity_id);


ALTER TABLE uddi_service_control_monitor_runtime ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('uddi_service_control_monitor_runtime_prefix', cast(randomLongNotReserved() as char(21)));
update uddi_service_control_monitor_runtime set goid = toGoid(cast(getVariable('uddi_service_control_monitor_runtime_prefix') as bigint), objectid);
ALTER TABLE uddi_service_control_monitor_runtime ALTER COLUMN goid NOT NULL;
ALTER TABLE uddi_service_control_monitor_runtime DROP COLUMN objectid;
ALTER TABLE uddi_service_control_monitor_runtime ADD PRIMARY KEY (goid);

ALTER TABLE uddi_service_control_monitor_runtime ADD COLUMN uddi_service_control_goid CHAR(16) FOR BIT DATA;
update uddi_service_control_monitor_runtime set uddi_service_control_goid = toGoid(cast(getVariable('uddi_service_control_prefix') as bigint), uddi_service_control_oid);
ALTER TABLE uddi_service_control_monitor_runtime DROP COLUMN uddi_service_control_oid;

-- RBAC role

call setVariable('rbac_role_prefix', cast(randomLongNotReserved() as char(21)));

ALTER TABLE rbac_role ADD COLUMN goid CHAR(16) FOR BIT DATA;
update rbac_role set goid = toGoid(cast(getVariable('rbac_role_prefix') as bigint), objectid);
ALTER TABLE rbac_role ALTER COLUMN goid NOT NULL;
ALTER TABLE rbac_role DROP COLUMN objectid;
ALTER TABLE rbac_role ADD PRIMARY KEY (goid);

update rbac_role set entity_goid = toGoid(cast(getVariable('rbac_role_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='RBAC_ROLE';
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('rbac_role_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'RBAC_ROLE'), oid1.entity_id);

-- RBAC role assignment

call setVariable('rbac_assignment_prefix', cast(randomLongNotReserved() as char(21)));

ALTER TABLE rbac_assignment ADD COLUMN goid CHAR(16) FOR BIT DATA;
update rbac_assignment set goid = toGoid(cast(getVariable('rbac_assignment_prefix') as bigint), objectid);
ALTER TABLE rbac_assignment ALTER COLUMN goid NOT NULL;
ALTER TABLE rbac_assignment DROP COLUMN objectid;
ALTER TABLE rbac_assignment ADD PRIMARY KEY (goid);

ALTER TABLE rbac_assignment ADD COLUMN role_goid CHAR(16) FOR BIT DATA;
update rbac_assignment set role_goid = toGoid(cast(getVariable('rbac_assignment_prefix') as bigint), role_oid);
ALTER TABLE rbac_assignment ALTER COLUMN role_goid NOT NULL;
ALTER TABLE rbac_assignment DROP COLUMN role_oid;

-- RBAC permission

call setVariable('rbac_permission_prefix', cast(randomLongNotReserved() as char(21)));

ALTER TABLE rbac_permission ADD COLUMN goid CHAR(16) FOR BIT DATA;
update rbac_permission set goid = toGoid(cast(getVariable('rbac_permission_prefix') as bigint), objectid);
ALTER TABLE rbac_permission ALTER COLUMN goid NOT NULL;
ALTER TABLE rbac_permission DROP COLUMN objectid;
ALTER TABLE rbac_permission ADD PRIMARY KEY (goid);

ALTER TABLE rbac_permission ADD COLUMN role_goid CHAR(16) FOR BIT DATA;
update rbac_permission set role_goid = toGoid(cast(getVariable('rbac_assignment_prefix') as bigint), role_oid);
ALTER TABLE rbac_permission ALTER COLUMN role_goid NOT NULL;
ALTER TABLE rbac_permission DROP COLUMN role_oid;

-- RBAC scope predicates

call setVariable('rbac_predicate_prefix', cast(randomLongNotReserved() as char(21)));

ALTER TABLE rbac_predicate ADD COLUMN goid CHAR(16) FOR BIT DATA;
update rbac_predicate set goid = toGoid(cast(getVariable('rbac_predicate_prefix') as bigint), objectid);
ALTER TABLE rbac_predicate ALTER COLUMN goid NOT NULL;
ALTER TABLE rbac_predicate DROP COLUMN objectid;
ALTER TABLE rbac_predicate ADD PRIMARY KEY (goid);

ALTER TABLE rbac_predicate ADD COLUMN permission_goid CHAR(16) FOR BIT DATA;
update rbac_predicate set permission_goid = toGoid(cast(getVariable('rbac_assignment_prefix') as bigint), permission_oid);
ALTER TABLE rbac_predicate ALTER COLUMN permission_goid NOT NULL;
ALTER TABLE rbac_predicate DROP COLUMN permission_oid;

ALTER TABLE rbac_predicate_attribute ADD COLUMN goid CHAR(16) FOR BIT DATA;
update rbac_predicate_attribute set goid = toGoid(cast(getVariable('rbac_predicate_prefix') as bigint), objectid);
ALTER TABLE rbac_predicate_attribute ALTER COLUMN goid NOT NULL;
ALTER TABLE rbac_predicate_attribute DROP COLUMN objectid;
ALTER TABLE rbac_predicate_attribute ADD PRIMARY KEY (goid);

ALTER TABLE rbac_predicate_security_zone ADD COLUMN goid CHAR(16) FOR BIT DATA;
update rbac_predicate_security_zone set goid = toGoid(cast(getVariable('rbac_predicate_prefix') as bigint), objectid);
ALTER TABLE rbac_predicate_security_zone ALTER COLUMN goid NOT NULL;
ALTER TABLE rbac_predicate_security_zone DROP COLUMN objectid;
ALTER TABLE rbac_predicate_security_zone ADD PRIMARY KEY (goid);

ALTER TABLE rbac_predicate_oid ADD COLUMN goid CHAR(16) FOR BIT DATA;
update rbac_predicate_oid set goid = toGoid(cast(getVariable('rbac_predicate_prefix') as bigint), objectid);
ALTER TABLE rbac_predicate_oid ALTER COLUMN goid NOT NULL;
ALTER TABLE rbac_predicate_oid DROP COLUMN objectid;
ALTER TABLE rbac_predicate_oid ADD PRIMARY KEY (goid);

ALTER TABLE rbac_predicate_folder ADD COLUMN goid CHAR(16) FOR BIT DATA;
update rbac_predicate_folder set goid = toGoid(cast(getVariable('rbac_predicate_prefix') as bigint), objectid);
ALTER TABLE rbac_predicate_folder ALTER COLUMN goid NOT NULL;
ALTER TABLE rbac_predicate_folder DROP COLUMN objectid;
ALTER TABLE rbac_predicate_folder ADD PRIMARY KEY (goid);

ALTER TABLE rbac_predicate_entityfolder ADD COLUMN goid CHAR(16) FOR BIT DATA;
update rbac_predicate_entityfolder set goid = toGoid(cast(getVariable('rbac_predicate_prefix') as bigint), objectid);
ALTER TABLE rbac_predicate_entityfolder ALTER COLUMN goid NOT NULL;
ALTER TABLE rbac_predicate_entityfolder DROP COLUMN objectid;
ALTER TABLE rbac_predicate_entityfolder ADD PRIMARY KEY (goid);

-- RBAC foreign key constraints
alter table rbac_assignment add constraint FK51FEC6DACCD6DF3E foreign key (role_goid) references rbac_role;
alter table rbac_assignment add unique (provider_goid, role_goid, identity_id, entity_type);
alter table rbac_permission add constraint FKF5F905DCCCD6DF3E foreign key (role_goid) references rbac_role on delete cascade;
alter table rbac_predicate add constraint FKB894B40A45FC8430 foreign key (permission_goid) references rbac_permission on delete cascade;
alter table rbac_predicate_attribute add constraint FK563B54A7918005E4 foreign key (goid) references rbac_predicate on delete cascade;
alter table rbac_predicate_entityfolder add constraint FK6AE46026918005E4 foreign key (goid) references rbac_predicate on delete cascade;
alter table rbac_predicate_folder add constraint FKF111A643918005E4 foreign key (goid) references rbac_predicate on delete cascade;
alter table rbac_predicate_oid add constraint FK37D47C15918005E4 foreign key (goid) references rbac_predicate on delete cascade;
alter table rbac_predicate_security_zone add constraint FK_predicate_goid foreign key (goid) references rbac_predicate on delete cascade;

-- WSSC_SESSION
ALTER TABLE wssc_session ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('wssc_session_prefix', cast(randomLongNotReserved() as char(21)));
update wssc_session set goid = toGoid(cast(getVariable('wssc_session_prefix') as bigint), objectid);
ALTER TABLE wssc_session ALTER COLUMN goid NOT NULL;
ALTER TABLE wssc_session DROP PRIMARY KEY;
ALTER TABLE wssc_session DROP COLUMN objectid;
ALTER TABLE wssc_session ADD PRIMARY KEY (goid);

--
-- Register upgrade task for upgrading sink configuration references to GOIDs
--
INSERT INTO cluster_properties
    (goid, version, propkey, propvalue, properties) values
           (toGoid(0,-800001), 0, 'upgrade.task.800001', 'com.l7tech.server.upgrade.Upgrade71To80SinkConfig', null),
           (toGoid(0,-800002), 0, 'upgrade.task.800002', 'com.l7tech.server.upgrade.Upgrade71To80IdReferences', null),
           (toGoid(0,-800003), 0, 'upgrade.task.800003', 'com.l7tech.server.upgrade.Upgrade71To80IdProviderReferences', null),
           (toGoid(0,-800004), 0, 'upgrade.task.800004', 'com.l7tech.server.upgrade.Upgrade71To80AuditRecords', null);


--
-- License documents for updated licensing model
--

CREATE TABLE license_document (
  goid CHAR(16) FOR BIT DATA NOT NULL,
  version integer NOT NULL,
  contents clob(2147483647),
  PRIMARY KEY (goid)
);


CREATE TABLE goid_upgrade_map (
  prefix bigint NOT NULL,
  table_name varchar(255) NOT NULL,
  PRIMARY KEY (prefix, table_name)
);

INSERT INTO goid_upgrade_map (table_name, prefix) VALUES
      ('jdbc_connection', cast(getVariable('jdbc_connection_prefix') as bigint)),
      ('logon_info', cast(getVariable('logon_info_prefix') as bigint)),
      ('sample_messages', cast(getVariable('sample_messages_prefix') as bigint)),
      ('cluster_properties', cast(getVariable('cluster_properties_prefix') as bigint)),
      ('email_listener', cast(getVariable('email_listener_prefix') as bigint)),
      ('email_listener_state', cast(getVariable('email_listener_state_prefix') as bigint)),
      ('generic_entity', cast(getVariable('generic_entity_prefix') as bigint)),
      ('connector', cast(getVariable('connector_prefix') as bigint)),
      ('identity_provider', cast(getVariable('identity_provider_prefix') as bigint)),
      ('internal_group', cast(getVariable('internal_group_prefix') as bigint)),
      ('internal_user', cast(getVariable('internal_user_prefix') as bigint)),
      ('internal_user_group', cast(getVariable('internal_user_group_prefix') as bigint)),
      ('fed_user', cast(getVariable('fed_user_prefix') as bigint)),
      ('fed_group', cast(getVariable('fed_group_prefix') as bigint)),
      ('fed_user_group', cast(getVariable('fed_user_group_prefix') as bigint)),
      ('firewall_rule', cast(getVariable('firewall_rule_prefix') as bigint)),
      ('encapsulated_assertion', cast(getVariable('encass_prefix') as bigint)),
      ('encapsulated_assertion_argument', cast(getVariable('encass_argument_prefix') as bigint)),
      ('encapsulated_assertion_result', cast(getVariable('encass_result_prefix') as bigint)),
      ('jms_connection', cast(getVariable('jms_connection_prefix') as bigint)),
      ('jms_endpoint', cast(getVariable('jms_endpoint_prefix') as bigint)),
      ('http_configuration', cast(getVariable('http_configuration_prefix') as bigint)),
      ('active_connector', cast(getVariable('active_connector_prefix') as bigint)),
      ('folder', cast(getVariable('folder_prefix') as bigint)),
      ('policy', cast(getVariable('policy_prefix') as bigint)),
      ('policy_alias', cast(getVariable('policy_alias_prefix') as bigint)),
      ('policy_version', cast(getVariable('policy_version_prefix') as bigint)),
      ('published_service', cast(getVariable('published_service_prefix') as bigint)),
      ('published_service_alias', cast(getVariable('published_service_alias_prefix') as bigint)),
      ('service_documents', cast(getVariable('service_documents_prefix') as bigint)),
      ('uddi_registries', cast(getVariable('uddi_registries_prefix') as bigint)),
      ('uddi_registry_subscription', cast(getVariable('uddi_registry_subscription_prefix') as bigint)),
      ('uddi_proxied_service_info', cast(getVariable('uddi_proxied_service_info_prefix') as bigint)),
      ('uddi_proxied_service', cast(getVariable('uddi_proxied_service_prefix') as bigint)),
      ('uddi_publish_status', cast(getVariable('uddi_publish_status_prefix') as bigint)),
      ('uddi_business_service_status', cast(getVariable('uddi_business_service_status_prefix') as bigint)),
      ('uddi_service_control', cast(getVariable('uddi_service_control_prefix') as bigint)),
      ('uddi_service_control_monitor_runtime', cast(getVariable('uddi_service_control_monitor_runtime_prefix') as bigint)),
      ('rbac_role', cast(getVariable('rbac_role_prefix') as bigint)),
      ('rbac_assignment', cast(getVariable('rbac_assignment_prefix') as bigint)),
      ('rbac_permission', cast(getVariable('rbac_permission_prefix') as bigint)),
      ('rbac_predicate', cast(getVariable('rbac_predicate_prefix') as bigint)),
      ('client_cert', cast(getVariable('client_cert_prefix') as bigint)),
      ('trusted_cert', cast(getVariable('trusted_cert_prefix') as bigint)),
      ('revocation_check_policy', cast(getVariable('revocation_check_policy_prefix') as bigint)),
      ('resource_entry', cast(getVariable('resource_entry_prefix') as bigint)),
      ('secure_password', cast(getVariable('secure_password_prefix') as bigint)),
      ('wssc_session', cast(getVariable('wssc_session_prefix') as bigint));

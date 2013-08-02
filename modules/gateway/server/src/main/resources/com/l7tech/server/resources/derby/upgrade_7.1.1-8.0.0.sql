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
  objectid bigint not null,
  version integer,
  name varchar(255) not null unique,
  security_zone_goid CHAR(16) FOR BIT DATA references security_zone(goid) on delete set null,
  primary key (objectid)
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
INSERT INTO rbac_permission VALUES (-1501,0,-1500,'READ',NULL,'SITEMINDER_CONFIGURATION');
INSERT INTO rbac_permission VALUES (-1502,0,-1500,'CREATE',NULL,'SITEMINDER_CONFIGURATION');
INSERT INTO rbac_permission VALUES (-1503,0,-1500,'UPDATE',NULL,'SITEMINDER_CONFIGURATION');
INSERT INTO rbac_permission VALUES (-1504,0,-1500,'DELETE',NULL,'SITEMINDER_CONFIGURATION');
INSERT INTO rbac_permission VALUES (-1505,0,-1500,'READ',NULL,'SECURE_PASSWORD');

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
INSERT INTO rbac_permission VALUES (-360,0,-350,'READ',NULL,'ASSERTION_ACCESS');
INSERT INTO rbac_permission VALUES (-361,0,-350,'CREATE',NULL,'ASSERTION_ACCESS');
INSERT INTO rbac_permission VALUES (-442,0,-400,'CREATE',NULL,'ASSERTION_ACCESS');
INSERT INTO rbac_permission VALUES (-443,0,-400,'READ',NULL,'ASSERTION_ACCESS');

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
INSERT INTO rbac_permission VALUES (-1451,0,-1450,'CREATE',null,'CUSTOM_KEY_VALUE_STORE');
INSERT INTO rbac_permission VALUES (-1452,0,-1450,'READ',null,'CUSTOM_KEY_VALUE_STORE');
INSERT INTO rbac_permission VALUES (-1453,0,-1450,'UPDATE',null,'CUSTOM_KEY_VALUE_STORE');
INSERT INTO rbac_permission VALUES (-1454,0,-1450,'DELETE',null,'CUSTOM_KEY_VALUE_STORE');

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
ALTER TABLE service_metrics_details DROP COLUMN service_metrics_goid;
ALTER TABLE service_metrics_details ADD PRIMARY KEY (service_metrics_oid, mapping_values_oid);

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

update rbac_role set entity_goid = toGoid(cast(getVariable('connector_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='SSG_CONNECTOR';
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('connector_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'SSG_CONNECTOR'), oid1.entity_id);

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

update rbac_role set entity_goid = toGoid(cast(getVariable('active_connector_prefix') as bigint),entity_oid) where entity_oid is not null and entity_type='SSG_ACTIVE_CONNECTOR';
update rbac_predicate_oid oid1 set oid1.entity_id = ifnull((select goidToString(toGoid(cast(getVariable('active_connector_prefix') as bigint),cast(oid1.entity_id as bigint))) from rbac_predicate left join rbac_permission on rbac_predicate.permission_oid = rbac_permission.objectid where rbac_predicate.objectid = oid1.objectid and rbac_permission.entity_type = 'SSG_ACTIVE_CONNECTOR'), oid1.entity_id);

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
ALTER TABLE policy_alias add constraint FKA07B7103DB935A63 foreign key (folder_goid) references folder;

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

ALTER TABLE published_service ADD COLUMN old_objectid bigint;
update published_service set old_objectid = objectid;
ALTER TABLE published_service DROP COLUMN objectid;
ALTER TABLE published_service ADD COLUMN goid CHAR(16) FOR BIT DATA;
call setVariable('published_service_prefix', cast(randomLongNotReserved() as char(21)));
update published_service set goid = toGoid(cast(getVariable('published_service_prefix') as bigint), old_objectid);
ALTER TABLE published_service ALTER COLUMN goid NOT NULL;
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
ALTER TABLE published_service_alias add constraint FK6AE79FB5DB935A63 foreign key (folder_goid) references folder;

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
ALTER TABLE wsdm_subscription DROP COLUMN published_service_oid;

ALTER TABLE wsdm_subscription ADD COLUMN esm_service_goid CHAR(16) FOR BIT DATA;
update wsdm_subscription set esm_service_goid = toGoid(cast(getVariable('published_service_prefix') as bigint), esm_service_oid);
ALTER TABLE wsdm_subscription DROP COLUMN esm_service_oid;

ALTER TABLE encapsulated_assertion ADD COLUMN policy_goid CHAR(16) FOR BIT DATA;
update encapsulated_assertion set policy_goid = toGoid(cast(getVariable('policy_prefix') as bigint), policy_oid);
ALTER TABLE encapsulated_assertion DROP COLUMN policy_oid;
alter table encapsulated_assertion add constraint FK_ENCASS_POL foreign key (policy_goid) references policy;

update rbac_predicate_attribute set value = goidToString(toGoid(cast(getVariable('published_service_prefix') as bigint), cast(value as bigint))) where attribute='serviceOid' OR attribute='publishedServiceOid' OR attribute='serviceid';
update rbac_predicate_attribute set attribute = 'serviceGoid' where attribute='serviceOid';
update rbac_predicate_attribute set attribute = 'publishedServiceGoid' where attribute='publishedServiceOid';

update rbac_predicate_entityfolder set entity_id = goidToString(toGoid(cast(getVariable('published_service_prefix') as bigint), cast(entity_id as bigint))) where entity_type='SERVICE';
update rbac_predicate_entityfolder set entity_id = goidToString(toGoid(cast(getVariable('folder_prefix') as bigint), cast(entity_id as bigint))) where entity_type='FOLDER';
update rbac_predicate_entityfolder set entity_id = goidToString(toGoid(0, -5002)) where entity_type='FOLDER' and entity_id=goidToString(toGoid(cast(getVariable('folder_prefix') as bigint), -5002));
update rbac_predicate_entityfolder set entity_id = goidToString(toGoid(cast(getVariable('policy_prefix') as bigint), cast(entity_id as bigint))) where entity_type='POLICY';

--
-- Register upgrade task for upgrading sink configuration references to GOIDs
--
INSERT INTO cluster_properties
    (goid, version, propkey, propvalue, properties)
    values (toGoid(0,-800001), 0, 'upgrade.task.800001', 'com.l7tech.server.upgrade.Upgrade71To80SinkConfig', null),
           (toGoid(0,-800002), 0, 'upgrade.task.800002', 'com.l7tech.server.upgrade.Upgrade71To80OidReferences', null);


--
-- License documents for updated licensing model
--

CREATE TABLE license_document (
  objectid bigint NOT NULL,
  contents clob(2147483647),
  PRIMARY KEY (objectid)
);

--
-- Script to update derby ssg database from 7.1.0 to 8.0.0
--
-- Layer 7 Technologies, inc
--

UPDATE ssg_version SET current_version = '8.0.0';

-- TODO fix FR-473 by renumbering rbac_permission -440 to -441 and inserting CREATE ANY POLICY as new -440

--
-- Security Zones
--
create table security_zone (
  objectid bigint not null,
  version integer not null,
  name varchar(128) not null unique,
  description varchar(255) not null,
  entity_types varchar(4096) not null,
  primary key (objectid)
);

create table rbac_predicate_security_zone (
  objectid bigint not null references rbac_predicate(objectid) on delete cascade,
  security_zone_oid bigint references security_zone(objectid) on delete cascade,
  transitive smallint not null,
  primary key (objectid)
);

create table assertion_access (
  objectid bigint not null,
  version integer,
  name varchar(255) not null unique,
  security_zone_oid bigint references security_zone(objectid) on delete set null,
  primary key (objectid)
);

alter table policy add column security_zone_oid bigint;
alter table policy add foreign key (security_zone_oid) references security_zone (objectid) on delete set null;

alter table policy_alias add column security_zone_oid bigint;
alter table policy_alias add foreign key (security_zone_oid) references security_zone (objectid) on delete set null;

alter table published_service add column security_zone_oid bigint;
alter table published_service add foreign key (security_zone_oid) references security_zone (objectid) on delete set null;

alter table published_service_alias add column security_zone_oid bigint;
alter table published_service_alias add foreign key (security_zone_oid) references security_zone (objectid) on delete set null;

alter table folder add column security_zone_oid bigint;
alter table folder add foreign key (security_zone_oid) references security_zone (objectid) on delete set null;

alter table identity_provider add column security_zone_oid bigint;
alter table identity_provider add foreign key (security_zone_oid) references security_zone (objectid) on delete set null;

alter table jdbc_connection add column security_zone_oid bigint;
alter table jdbc_connection add foreign key (security_zone_oid) references security_zone (objectid) on delete set null;

alter table trusted_cert add column security_zone_oid bigint;
alter table trusted_cert add foreign key (security_zone_oid) references security_zone (objectid) on delete set null;

alter table sink_config add column security_zone_oid bigint;
alter table sink_config add foreign key (security_zone_oid) references security_zone (objectid) on delete set null;

alter table secure_password add column security_zone_oid bigint;
alter table secure_password add foreign key (security_zone_oid) references security_zone (objectid) on delete set null;

alter table email_listener add column security_zone_oid bigint;
alter table email_listener add foreign key (security_zone_oid) references security_zone (objectid) on delete set null;

alter table resource_entry add column security_zone_oid bigint;
alter table resource_entry add foreign key (security_zone_oid) references security_zone (objectid) on delete set null;

alter table http_configuration add column security_zone_oid bigint;
alter table http_configuration add foreign key (security_zone_oid) references security_zone (objectid) on delete set null;

alter table connector add column security_zone_oid bigint;
alter table connector add foreign key (security_zone_oid) references security_zone (objectid) on delete set null;

alter table encapsulated_assertion add column security_zone_oid bigint;
alter table encapsulated_assertion add foreign key (security_zone_oid) references security_zone (objectid) on delete set null;

alter table active_connector add column security_zone_oid bigint;
alter table active_connector add foreign key (security_zone_oid) references security_zone (objectid) on delete set null;

alter table revocation_check_policy add column security_zone_oid bigint;
alter table revocation_check_policy add foreign key (security_zone_oid) references security_zone (objectid) on delete set null;

alter table uddi_registries add column security_zone_oid bigint;
alter table uddi_registries add foreign key (security_zone_oid) references security_zone (objectid) on delete set null;
alter table uddi_proxied_service_info add column security_zone_oid bigint;
alter table uddi_proxied_service_info add foreign key (security_zone_oid) references security_zone (objectid) on delete set null;
alter table uddi_service_control add column security_zone_oid bigint;
alter table uddi_service_control add foreign key (security_zone_oid) references security_zone (objectid) on delete set null;

alter table sample_messages add column security_zone_oid bigint;
alter table sample_messages add foreign key (security_zone_oid) references security_zone (objectid) on delete set null;

alter table jms_endpoint add column security_zone_oid bigint;
alter table jms_endpoint add foreign key (security_zone_oid) references security_zone (objectid) on delete set null;
alter table jms_connection add column security_zone_oid bigint;
alter table jms_connection add foreign key (security_zone_oid) references security_zone (objectid) on delete set null;
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
  security_zone_oid bigint references security_zone(objectid) on delete set null,
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
-- Register the derby oids upgrade task for changing oids to goids.
-- Note this is only in the derby upgrade script because this is only supposed to run on derby databases.
--
INSERT INTO cluster_properties
    (objectid, version, propkey, propvalue, properties)
    values (-800001, 0, 'upgrade.task.800001', 'com.l7tech.server.upgrade.Upgrade71To80DerbyOids', null);

--
-- Goidification modification. These involve replacing the oid column with a goid column on entity tables.
--

--- JdbcConnection
ALTER TABLE jdbc_connection ADD COLUMN goid CHAR(16) FOR BIT DATA;

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

--- LogonInfo
ALTER TABLE logon_info ADD COLUMN goid CHAR(16) FOR BIT DATA;

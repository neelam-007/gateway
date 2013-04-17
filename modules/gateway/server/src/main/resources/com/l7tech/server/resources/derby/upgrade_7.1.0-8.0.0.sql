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
  security_zone_oid bigint not null references security_zone(objectid) on delete cascade,
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

--
-- RBAC for Assertions: Update "Publish Webservices" and "Manage Webservices" canned roles so they can still use policy assertions in 8.0
--
INSERT INTO rbac_permission VALUES (-360,0,-350,'READ',NULL,'ASSERTION_ACCESS');
INSERT INTO rbac_permission VALUES (-361,0,-350,'CREATE',NULL,'ASSERTION_ACCESS');
INSERT INTO rbac_permission VALUES (-442,0,-400,'CREATE',NULL,'ASSERTION_ACCESS');
INSERT INTO rbac_permission VALUES (-443,0,-400,'READ',NULL,'ASSERTION_ACCESS');

--
-- Register upgrade task for adding Assertion Access to auto-created "Manage <Blah>" roles
--
INSERT INTO cluster_properties
    (objectid, version, propkey, propvalue, properties)
    values (-800000, 0, 'upgrade.task.800000', 'com.l7tech.server.upgrade.Upgrade71To80UpdateRoles', null);

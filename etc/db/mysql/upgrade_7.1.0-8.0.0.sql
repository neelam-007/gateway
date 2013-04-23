--
-- Script to update mysql ssg database from 7.1.0 to 8.0.0
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
UPDATE ssg_version SET current_version = '8.0.0';

--
-- Security Zones
--
CREATE TABLE security_zone (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  description varchar(255) NOT NULL,
  entity_types varchar(4096) NOT NULL,
  PRIMARY KEY (objectid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

CREATE TABLE rbac_predicate_security_zone (
  objectid bigint(20) NOT NULL,
  security_zone_oid bigint(20) NOT NULL,
  PRIMARY KEY (objectid),
  FOREIGN KEY (objectid) REFERENCES rbac_predicate (objectid) ON DELETE CASCADE,
  FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

CREATE TABLE assertion_access (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  name varchar(255) NOT NULL,
  security_zone_oid bigint(20),
  PRIMARY KEY (objectid),
  UNIQUE KEY i_name (name),
  CONSTRAINT assertion_access_security_zone FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

alter table policy add column security_zone_oid bigint(20);
alter table policy add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table policy_alias add column security_zone_oid bigint(20);
alter table policy_alias add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table published_service add column security_zone_oid bigint(20);
alter table published_service add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table published_service_alias add column security_zone_oid bigint(20);
alter table published_service_alias add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table folder add column security_zone_oid bigint(20);
alter table folder add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table identity_provider add column security_zone_oid bigint(20);
alter table identity_provider add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table jdbc_connection add column security_zone_oid bigint(20);
alter table jdbc_connection add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table trusted_cert add column security_zone_oid bigint(20);
alter table trusted_cert add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table sink_config add column security_zone_oid bigint(20);
alter table sink_config add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table secure_password add column security_zone_oid bigint(20);
alter table secure_password add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table email_listener add column security_zone_oid bigint(20);
alter table email_listener add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table resource_entry add column security_zone_oid bigint(20);
alter table resource_entry add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table http_configuration add column security_zone_oid bigint(20);
alter table http_configuration add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table connector add column security_zone_oid bigint(20);
alter table connector add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table encapsulated_assertion add column security_zone_oid bigint(20);
alter table encapsulated_assertion add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table active_connector add column security_zone_oid bigint(20);
alter table active_connector add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table revocation_check_policy add column security_zone_oid bigint(20);
alter table revocation_check_policy add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table uddi_registries add column security_zone_oid bigint(20);
alter table uddi_registries add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;
alter table uddi_proxied_service_info add column security_zone_oid bigint(20);
alter table uddi_proxied_service_info add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;
alter table uddi_service_control add column security_zone_oid bigint(20);
alter table uddi_service_control add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table sample_messages add column security_zone_oid bigint(20);
alter table sample_messages add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;

alter table jms_endpoint add column security_zone_oid bigint(20);
alter table jms_endpoint add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;
alter table jms_connection add column security_zone_oid bigint(20);
alter table jms_connection add FOREIGN KEY (security_zone_oid) REFERENCES security_zone (objectid) ON DELETE SET NULL;
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

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--

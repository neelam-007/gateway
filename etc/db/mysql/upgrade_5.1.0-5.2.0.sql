--
-- Script to update mysql ssg database from 5.1.0 to 5.2.0
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
UPDATE ssg_version SET current_version = '5.2.0';

--
-- Convert DN columns to allow larger values
--
ALTER TABLE client_cert MODIFY COLUMN subject_dn VARCHAR(500), MODIFY COLUMN issuer_dn VARCHAR(500);
ALTER TABLE trusted_cert MODIFY COLUMN subject_dn VARCHAR(500) NOT NULL, MODIFY COLUMN issuer_dn VARCHAR(500) NOT NULL;

--
-- Upgrade task for DN canonicalization
--
INSERT INTO cluster_properties
    (objectid, version, propkey, propvalue)
    values (-500200, 0, "upgrade.task.500200", "com.l7tech.server.upgrade.Upgrade51To52CanonicalizeDNs");

--
-- Create Table structure for JDBC Connections
--
DROP TABLE IF EXISTS jdbc_connection;
CREATE TABLE jdbc_connection (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  driver_class varchar(256) NOT NULL,
  jdbc_url varchar(256) NOT NULL,
  user_name varchar(128) NOT NULL,
  password varchar(64) NOT NULL,
  enabled tinyint(1) NOT NULL DEFAULT 1,
  min_pool_size integer NOT NULL DEFAULT 3,
  max_pool_size integer NOT NULL DEFAULT 15,
  additional_properties mediumtext,
  PRIMARY KEY (objectid),
  UNIQUE(name)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for UDDI Registries
-- Note: base_url is unique and has a size limit of 255 bytes, which is the max allowed for a unique key
-- in mysql when using utf-8 encoding. It is the max size of a hostname
--
DROP TABLE IF EXISTS uddi_registries;
CREATE TABLE uddi_registries (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  enabled tinyint(1) NOT NULL DEFAULT 0,
  registry_type varchar(128) NOT NULL,
  base_url varchar(255) NOT NULL,
  security_url varchar(255) NOT NULL,
  inquiry_url varchar(255) NOT NULL,
  publish_url varchar(255) NOT NULL,
  subscription_url varchar(255) NULL,
  client_auth tinyint(1) NOT NULL DEFAULT 0,
  keystore_oid bigint(20) NULL,
  key_alias varchar(255) NULL,
  user_name varchar(128),
  password varchar(128),
  metrics_enabled tinyint(1) NOT NULL DEFAULT 0,
  metrics_publish_frequency integer NOT NULL DEFAULT 0,
  monitoring_enabled tinyint(1) NOT NULL DEFAULT 0,
  subscribe_for_notifications tinyint(1) NOT NULL DEFAULT 0,
  monitor_frequency integer NOT NULL DEFAULT 0,
  PRIMARY KEY (objectid),
  UNIQUE(name),
  UNIQUE(base_url)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table for UDDI registry subscription information
--
DROP TABLE IF EXISTS uddi_registry_subscription;
CREATE TABLE uddi_registry_subscription (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  uddi_registry_oid bigint(20) NOT NULL,
  uddi_subscription_key varchar(255),
  uddi_subscription_expiry_time bigint NOT NULL,
  uddi_subscription_notified_time bigint NOT NULL,
  uddi_subscription_check_time bigint NOT NULL,
  PRIMARY KEY (objectid),
  UNIQUE KEY  (uddi_registry_oid),
  FOREIGN KEY (uddi_registry_oid) REFERENCES uddi_registries (objectid) ON DELETE CASCADE
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for Gateway WSDLs published to UDDI. Known as 'Proxied Business Services'
-- Entity UDDIProxiedService
-- the general_keyword value is used to identify all services which originated from the same published service's wsdl
--
DROP TABLE IF EXISTS uddi_proxied_service_info;
CREATE TABLE uddi_proxied_service_info (
  objectid bigint(20) NOT NULL,
  published_service_oid bigint(20) NOT NULL,
  uddi_registry_oid bigint(20) NOT NULL,
  version integer NOT NULL,
  uddi_business_key varchar(255) NOT NULL,
  uddi_business_name varchar(255) NOT NULL,
  update_proxy_on_local_change tinyint(1) NOT NULL DEFAULT 0,
  created_from_existing tinyint(1) NOT NULL DEFAULT 0,
  metrics_enabled tinyint(1) NOT NULL DEFAULT 0,
  wspolicy_tmodel_key varchar(255),
  publish_type tinyint(1) NOT NULL,        
  PRIMARY KEY (objectid),
  UNIQUE KEY  (published_service_oid),
  FOREIGN KEY (published_service_oid) REFERENCES published_service (objectid) ON DELETE CASCADE,
  FOREIGN KEY (uddi_registry_oid) REFERENCES uddi_registries (objectid) ON DELETE CASCADE
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Child of uddi_proxied_service_info, represents the publishing of a specific wsdl:service from the Gateway's WSDL
--
DROP TABLE IF EXISTS uddi_proxied_service;
CREATE TABLE uddi_proxied_service (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  uddi_proxied_service_info_oid bigint(20) NOT NULL,
  uddi_service_key varchar(255) NOT NULL,
  uddi_service_name varchar(255) NOT NULL,
  wsdl_service_name varchar(255) NOT NULL,
  PRIMARY KEY (objectid),
  UNIQUE KEY (uddi_proxied_service_info_oid, wsdl_service_name),
  UNIQUE (uddi_service_key),
  FOREIGN KEY (uddi_proxied_service_info_oid) REFERENCES uddi_proxied_service_info (objectid) ON DELETE CASCADE
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Child of uddi_proxied_service_info, runtime information regarding its publish status
--
DROP TABLE IF EXISTS uddi_publish_status;
CREATE TABLE uddi_publish_status (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  uddi_proxied_service_info_oid bigint(20) NOT NULL,
  publish_status tinyint(1) NOT NULL,
  last_status_change BIGINT(20) NOT NULL,
  PRIMARY KEY (objectid),
  UNIQUE (uddi_proxied_service_info_oid),
  FOREIGN KEY (uddi_proxied_service_info_oid) REFERENCES uddi_proxied_service_info (objectid) ON DELETE CASCADE
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Tracking table for metrics and policy attachment publishing
--
DROP TABLE IF EXISTS uddi_business_service_status;
CREATE TABLE uddi_business_service_status (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  published_service_oid bigint(20) NOT NULL,
  uddi_registry_oid bigint(20) NOT NULL,
  uddi_service_key varchar(255) NOT NULL,
  uddi_service_name varchar(255) NOT NULL,
  uddi_policy_tmodel_key varchar(255),
  policy_status varchar(32) NOT NULL,
  uddi_metrics_tmodel_key varchar(255),
  metrics_reference_status varchar(32) NOT NULL,
  PRIMARY KEY (objectid),
  FOREIGN KEY (published_service_oid) REFERENCES published_service (objectid) ON DELETE CASCADE,
  FOREIGN KEY (uddi_registry_oid) REFERENCES uddi_registries (objectid) ON DELETE CASCADE
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

-- Table for a service published from (or otherwise associated with) UDDI
--
DROP TABLE IF EXISTS uddi_service_control;
CREATE TABLE uddi_service_control (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  published_service_oid bigint(20) NOT NULL,
  uddi_registry_oid bigint(20) NOT NULL,
  uddi_business_key varchar(255) NOT NULL,
  uddi_business_name varchar(255) NOT NULL,
  uddi_service_key varchar(255) NOT NULL,
  uddi_service_name varchar(255) NOT NULL,
  wsdl_service_name varchar(255) NOT NULL,
  wsdl_port_name varchar(255) NOT NULL,
  wsdl_port_binding varchar(255) NOT NULL,
  access_point_url varchar(255) NOT NULL,
  under_uddi_control tinyint(1) NOT NULL DEFAULT 0,
  proxy_binding_key varchar(255),
  monitoring_enabled tinyint(1) NOT NULL DEFAULT 0,
  disable_service_on_change tinyint(1) NOT NULL DEFAULT 0,
  metrics_enabled tinyint(1) NOT NULL DEFAULT 0,
  wspolicy_tmodel_key varchar(255),
  PRIMARY KEY (objectid),
  UNIQUE KEY  (published_service_oid),
  FOREIGN KEY (published_service_oid) REFERENCES published_service (objectid) ON DELETE CASCADE,
  FOREIGN KEY (uddi_registry_oid) REFERENCES uddi_registries (objectid) ON DELETE CASCADE
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

INSERT INTO rbac_role VALUES (-950,0,'Manage JDBC Connections', null,null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete JDBC connections.');
INSERT INTO rbac_permission VALUES (-951,0,-950,'READ',NULL,'JDBC_CONNECTION');
INSERT INTO rbac_permission VALUES (-952,0,-950,'CREATE',NULL,'JDBC_CONNECTION');
INSERT INTO rbac_permission VALUES (-953,0,-950,'UPDATE',NULL,'JDBC_CONNECTION');
INSERT INTO rbac_permission VALUES (-954,0,-950,'DELETE',NULL,'JDBC_CONNECTION');
INSERT INTO rbac_permission VALUES (-955,0,-950,'READ',NULL,'SERVICE');

INSERT INTO rbac_role VALUES (-1000,0,'Manage UDDI Registries', null,null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete UDDI Registry connections  and proxied Business Services.');
INSERT INTO rbac_permission VALUES (-1001,0,-1000,'READ',NULL,'UDDI_REGISTRY');
INSERT INTO rbac_permission VALUES (-1002,0,-1000,'CREATE',NULL,'UDDI_REGISTRY');
INSERT INTO rbac_permission VALUES (-1003,0,-1000,'UPDATE',NULL,'UDDI_REGISTRY');
INSERT INTO rbac_permission VALUES (-1004,0,-1000,'DELETE',NULL,'UDDI_REGISTRY');
INSERT INTO rbac_permission VALUES (-1005,0,-1000,'READ',NULL,'SERVICE');

--Update to the 'Manage Webservices Role'
INSERT INTO rbac_permission VALUES (-429,0,-400,'READ',NULL,'UDDI_REGISTRY');
INSERT INTO rbac_permission VALUES (-430,0,-400,'READ',NULL,'UDDI_PROXIED_SERVICE_INFO');
INSERT INTO rbac_permission VALUES (-431,0,-400,'UPDATE',NULL,'UDDI_PROXIED_SERVICE_INFO');
INSERT INTO rbac_permission VALUES (-432,0,-400,'DELETE',NULL,'UDDI_PROXIED_SERVICE_INFO');
INSERT INTO rbac_permission VALUES (-433,0,-400,'CREATE',NULL,'UDDI_PROXIED_SERVICE_INFO');
INSERT INTO rbac_permission VALUES (-434,0,-400,'READ',NULL,'UDDI_SERVICE_CONTROL');
INSERT INTO rbac_permission VALUES (-435,0,-400,'UPDATE',NULL,'UDDI_SERVICE_CONTROL');
INSERT INTO rbac_permission VALUES (-436,0,-400,'DELETE',NULL,'UDDI_SERVICE_CONTROL');
INSERT INTO rbac_permission VALUES (-437,0,-400,'CREATE',NULL,'UDDI_SERVICE_CONTROL');

-- Updated Roles which require permission to read UDDIRegistries - 'Publish Webservices'
INSERT INTO rbac_permission VALUES (-356,0,-350,'READ',NULL,'UDDI_REGISTRY');

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--

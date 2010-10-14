--
-- Script to update mysql ssg database from 5.3.1 to 5.4.0
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

UPDATE ssg_version SET current_version = '5.4.0';

--
-- bug 9071  allow larger JDBC URL
--
ALTER TABLE jdbc_connection MODIFY COLUMN jdbc_url varchar(4096) NOT NULL;

--
-- Secure password storage facility
--
DROP TABLE IF EXISTS secure_password;
CREATE TABLE secure_password (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  description varchar(256),
  usage_from_variable tinyint(1) NOT NULL DEFAULT 0,
  encoded_password varchar(256) NOT NULL,
  last_update bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (objectid),
  UNIQUE(name)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;


INSERT INTO rbac_role VALUES (-1050,0,'Manage Secure Passwords', null,null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete any stored password.');
INSERT INTO rbac_permission VALUES (-1051,0,-1050,'READ',NULL,'SECURE_PASSWORD');
INSERT INTO rbac_permission VALUES (-1052,0,-1050,'CREATE',NULL,'SECURE_PASSWORD');
INSERT INTO rbac_permission VALUES (-1053,0,-1050,'UPDATE',NULL,'SECURE_PASSWORD');
INSERT INTO rbac_permission VALUES (-1054,0,-1050,'DELETE',NULL,'SECURE_PASSWORD');

--
-- Increase IP address string lengths to 39 to accommodate IPv6
--
ALTER TABLE audit_main MODIFY ip_address varchar(39) DEFAULT NULL;
ALTER TABLE cluster_info MODIFY address varchar(39) NOT NULL;
ALTER TABLE cluster_info MODIFY multicast_address varchar(39) NOT NULL;

--
-- HTTP options support
--
DROP TABLE IF EXISTS http_configuration;
CREATE TABLE http_configuration (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  host varchar(128) NOT NULL,
  port int(5) NOT NULL DEFAULT 0,
  protocol varchar(8) DEFAULT NULL,
  path varchar(4096) DEFAULT NULL,
  username varchar(255) DEFAULT NULL,
  password_oid bigint(20) DEFAULT NULL,
  ntlm_host varchar(128) DEFAULT NULL,
  ntlm_domain varchar(255) DEFAULT NULL,
  tls_version varchar(8) DEFAULT NULL,
  tls_key_use varchar(8) DEFAULT 'DEFAULT',
  tls_keystore_oid bigint(20) NOT NULL DEFAULT 0,
  tls_key_alias varchar(255) DEFAULT NULL,
  timeout_connect int(10) NOT NULL DEFAULT -1,
  timeout_read int(10) NOT NULL DEFAULT -1,
  follow_redirects tinyint(1) NOT NULL DEFAULT 0,
  proxy_use varchar(8) DEFAULT 'DEFAULT',
  proxy_host varchar(128) DEFAULT NULL,
  proxy_port int(5) NOT NULL DEFAULT 0,
  proxy_username varchar(255) DEFAULT NULL,
  proxy_password_oid bigint(20) DEFAULT NULL,
  FOREIGN KEY (password_oid) REFERENCES secure_password (objectid),
  FOREIGN KEY (proxy_password_oid) REFERENCES secure_password (objectid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

INSERT INTO rbac_permission VALUES (-439,0,-400,'READ',NULL,'HTTP_CONFIGURATION');
INSERT INTO rbac_permission VALUES (-358,0,-350,'READ',NULL,'HTTP_CONFIGURATION');

INSERT INTO cluster_properties
    (objectid, version, propkey, propvalue)
    values (-500400, 0, "upgrade.task.500400", "com.l7tech.server.upgrade.Upgrade531To54UpdateRoles");


--
-- Manage Private Keys role
--
INSERT INTO `rbac_role` VALUES (-1100,1,'Manage Private Keys',NULL,NULL,NULL,'Users in this role have the ability to read, create, update, and delete private keys, as well as the ability to change the default SSL key and the default CA key.');
INSERT INTO `rbac_permission` VALUES
    (-1101,0,-1100,'UPDATE',NULL,'CLUSTER_PROPERTY'),
    (-1102,0,-1100,'DELETE',NULL,'SSG_KEY_ENTRY'),
    (-1103,0,-1100,'READ',NULL,'CLUSTER_PROPERTY'),
    (-1104,0,-1100,'READ',NULL,'SSG_KEY_ENTRY'),
    (-1105,0,-1100,'READ',NULL,'CLUSTER_PROPERTY'),
    (-1106,0,-1100,'UPDATE',NULL,'SSG_KEY_ENTRY'),
    (-1107,0,-1100,'CREATE',NULL,'SSG_KEY_ENTRY'),
    (-1108,0,-1100,'UPDATE',NULL,'CLUSTER_PROPERTY'),
    (-1109,0,-1100,'DELETE',NULL,'CLUSTER_PROPERTY'),
    (-1110,0,-1100,'CREATE',NULL,'CLUSTER_PROPERTY'),
    (-1111,0,-1100,'CREATE',NULL,'CLUSTER_PROPERTY'),
    (-1112,0,-1100,'UPDATE',NULL,'SSG_KEYSTORE'),
    (-1113,0,-1100,'DELETE',NULL,'CLUSTER_PROPERTY'),
    (-1114,0,-1100,'READ',NULL,'SSG_KEYSTORE');
INSERT INTO `rbac_predicate` VALUES
    (-1101,0,-1101),
    (-1103,0,-1103),
    (-1105,0,-1105),
    (-1108,0,-1108),
    (-1109,0,-1109),
    (-1110,0,-1110),
    (-1111,0,-1111),
    (-1113,0,-1113);
INSERT INTO `rbac_predicate_attribute` VALUES
    (-1101,'name','keyStore.defaultSsl.alias'),
    (-1103,'name','keyStore.defaultCa.alias'),
    (-1105,'name','keyStore.defaultSsl.alias'),
    (-1108,'name','keyStore.defaultCa.alias'),
    (-1109,'name','keyStore.defaultSsl.alias'),
    (-1110,'name','keyStore.defaultCa.alias'),
    (-1111,'name','keyStore.defaultSsl.alias'),
    (-1113,'name','keyStore.defaultCa.alias');

--
-- Explicit SOAP version configuration
--
ALTER TABLE published_service ADD COLUMN soap_version VARCHAR(20) DEFAULT NULL;

--
-- Reenable FK at very end of script
--
SET FOREIGN_KEY_CHECKS=1;

--
-- DO NOT ADD STATEMENTS HERE. ADD THEM ABOVE "SET FOREIGN_KEY_CHECKS"
--


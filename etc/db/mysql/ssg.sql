--
-- MySQL version of SSG database creation script.
--

SET FOREIGN_KEY_CHECKS = 0;

--
-- Table structure for table 'hibernate_unique_key'
--

DROP TABLE IF EXISTS hibernate_unique_key;
CREATE TABLE hibernate_unique_key (
  next_hi int(11) default NULL
) ENGINE=MyISAM DEFAULT CHARACTER SET utf8;

--
-- Dumping data for table 'hibernate_unique_key'
--


INSERT INTO hibernate_unique_key (next_hi) VALUES (1);

--
-- Create "sequence" function for next_hi value
--
-- NOTE that the function is safe when either row based or statement based replication is in use.
--
DROP FUNCTION IF EXISTS next_hi;
delimiter //
CREATE FUNCTION next_hi() RETURNS bigint NOT DETERMINISTIC MODIFIES SQL DATA SQL SECURITY INVOKER
BEGIN
    UPDATE hibernate_unique_key SET next_hi=last_insert_id(next_hi)+IF(@@global.server_id=0,1,2);
    RETURN IF((last_insert_id()%2=0 and @@global.server_id=1) or (last_insert_id()%2=1 and @@global.server_id=2),last_insert_id()+1,last_insert_id());
END
//
delimiter ;

--
-- Create the toGoid function. This makes it easier to create goid's from a high and low number. This returns the goid
-- as a binary(16)
--
DROP FUNCTION IF EXISTS toGoid;
delimiter //
CREATE FUNCTION toGoid (prefix bigint, suffix bigint)
RETURNS binary(16) DETERMINISTIC
begin
    if suffix is null then RETURN null;
	else RETURN concat(lpad(char(prefix >> 32, prefix),8,'\0'),lpad(char(suffix >> 32, suffix),8,'\0'));
	end if;
end//
delimiter ;

DROP FUNCTION IF EXISTS goidToString;
CREATE FUNCTION goidToString(goid binary(16)) RETURNS CHAR(32) DETERMINISTIC
RETURN lower(hex(goid));

--
-- Security zones
--
DROP TABLE IF EXISTS security_zone;
CREATE TABLE security_zone (
  goid binary(16) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  description varchar(255) NOT NULL,
  entity_types varchar(4096) NOT NULL,
  PRIMARY KEY (goid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table for replication monitoring
--
DROP TABLE IF EXISTS replication_status;
CREATE TABLE replication_status (
  objectid bigint(20) NOT NULL,
  sequence bigint(20) NOT NULL DEFAULT 1,
  updated bigint(20) NOT NULL DEFAULT 0,
  nodeid varchar(32),
  delay bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (objectid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

INSERT INTO replication_status (objectid, nodeid) values (1, null);

--
-- Table structure for table 'identity_provider'
--

DROP TABLE IF EXISTS identity_provider;
CREATE TABLE identity_provider (
  goid binary(16) NOT NULL,
  version int(11) default NULL,
  name varchar(128) NOT NULL,
  description mediumtext,
  type bigint(20) NOT NULL,
  properties mediumtext,
  security_zone_goid binary(16),
  PRIMARY KEY  (goid),
  UNIQUE KEY ipnm_idx (name),
  CONSTRAINT identity_provider_security_zone FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Dumping data for table 'identity_provider'
--

INSERT INTO identity_provider (goid,name,description,type,properties,version,security_zone_goid) VALUES (toGoid(0,-2),'Internal Identity Provider','Internal Identity Provider',1,'<java version="1.6.0_01" class="java.beans.XMLDecoder"><object class="java.util.HashMap"><void method="put"><string>adminEnabled</string><boolean>true</boolean></void></object></java>',0,NULL);

--
-- Table structure for table 'internal_group'
--

DROP TABLE IF EXISTS internal_group;
CREATE TABLE internal_group (
  goid binary(16) NOT NULL,
  version int(11) NOT NULL,
  name varchar(128) NOT NULL,
  description mediumtext,
  enabled boolean DEFAULT TRUE,
  PRIMARY KEY  (goid),
  UNIQUE KEY g_idx (name)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table 'internal_user'
--

DROP TABLE IF EXISTS internal_user;
CREATE TABLE internal_user (
  goid binary(16) NOT NULL,
  version int(11) NOT NULL,
  name varchar(128) default NULL,
  login varchar(255) NOT NULL,
  password varchar(256) NOT NULL,
  digest varchar(32) default NULL,
  first_name varchar(32) default NULL,
  last_name varchar(32) default NULL,
  email varchar(128) default NULL,
  description varchar(255) default NULL,
  expiration bigint(20) NOT NULL,
  password_expiry bigint(20) DEFAULT 0,
  change_password boolean DEFAULT TRUE,
  enabled boolean DEFAULT TRUE,
  properties mediumtext default NULL,
  PRIMARY KEY  (goid),
  UNIQUE KEY l_idx (login)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Dumping data for table 'internal_user'
--

-- The same hash from resetAdmin.sh is used here. Digest property is set to NULL by default.
INSERT INTO internal_user (goid, version, name, login, password, digest, first_name, last_name, email, description, expiration, password_expiry, change_password, enabled, properties) VALUES (toGoid(0,3),0,'admin','admin','$6$S7Z3HcudYNsObgs8$SjwZ3xtCkSjXOK2vHfOVEg2dJES3cgvtIUdHbEN/KdCBXoI6uuPSbxTEwcH.av6lpcb1p6Lu.gFeIX04FBxiJ.',NULL,'','','','',-1,1577865600000,FALSE,TRUE,NULL);

--
-- Table structure for table 'internal_user_group'
--

DROP TABLE IF EXISTS internal_user_group;
CREATE TABLE internal_user_group (
  goid binary(16) not null,
  version int(11) not null,
  internal_group binary(16) NOT NULL,
  provider_goid binary(16) NOT NULL,
  user_goid binary(16) NOT NULL,
  subgroup_id varchar(255),
  PRIMARY KEY (goid),
  INDEX (internal_group),
  INDEX (provider_goid),
  INDEX (user_goid),
  INDEX (subgroup_id)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;


--
-- Table structure for table 'folder'
--

DROP TABLE IF EXISTS folder;
CREATE TABLE folder (
  goid binary(16) NOT NULL,
  version int(11) not null,
  name varchar(128) NOT NULL,
  parent_folder_goid binary(16),
  security_zone_goid binary(16),
  PRIMARY KEY  (goid),
  CONSTRAINT folder_parent_folder FOREIGN KEY (parent_folder_goid) REFERENCES folder (goid),
  CONSTRAINT folder_security_zone FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL,
  UNIQUE KEY `i_name_parent` (`name`,`parent_folder_goid`)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;


INSERT INTO folder (goid, version, name, parent_folder_goid, security_zone_goid) VALUES (toGoid(0,-5002), 0, 'Root Node', NULL, NULL);

--
-- Table to record system logon activity
--
DROP TABLE IF EXISTS logon_info;
CREATE TABLE logon_info (
  goid BINARY(16) NOT NULL,
  version int(11) NOT NULL,
  provider_goid binary(16) NOT NULL,
  login varchar(255) NOT NULL,
  fail_count int(11) NOT NULL DEFAULT 0,
  last_attempted bigint(20) NOT NULL,
  last_activity bigint(20) NOT NULL,
  state varchar(32) NOT NULL DEFAULT 'ACTIVE',
  PRIMARY KEY (goid),
  UNIQUE KEY unique_provider_login (provider_goid, login),
  CONSTRAINT logon_info_provider FOREIGN KEY (provider_goid) REFERENCES identity_provider(goid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table to record password changes being made
--
DROP TABLE IF EXISTS password_history;
CREATE TABLE password_history (
  objectid bigint(20) NOT NULL,
  internal_user_goid binary(16) NOT NULL,
  last_changed bigint(20) NOT NULL,
  prev_password varchar(256) NULL,
  PRIMARY KEY (objectid),
  FOREIGN KEY (internal_user_goid) REFERENCES internal_user (goid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table 'published_service'
--

DROP TABLE IF EXISTS published_service;
CREATE TABLE published_service (
  goid binary(16) NOT NULL,
  version int(11) NOT NULL,
  name varchar(255) NOT NULL,
  policy_xml mediumtext,
  policy_goid binary(16) default NULL,
  wsdl_url varchar(4096),
  wsdl_xml mediumtext,
  disabled TINYINT(1) NOT NULL DEFAULT 0,
  soap TINYINT(1) NOT NULL DEFAULT 1,
  internal TINYINT(1) NOT NULL DEFAULT 0,
  routing_uri varchar(128),
  default_routing_url varchar(4096),
  http_methods mediumtext,
  lax_resolution TINYINT(1) NOT NULL DEFAULT 0,
  wss_processing TINYINT(1) NOT NULL DEFAULT 1,
  tracing TINYINT(1) NOT NULL DEFAULT 0,
  folder_goid binary(16),
  soap_version VARCHAR(20) DEFAULT 'UNKNOWN',
  security_zone_goid binary(16),
  PRIMARY KEY (goid),
  FOREIGN KEY (policy_goid) REFERENCES policy (goid),
  CONSTRAINT published_service_folder FOREIGN KEY (folder_goid) REFERENCES folder (goid),
  CONSTRAINT service_security_zone FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table 'published_service_alias'
--
DROP TABLE IF EXISTS published_service_alias;
CREATE TABLE published_service_alias (
  `goid` binary(16) NOT NULL,
  `version` int(11) NOT NULL,
  `folder_goid` binary(16) NOT NULL,
  `published_service_goid` binary(16) NOT NULL,
  security_zone_goid binary(16),
  UNIQUE KEY (folder_goid, published_service_goid),
  FOREIGN KEY (published_service_goid) REFERENCES published_service (goid) ON DELETE CASCADE,
  FOREIGN KEY (folder_goid) REFERENCES folder (goid) ON DELETE CASCADE,
  CONSTRAINT service_alias_security_zone FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table 'policy'
--

DROP TABLE IF EXISTS policy;
CREATE TABLE policy (
  goid binary(16) NOT NULL,
  version int(11) NOT NULL,
  name varchar(255) NOT NULL,
  xml mediumtext NOT NULL,
  policy_type VARCHAR(32) NOT NULL,
  soap TINYINT(1) NOT NULL DEFAULT 0,
  guid char(36) NOT NULL,
  internal_tag VARCHAR(64),
  folder_goid binary(16),
  security_zone_goid binary(16),
  PRIMARY KEY (goid),
  UNIQUE KEY i_name (name),
  UNIQUE KEY i_guid (guid),
  CONSTRAINT policy_folder FOREIGN KEY (folder_goid) REFERENCES folder (goid),
  CONSTRAINT policy_security_zone FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL,
  INDEX (policy_type)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Policy XML rollback support
--

DROP TABLE IF EXISTS policy_version;
CREATE TABLE policy_version (
  goid binary(16) NOT NULL,
  version int(11) NOT NULL,
  name varchar(255),
  policy_goid binary(16) NOT NULL,
  ordinal int(20) NOT NULL,
  time bigint(20) NOT NULL,
  user_provider_goid binary(16),
  user_login varchar(255),
  active boolean,
  xml mediumtext,
  PRIMARY KEY (goid),
  INDEX (policy_goid),
  UNIQUE KEY i_policy_ordinal (policy_goid, ordinal),
  FOREIGN KEY (policy_goid) REFERENCES policy (goid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;


--
-- Table structure for table 'policy_alias'
--
DROP TABLE IF EXISTS policy_alias;
CREATE TABLE policy_alias (
  `goid` binary(16) NOT NULL,
  `version` int(11) NOT NULL,
  `folder_goid` binary(16) NOT NULL,
  `policy_goid` binary(16) NOT NULL,
  security_zone_goid binary(16),
  UNIQUE KEY (folder_goid, policy_goid),
  FOREIGN KEY (policy_goid) REFERENCES policy (goid) ON DELETE CASCADE,
  FOREIGN KEY (folder_goid) REFERENCES folder (goid) ON DELETE CASCADE,
  CONSTRAINT policy_alias_security_zone FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


--
-- Table structure for table 'client_cert'
-- UNIQUE KEY i_issuer_serial (issuer_dn, serial), --must be added in upgrade task or it will fail on multi-version upgrades
--

DROP TABLE IF EXISTS client_cert;
CREATE TABLE client_cert (
  goid binary(16) NOT NULL,
  provider binary(16) NOT NULL,
  user_id varchar(255),
  login varchar(255),
  cert mediumtext DEFAULT NULL,
  reset_counter int NOT NULL,
  thumbprint_sha1 varchar(64),
  ski varchar(64),
  subject_dn varchar(2048),
  issuer_dn varchar(2048),
  serial varchar(64),
  PRIMARY KEY  (goid),
  FOREIGN KEY (provider) REFERENCES identity_provider (goid) ON DELETE CASCADE,
  UNIQUE KEY i_identity (provider, user_id),
  INDEX i_subject_dn (subject_dn(255)),
  INDEX i_issuer_dn (issuer_dn(255)),
  INDEX i_thumb (thumbprint_sha1),
  INDEX i_ski (ski)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table 'resolution_configuration'
--

DROP TABLE IF EXISTS resolution_configuration;
CREATE TABLE resolution_configuration (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  path_required tinyint NOT NULL default '0',
  path_case_sensitive tinyint NOT NULL default '0',
  use_url_header tinyint NOT NULL default '0',
  use_service_oid tinyint NOT NULL default '0',
  use_soap_action tinyint NOT NULL default '0',
  use_soap_namespace tinyint NOT NULL default '0',
  PRIMARY KEY (objectid),
  UNIQUE KEY rc_name_idx (name)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

INSERT INTO resolution_configuration (objectid, version, name, path_case_sensitive, use_url_header, use_service_oid, use_soap_action, use_soap_namespace) VALUES (-2, 0, 'Default', 1, 1, 1, 1, 1);

--
-- Table structure for table 'cluster_info'
--

DROP TABLE IF EXISTS cluster_info;

CREATE TABLE cluster_info (
  nodeid varchar(32) NOT NULL,
  mac varchar(18) NOT NULL,
  name varchar(128) NOT NULL,
  address varchar(39) NOT NULL,
  esm_address varchar(39) NOT NULL,
  multicast_address varchar(39),
  uptime bigint NOT NULL,
  avgload double NOT NULL,
  statustimestamp bigint NOT NULL,
  PRIMARY KEY(nodeid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table 'cluster_master'
--

DROP TABLE IF EXISTS cluster_master;

CREATE TABLE cluster_master (
  nodeid varchar(32),
  touched_time BIGINT(20) NOT NULL,
  version integer NOT NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

INSERT INTO cluster_master (nodeid, touched_time, version) VALUES (NULL, 0, 0);

--
-- Table structure for table 'service_usage'
--

DROP TABLE IF EXISTS service_usage;
CREATE TABLE service_usage (
  serviceid binary(16) NOT NULL,
  nodeid varchar(32) NOT NULL,
  requestnr bigint NOT NULL,
  authorizedreqnr bigint NOT NULL,
  completedreqnr bigint NOT NULL,
  primary key(serviceid, nodeid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table 'jms_connection'
--

DROP TABLE IF EXISTS jms_connection;
CREATE TABLE jms_connection (
  goid binary(16) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  jndi_url varchar(255),
  factory_classname varchar(255),
  destination_factory_url varchar(255) default '',
  queue_factory_url varchar(255) default '',
  topic_factory_url varchar(255) default '',
  username varchar(255) default '',
  password varchar(255) default '',
  is_template tinyint NOT NULL default '0',
  properties mediumtext,
  provider_type varchar(255),
  security_zone_goid binary(16),
  primary key(goid),
  CONSTRAINT jms_conn_security_zone FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table 'jms_endpoint'
--

DROP TABLE IF EXISTS jms_endpoint;
CREATE TABLE jms_endpoint(
  goid binary(16) NOT NULL,
  version integer NOT NULL,
  connection_goid binary(16) NOT NULL,
  name varchar(128) NOT NULL,
  destination_type tinyint(1) NOT NULL default 1,
  destination_name varchar(128),
  failure_destination_name varchar(128),
  acknowledgement_type varchar(128),    
  reply_type integer default '0',
  reply_to_queue_name varchar(128),
  disabled tinyint(1) NOT NULL default 0,
  username varchar(255) default '',
  password varchar(255) default '',
  max_concurrent_requests integer default '1',
  is_message_source tinyint default '0',
  is_template tinyint NOT NULL default '0',
  outbound_message_type varchar(128),
  use_message_id_for_correlation tinyint(1) NOT NULL DEFAULT 0,
  request_max_size bigint NOT NULL default -1,
  security_zone_goid binary(16),
  old_objectid bigint,
  primary key(goid),
  CONSTRAINT jms_endpoint_security_zone FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS revocation_check_policy;
CREATE TABLE revocation_check_policy (
  goid binary(16) NOT NULL,
  version int(11) default NULL,
  name varchar(128) NOT NULL,
  revocation_policy_xml mediumtext,
  default_policy tinyint default '0',
  default_success tinyint default '0',
  continue_server_unavailable tinyint default '0',
  security_zone_goid binary(16),
  PRIMARY KEY  (goid),
  UNIQUE KEY rcp_name_idx (name),
  CONSTRAINT rcp_security_zone FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table 'trusted_cert'
-- must be added on upgrade task:
-- UNIQUE i_issuer_serial (issuer_dn, serial),
--

DROP TABLE IF EXISTS trusted_cert;
CREATE TABLE trusted_cert (
  goid binary(16) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  subject_dn varchar(2048),
  cert_base64 mediumtext NOT NULL,
  trusted_for_ssl tinyint(1) default '0',
  trusted_for_client tinyint(1) default '0',
  trusted_for_server tinyint(1) default '0',
  trusted_for_saml tinyint(1) default '0',
  trusted_as_saml_attesting_entity tinyint(1) default '0',
  verify_hostname tinyint(1) default '0',
  thumbprint_sha1 varchar(64),
  ski varchar(64),
  trust_anchor tinyint default 1,
  revocation_type varchar(128) NOT NULL DEFAULT 'USE_DEFAULT',
  revocation_check_policy_goid binary(16),
  issuer_dn varchar(2048),
  serial varchar(64),
  security_zone_goid binary(16),
  PRIMARY KEY  (goid),
  UNIQUE i_thumb (thumbprint_sha1),
  INDEX i_ski (ski),
  INDEX i_subject_dn (subject_dn(255)),
  INDEX i_issuer_dn (issuer_dn(255)),
  FOREIGN KEY (revocation_check_policy_goid) REFERENCES revocation_check_policy (goid),
  CONSTRAINT trusted_cert_security_zone FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS trusted_esm;
CREATE TABLE trusted_esm (
  objectid bigint NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  trusted_cert_goid binary(16) NOT NULL,
  primary key(objectid),
  FOREIGN KEY (trusted_cert_goid) REFERENCES trusted_cert (goid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS trusted_esm_user;
CREATE TABLE trusted_esm_user (
  objectid bigint NOT NULL,
  version integer NOT NULL,
  trusted_esm_oid bigint(20) NOT NULL,
  provider_goid binary(16) NOT NULL,
  user_id varchar(128) NOT NULL,
  esm_user_id varchar(128) NOT NULL,
  esm_user_display_name varchar(128) default NULL,
  PRIMARY KEY(objectid),
  FOREIGN KEY (trusted_esm_oid) REFERENCES trusted_esm (objectid) ON DELETE CASCADE,
  FOREIGN KEY (provider_goid) REFERENCES identity_provider (goid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS fed_user;
CREATE TABLE fed_user (
  goid binary(16) NOT NULL,
  version int(11) NOT NULL,
  name varchar(128) NOT NULL,
  provider_goid binary(16) NOT NULL,
  subject_dn varchar(255),
  email varchar(128) default NULL,
  login varchar(255),
  first_name varchar(32) default NULL,
  last_name varchar(32) default NULL,
  PRIMARY KEY (goid),
  INDEX i_provider_goid (provider_goid),
  INDEX i_email (email),
  INDEX i_login (login),
  INDEX i_subject_dn (subject_dn),
  UNIQUE KEY i_name (provider_goid, name)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS fed_group;
CREATE TABLE fed_group (
  goid binary(16) NOT NULL,
  version int(11) NOT NULL,
  provider_goid binary(16) NOT NULL,
  name varchar(128) NOT NULL,
  description mediumtext,
  PRIMARY KEY  (goid),
  INDEX i_provider_goid (provider_goid),
  UNIQUE KEY i_name (provider_goid, name)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS fed_user_group;
CREATE TABLE fed_user_group (
  provider_goid binary(16) NOT NULL,
  fed_user_goid binary(16) NOT NULL,
  fed_group_goid binary(16) NOT NULL,
  PRIMARY KEY (provider_goid,fed_user_goid,fed_group_goid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS fed_group_virtual;
CREATE TABLE fed_group_virtual (
  goid binary(16) NOT NULL,
  version int(11) NOT NULL,
  provider_goid binary(16) NOT NULL,
  name varchar(128) NOT NULL,
  description mediumtext,
  x509_subject_dn_pattern varchar(255),
  saml_email_pattern varchar(128),
  properties mediumtext,
  PRIMARY KEY  (goid),
  INDEX i_provider_goid (provider_goid),
  INDEX i_x509_subject_dn_pattern (x509_subject_dn_pattern),
  INDEX i_saml_email_pattern (saml_email_pattern),
  UNIQUE KEY i_name (provider_goid, name)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for message_context_mapping_keys
--
DROP TABLE IF EXISTS message_context_mapping_keys;
CREATE TABLE message_context_mapping_keys (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  digested char(36) NOT NULL,
  mapping1_type varchar(36),
  mapping1_key varchar(128),
  mapping2_type varchar(36),
  mapping2_key varchar(128),
  mapping3_type varchar(36),
  mapping3_key varchar(128),
  mapping4_type varchar(36),
  mapping4_key varchar(128),
  mapping5_type varchar(36),
  mapping5_key varchar(128),
  create_time bigint(20),
  PRIMARY KEY (objectid),
  INDEX (digested)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for message_context_mapping_values
--
DROP TABLE IF EXISTS message_context_mapping_values;
CREATE TABLE message_context_mapping_values (
  objectid bigint(20) NOT NULL,
  digested char(36) NOT NULL,
  mapping_keys_oid bigint(20) NOT NULL,
  auth_user_provider_id binary(16),
  auth_user_id varchar(255),
  auth_user_unique_id varchar(255),
  service_operation varchar(255),
  mapping1_value varchar(255),
  mapping2_value varchar(255),
  mapping3_value varchar(255),
  mapping4_value varchar(255),
  mapping5_value varchar(255),
  create_time bigint(20),
  PRIMARY KEY  (objectid),
  FOREIGN KEY (mapping_keys_oid) REFERENCES message_context_mapping_keys (objectid),
  INDEX (digested)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8 COLLATE utf8_bin;

--
-- Table structure for table `audit`
--

DROP TABLE IF EXISTS audit_main;
CREATE TABLE audit_main (
  objectid bigint(20) NOT NULL,
  nodeid varchar(32) NOT NULL,
  time bigint(20) NOT NULL,
  audit_level varchar(12) NOT NULL,
  name varchar(255),
  message varchar(255) NOT NULL,
  ip_address varchar(39),
  user_name varchar(255),
  user_id varchar(255),
  provider_goid binary(16),
  signature varchar(1024),
  PRIMARY KEY  (objectid),
  KEY idx_time (time),
  KEY idx_ip_address (ip_address),
  KEY idx_prov_user (provider_goid, user_id)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table `audit_admin`
--

DROP TABLE IF EXISTS audit_admin;
CREATE TABLE audit_admin (
  objectid bigint(20) NOT NULL,
  entity_class varchar(255),
  entity_id bigint(20),
  action char(1),
  PRIMARY KEY  (objectid),
  KEY idx_class (entity_class),
  KEY idx_oid (entity_id),
  FOREIGN KEY (objectid) REFERENCES audit_main (objectid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table `audit_message`
--

DROP TABLE IF EXISTS audit_message;
CREATE TABLE audit_message (
  objectid bigint(20) NOT NULL,
  status varchar(32) NOT NULL,
  request_id varchar(40) NOT NULL,
  service_goid binary(16),
  operation_name varchar(255),
  authenticated tinyint(1) default '0',
  authenticationType int(11),
  request_length int(11) NOT NULL,
  response_length int(11),
  request_zipxml mediumblob,
  response_zipxml mediumblob,
  response_status int(11),
  routing_latency int(11),
  mapping_values_oid BIGINT(20),
  PRIMARY KEY  (objectid),
  KEY idx_status (status),
  KEY idx_request_id (request_id),
  KEY idx_service_oid (service_goid),
  FOREIGN KEY (objectid) REFERENCES audit_main (objectid) ON DELETE CASCADE,
  CONSTRAINT message_context_mapping FOREIGN KEY (mapping_values_oid) REFERENCES message_context_mapping_values (objectid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS audit_system;
CREATE TABLE audit_system (
  objectid bigint(20) NOT NULL,
  component_id integer NOT NULL,
  action varchar(32) NOT NULL,
  PRIMARY KEY (objectid),
  KEY idx_component_id (component_id),
  KEY idx_action (action),
  FOREIGN KEY (objectid) REFERENCES audit_main (objectid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS audit_detail;
CREATE TABLE audit_detail (
  objectid bigint(20) NOT NULL,
  audit_oid bigint(20) NOT NULL,
  time bigint(20) NOT NULL,
  component_id integer,
  ordinal integer,
  message_id integer NOT NULL,
  exception_message MEDIUMTEXT,
  PRIMARY KEY (objectid),
  KEY idx_component_id (component_id),
  KEY idx_audit_oid (audit_oid),
  FOREIGN KEY (audit_oid) REFERENCES audit_main (objectid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS audit_detail_params;
CREATE TABLE audit_detail_params (
  audit_detail_oid bigint(20) NOT NULL,
  position integer NOT NULL,
  value MEDIUMTEXT NOT NULL,
  PRIMARY KEY (audit_detail_oid, position),
  FOREIGN KEY (audit_detail_oid) REFERENCES audit_detail (objectid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS message_id;
CREATE TABLE message_id (
  messageid varchar(255) NOT NULL PRIMARY KEY,
  expires bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS counters;
CREATE TABLE counters (
  counterid bigint(20) NOT NULL,
  countername varchar(255) NOT NULL,
  cnt_sec bigint(20) default 0,
  cnt_min bigint(20) default 0,
  cnt_hr bigint(20) default 0,
  cnt_day bigint(20) default 0,
  cnt_mnt bigint(20) default 0,
  last_update bigint(20) default 0,
  PRIMARY KEY (counterid),
  UNIQUE (countername)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table 'resource_entry'
--

DROP TABLE IF EXISTS resource_entry;
CREATE TABLE resource_entry (
  goid binary(16) NOT NULL,
  version integer NOT NULL,
  description varchar(255),
  uri varchar(4096) NOT NULL,
  uri_hash varchar(128) NOT NULL,
  type varchar(32) NOT NULL,
  content_type varchar(255) NOT NULL,
  content mediumtext NOT NULL,
  resource_key1 varchar(4096),
  resource_key2 varchar(4096),
  resource_key3 varchar(4096),
  security_zone_goid binary(16),
  PRIMARY KEY (goid),
  UNIQUE KEY rduh_idx (uri_hash),
  CONSTRAINT resource_entry_security_zone FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Create default resources.
--

INSERT INTO resource_entry (goid, version, uri, uri_hash, type, content_type, content, resource_key1, security_zone_goid) VALUES (toGoid(0,-3),0,'http://schemas.xmlsoap.org/soap/envelope/','hC3quuokv29o8XDUK1vtJg29ywKS/fDsnJsj2chtn0maXa6J/7ga3LQxz12tlDYbLmJVWV/iP4PJsmBZ7lGiaQ==','XML_SCHEMA','text/xml','<?xml version=\'1.0\' encoding=\'UTF-8\' ?>\n<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n           xmlns:tns=\"http://schemas.xmlsoap.org/soap/envelope/\"\n           targetNamespace=\"http://schemas.xmlsoap.org/soap/envelope/\" >\n  <!-- Envelope, header and body -->\n  <xs:element name=\"Envelope\" type=\"tns:Envelope\" />\n  <xs:complexType name=\"Envelope\" >\n    <xs:sequence>\n      <xs:element ref=\"tns:Header\" minOccurs=\"0\" />\n      <xs:element ref=\"tns:Body\" minOccurs=\"1\" />\n      <xs:any namespace=\"##other\" minOccurs=\"0\" maxOccurs=\"unbounded\" processContents=\"lax\" />\n    </xs:sequence>\n    <xs:anyAttribute namespace=\"##other\" processContents=\"lax\" />\n  </xs:complexType>\n  <xs:element name=\"Header\" type=\"tns:Header\" />\n  <xs:complexType name=\"Header\" >\n    <xs:sequence>\n      <xs:any namespace=\"##other\" minOccurs=\"0\" maxOccurs=\"unbounded\" processContents=\"lax\" />\n    </xs:sequence>\n    <xs:anyAttribute namespace=\"##other\" processContents=\"lax\" />\n  </xs:complexType>\n  <xs:element name=\"Body\" type=\"tns:Body\" />\n  <xs:complexType name=\"Body\" >\n    <xs:sequence>\n      <xs:any namespace=\"##any\" minOccurs=\"0\" maxOccurs=\"unbounded\" processContents=\"lax\" />\n    </xs:sequence>\n    <xs:anyAttribute namespace=\"##any\" processContents=\"lax\" >\n          <xs:annotation>\n            <xs:documentation>\n                  Prose in the spec does not specify that attributes are allowed on the Body element\n                </xs:documentation>\n          </xs:annotation>\n        </xs:anyAttribute>\n  </xs:complexType>\n  <!-- Global Attributes.  The following attributes are intended to be usable via qualified attribute names on any complex type referencing them.  -->\n  <xs:attribute name=\"mustUnderstand\" >\n     <xs:simpleType>\n     <xs:restriction base=\'xs:boolean\'>\n           <xs:pattern value=\'0|1\' />\n         </xs:restriction>\n   </xs:simpleType>\n  </xs:attribute>\n  <xs:attribute name=\"actor\" type=\"xs:anyURI\" />\n  <xs:simpleType name=\"encodingStyle\" >\n    <xs:annotation>\n          <xs:documentation>\n            \'encodingStyle\' indicates any canonicalization conventions followed in the contents of the containing element.  For example, the value \'http://schemas.xmlsoap.org/soap/encoding/\' indicates the pattern described in SOAP specification\n          </xs:documentation>\n        </xs:annotation>\n    <xs:list itemType=\"xs:anyURI\" />\n  </xs:simpleType>\n  <xs:attribute name=\"encodingStyle\" type=\"tns:encodingStyle\" />\n  <xs:attributeGroup name=\"encodingStyle\" >\n    <xs:attribute ref=\"tns:encodingStyle\" />\n  </xs:attributeGroup>  <xs:element name=\"Fault\" type=\"tns:Fault\" />\n  <xs:complexType name=\"Fault\" final=\"extension\" >\n    <xs:annotation>\n          <xs:documentation>\n            Fault reporting structure\n          </xs:documentation>\n        </xs:annotation>\n    <xs:sequence>\n      <xs:element name=\"faultcode\" type=\"xs:QName\" />\n      <xs:element name=\"faultstring\" type=\"xs:string\" />\n      <xs:element name=\"faultactor\" type=\"xs:anyURI\" minOccurs=\"0\" />\n      <xs:element name=\"detail\" type=\"tns:detail\" minOccurs=\"0\" />\n    </xs:sequence>\n  </xs:complexType>\n  <xs:complexType name=\"detail\">\n    <xs:sequence>\n      <xs:any namespace=\"##any\" minOccurs=\"0\" maxOccurs=\"unbounded\" processContents=\"lax\" />\n    </xs:sequence>\n    <xs:anyAttribute namespace=\"##any\" processContents=\"lax\" />\n  </xs:complexType>\n</xs:schema>','http://schemas.xmlsoap.org/soap/envelope/',NULL);
INSERT INTO resource_entry (goid, version, uri, uri_hash, type, content_type, content, resource_key1, security_zone_goid) VALUES (toGoid(0,-4),0,'http://www.w3.org/2003/05/soap-envelope/','/IwS8Jif23iT/LGYVajOwoHmLxd/Acxqv8VZoeG7SN/5Qp0gcKmM+pnzTYc1qeaqg0YucLMOt3mmhPzH/tcpUQ==','XML_SCHEMA','text/xml','<?xml version=\'1.0\'?>\n<!-- Schema defined in the SOAP Version 1.2 Part 1 specification\n     Recommendation:\n     http://www.w3.org/TR/2003/REC-soap12-part1-20030624/\n\n     Copyright (C)2003 W3C(R) (MIT, ERCIM, Keio), All Rights Reserved.\n     W3C viability, trademark, document use and software licensing rules\n     apply.\n     http://www.w3.org/Consortium/Legal/\n\n     This document is governed by the W3C Software License [1] as\n     described in the FAQ [2].\n\n     [1] http://www.w3.org/Consortium/Legal/copyright-software-19980720\n     [2] http://www.w3.org/Consortium/Legal/IPR-FAQ-20000620.html#DTD\n-->\n\n<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n           xmlns:tns=\"http://www.w3.org/2003/05/soap-envelope\"\n           targetNamespace=\"http://www.w3.org/2003/05/soap-envelope\" \n		   elementFormDefault=\"qualified\" >\n\n  <xs:import namespace=\"http://www.w3.org/XML/1998/namespace\" \n             schemaLocation=\"http://www.w3.org/2001/xml.xsd\"/>\n\n  <!-- Envelope, header and body -->\n  <xs:element name=\"Envelope\" type=\"tns:Envelope\" />\n  <xs:complexType name=\"Envelope\" >\n    <xs:sequence>\n      <xs:element ref=\"tns:Header\" minOccurs=\"0\" />\n      <xs:element ref=\"tns:Body\" minOccurs=\"1\" />\n    </xs:sequence>\n    <xs:anyAttribute namespace=\"##other\" processContents=\"lax\" />\n  </xs:complexType>\n\n  <xs:element name=\"Header\" type=\"tns:Header\" />\n  <xs:complexType name=\"Header\" >\n    <xs:annotation>\n	  <xs:documentation>\n	  Elements replacing the wildcard MUST be namespace qualified, but can be in the targetNamespace\n	  </xs:documentation>\n	</xs:annotation>\n    <xs:sequence>\n      <xs:any namespace=\"##any\" processContents=\"lax\" minOccurs=\"0\" maxOccurs=\"unbounded\"  />\n    </xs:sequence>\n    <xs:anyAttribute namespace=\"##other\" processContents=\"lax\" />\n  </xs:complexType>\n  \n  <xs:element name=\"Body\" type=\"tns:Body\" />\n  <xs:complexType name=\"Body\" >\n    <xs:sequence>\n      <xs:any namespace=\"##any\" processContents=\"lax\" minOccurs=\"0\" maxOccurs=\"unbounded\" />\n    </xs:sequence>\n    <xs:anyAttribute namespace=\"##other\" processContents=\"lax\" />\n  </xs:complexType>\n\n  <!-- Global Attributes.  The following attributes are intended to be\n  usable via qualified attribute names on any complex type referencing\n  them.  -->\n  <xs:attribute name=\"mustUnderstand\" type=\"xs:boolean\" default=\"0\" />\n  <xs:attribute name=\"relay\" type=\"xs:boolean\" default=\"0\" />\n  <xs:attribute name=\"role\" type=\"xs:anyURI\" />\n\n  <!-- \'encodingStyle\' indicates any canonicalization conventions\n  followed in the contents of the containing element.  For example, the\n  value \'http://www.w3.org/2003/05/soap-encoding\' indicates the pattern\n  described in the SOAP Version 1.2 Part 2: Adjuncts Recommendation -->\n\n  <xs:attribute name=\"encodingStyle\" type=\"xs:anyURI\" />\n\n  <xs:element name=\"Fault\" type=\"tns:Fault\" />\n  <xs:complexType name=\"Fault\" final=\"extension\" >\n    <xs:annotation>\n	  <xs:documentation>\n	    Fault reporting structure\n	  </xs:documentation>\n	</xs:annotation>\n    <xs:sequence>\n      <xs:element name=\"Code\" type=\"tns:faultcode\" />\n      <xs:element name=\"Reason\" type=\"tns:faultreason\" />\n      <xs:element name=\"Node\" type=\"xs:anyURI\" minOccurs=\"0\" />\n	  <xs:element name=\"Role\" type=\"xs:anyURI\" minOccurs=\"0\" />\n      <xs:element name=\"Detail\" type=\"tns:detail\" minOccurs=\"0\" />\n    </xs:sequence>\n  </xs:complexType>\n\n  <xs:complexType name=\"faultreason\" >\n    <xs:sequence>\n	  <xs:element name=\"Text\" type=\"tns:reasontext\" \n                  minOccurs=\"1\"  maxOccurs=\"unbounded\" />\n	</xs:sequence>\n  </xs:complexType>\n\n  <xs:complexType name=\"reasontext\" >\n    <xs:simpleContent>\n	  <xs:extension base=\"xs:string\" >\n	    <xs:attribute ref=\"xml:lang\" use=\"required\" />\n	  </xs:extension>\n	</xs:simpleContent>\n  </xs:complexType>\n  \n  <xs:complexType name=\"faultcode\">\n    <xs:sequence>\n      <xs:element name=\"Value\"\n                  type=\"tns:faultcodeEnum\"/>\n      <xs:element name=\"Subcode\"\n                  type=\"tns:subcode\"\n                  minOccurs=\"0\"/>\n    </xs:sequence>\n  </xs:complexType>\n\n  <xs:simpleType name=\"faultcodeEnum\">\n    <xs:restriction base=\"xs:QName\">\n      <xs:enumeration value=\"tns:DataEncodingUnknown\"/>\n      <xs:enumeration value=\"tns:MustUnderstand\"/>\n      <xs:enumeration value=\"tns:Receiver\"/>\n      <xs:enumeration value=\"tns:Sender\"/>\n      <xs:enumeration value=\"tns:VersionMismatch\"/>\n    </xs:restriction>\n  </xs:simpleType>\n\n  <xs:complexType name=\"subcode\">\n    <xs:sequence>\n      <xs:element name=\"Value\"\n                  type=\"xs:QName\"/>\n      <xs:element name=\"Subcode\"\n                  type=\"tns:subcode\"\n                  minOccurs=\"0\"/>\n    </xs:sequence>\n  </xs:complexType>\n\n  <xs:complexType name=\"detail\">\n    <xs:sequence>\n      <xs:any namespace=\"##any\" processContents=\"lax\" minOccurs=\"0\" maxOccurs=\"unbounded\"  />\n    </xs:sequence>\n    <xs:anyAttribute namespace=\"##other\" processContents=\"lax\" /> \n  </xs:complexType>\n\n  <!-- Global element declaration and complex type definition for header entry returned due to a mustUnderstand fault -->\n  <xs:element name=\"NotUnderstood\" type=\"tns:NotUnderstoodType\" />\n  <xs:complexType name=\"NotUnderstoodType\" >\n    <xs:attribute name=\"qname\" type=\"xs:QName\" use=\"required\" />\n  </xs:complexType>\n\n\n  <!-- Global element and associated types for managing version transition as described in Appendix A of the SOAP Version 1.2 Part 1 Recommendation  -->  <xs:complexType name=\"SupportedEnvType\" >\n    <xs:attribute name=\"qname\" type=\"xs:QName\" use=\"required\" />\n  </xs:complexType>\n\n  <xs:element name=\"Upgrade\" type=\"tns:UpgradeType\" />\n  <xs:complexType name=\"UpgradeType\" >\n    <xs:sequence>\n	  <xs:element name=\"SupportedEnvelope\" type=\"tns:SupportedEnvType\" minOccurs=\"1\" maxOccurs=\"unbounded\" />\n	</xs:sequence>\n  </xs:complexType>\n\n\n</xs:schema>','http://www.w3.org/2003/05/soap-envelope',NULL);
INSERT INTO resource_entry (goid, version, uri, uri_hash, type, content_type, content, resource_key1, security_zone_goid) VALUES (toGoid(0,-5),0,'http://www.w3.org/2001/xml.xsd','hVcrKrS/aEB3urzQRjRATz5Jr2R4ai52xKbb/R2iaclst0ENOxLEU+IPdEtmrfiKGq0HOlCG3JDTTliMnoL0Zg==','XML_SCHEMA','text/xml','<?xml version=\'1.0\'?>\n<xs:schema targetNamespace=\"http://www.w3.org/XML/1998/namespace\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xml:lang=\"en\">\n\n <xs:annotation>\n  <xs:documentation>\n   See http://www.w3.org/XML/1998/namespace.html and\n   http://www.w3.org/TR/REC-xml for information about this namespace.\n\n    This schema document describes the XML namespace, in a form\n    suitable for import by other schema documents.\n\n    Note that local names in this namespace are intended to be defined\n    only by the World Wide Web Consortium or its subgroups.  The\n    following names are currently defined in this namespace and should\n    not be used with conflicting semantics by any Working Group,\n    specification, or document instance:\n\n    base (as an attribute name): denotes an attribute whose value\n         provides a URI to be used as the base for interpreting any\n         relative URIs in the scope of the element on which it\n         appears; its value is inherited.  This name is reserved\n         by virtue of its definition in the XML Base specification.\n\n    id   (as an attribute name): denotes an attribute whose value\n         should be interpreted as if declared to be of type ID.\n         This name is reserved by virtue of its definition in the\n         xml:id specification.\n\n    lang (as an attribute name): denotes an attribute whose value\n         is a language code for the natural language of the content of\n         any element; its value is inherited.  This name is reserved\n         by virtue of its definition in the XML specification.\n\n    space (as an attribute name): denotes an attribute whose\n         value is a keyword indicating what whitespace processing\n         discipline is intended for the content of the element; its\n         value is inherited.  This name is reserved by virtue of its\n         definition in the XML specification.\n\n    Father (in any context at all): denotes Jon Bosak, the chair of\n         the original XML Working Group.  This name is reserved by\n         the following decision of the W3C XML Plenary and\n         XML Coordination groups:\n\n             In appreciation for his vision, leadership and dedication\n             the W3C XML Plenary on this 10th day of February, 2000\n             reserves for Jon Bosak in perpetuity the XML name\n             xml:Father\n  </xs:documentation>\n </xs:annotation>\n\n <xs:annotation>\n  <xs:documentation>This schema defines attributes and an attribute group\n        suitable for use by\n        schemas wishing to allow xml:base, xml:lang, xml:space or xml:id\n        attributes on elements they define.\n\n        To enable this, such a schema must import this schema\n        for the XML namespace, e.g. as follows:\n        &lt;schema . . .>\n         . . .\n         &lt;import namespace=\"http://www.w3.org/XML/1998/namespace\"\n                    schemaLocation=\"http://www.w3.org/2001/xml.xsd\"/>\n\n        Subsequently, qualified reference to any of the attributes\n        or the group defined below will have the desired effect, e.g.\n\n        &lt;type . . .>\n         . . .\n         &lt;attributeGroup ref=\"xml:specialAttrs\"/>\n \n         will define a type which will schema-validate an instance\n         element with any of those attributes</xs:documentation>\n </xs:annotation>\n\n <xs:annotation>\n  <xs:documentation>In keeping with the XML Schema WG\'s standard versioning\n   policy, this schema document will persist at\n   http://www.w3.org/2007/08/xml.xsd.\n   At the date of issue it can also be found at\n   http://www.w3.org/2001/xml.xsd.\n   The schema document at that URI may however change in the future,\n   in order to remain compatible with the latest version of XML Schema\n   itself, or with the XML namespace itself.  In other words, if the XML\n   Schema or XML namespaces change, the version of this document at\n   http://www.w3.org/2001/xml.xsd will change\n   accordingly; the version at\n   http://www.w3.org/2007/08/xml.xsd will not change.\n  </xs:documentation>\n </xs:annotation>\n\n <xs:attribute name=\"lang\">\n  <xs:annotation>\n   <xs:documentation>Attempting to install the relevant ISO 2- and 3-letter\n         codes as the enumerated possible values is probably never\n         going to be a realistic possibility.  See\n         RFC 3066 at http://www.ietf.org/rfc/rfc3066.txt and the IANA registry\n         at http://www.iana.org/assignments/lang-tag-apps.htm for\n         further information.\n\n         The union allows for the \'un-declaration\' of xml:lang with\n         the empty string.</xs:documentation>\n  </xs:annotation>\n  <xs:simpleType>\n   <xs:union memberTypes=\"xs:language\">\n    <xs:simpleType>\n     <xs:restriction base=\"xs:string\">\n      <xs:enumeration value=\"\"/>\n     </xs:restriction>\n    </xs:simpleType>\n   </xs:union>\n  </xs:simpleType>\n </xs:attribute>\n\n <xs:attribute name=\"space\">\n  <xs:simpleType>\n   <xs:restriction base=\"xs:NCName\">\n    <xs:enumeration value=\"default\"/>\n    <xs:enumeration value=\"preserve\"/>\n   </xs:restriction>\n  </xs:simpleType>\n </xs:attribute>\n\n <xs:attribute name=\"base\" type=\"xs:anyURI\">\n  <xs:annotation>\n   <xs:documentation>See http://www.w3.org/TR/xmlbase/ for\n                     information about this attribute.</xs:documentation>\n  </xs:annotation>\n </xs:attribute>\n\n <xs:attribute name=\"id\" type=\"xs:ID\">\n  <xs:annotation>\n   <xs:documentation>See http://www.w3.org/TR/xml-id/ for\n                     information about this attribute.</xs:documentation>\n  </xs:annotation>\n </xs:attribute>\n\n <xs:attributeGroup name=\"specialAttrs\">\n  <xs:attribute ref=\"xml:base\"/>\n  <xs:attribute ref=\"xml:lang\"/>\n  <xs:attribute ref=\"xml:space\"/>\n  <xs:attribute ref=\"xml:id\"/>\n </xs:attributeGroup>\n\n</xs:schema>','http://www.w3.org/XML/1998/namespace',NULL);
INSERT INTO resource_entry (goid, version, uri, uri_hash, type, content_type, content, resource_key1, security_zone_goid) VALUES (toGoid(0,-6),0,'http://www.w3.org/2001/datatypes.dtd','CnGeQLLg3aDZGm+VXQAHZEimjslNt6DgjHWn3RZ8VH3haj30QvOihEtZxgzq9y68dj9YSJ8JP71BQVEJ+9ycYg==','DTD','text/plain','<!--\n        DTD for XML Schemas: Part 2: Datatypes\n        $Id: datatypes.dtd,v 1.23 2001/03/16 17:36:30 ht Exp $\n        Note this DTD is NOT normative, or even definitive. - - the\n        prose copy in the datatypes REC is the definitive version\n        (which shouldn\'t differ from this one except for this comment\n        and entity expansions, but just in case)\n  -->\n\n<!--\n        This DTD cannot be used on its own, it is intended\n        only for incorporation in XMLSchema.dtd, q.v.\n  -->\n\n<!-- Define all the element names, with optional prefix -->\n<!ENTITY % simpleType \"%p;simpleType\">\n<!ENTITY % restriction \"%p;restriction\">\n<!ENTITY % list \"%p;list\">\n<!ENTITY % union \"%p;union\">\n<!ENTITY % maxExclusive \"%p;maxExclusive\">\n<!ENTITY % minExclusive \"%p;minExclusive\">\n<!ENTITY % maxInclusive \"%p;maxInclusive\">\n<!ENTITY % minInclusive \"%p;minInclusive\">\n<!ENTITY % totalDigits \"%p;totalDigits\">\n<!ENTITY % fractionDigits \"%p;fractionDigits\">\n<!ENTITY % length \"%p;length\">\n<!ENTITY % minLength \"%p;minLength\">\n<!ENTITY % maxLength \"%p;maxLength\">\n<!ENTITY % enumeration \"%p;enumeration\">\n<!ENTITY % whiteSpace \"%p;whiteSpace\">\n<!ENTITY % pattern \"%p;pattern\">\n\n<!--\n        Customisation entities for the ATTLIST of each element\n        type. Define one of these if your schema takes advantage\n        of the anyAttribute=\'##other\' in the schema for schemas\n  -->\n\n<!ENTITY % simpleTypeAttrs \"\">\n<!ENTITY % restrictionAttrs \"\">\n<!ENTITY % listAttrs \"\">\n<!ENTITY % unionAttrs \"\">\n<!ENTITY % maxExclusiveAttrs \"\">\n<!ENTITY % minExclusiveAttrs \"\">\n<!ENTITY % maxInclusiveAttrs \"\">\n<!ENTITY % minInclusiveAttrs \"\">\n<!ENTITY % totalDigitsAttrs \"\">\n<!ENTITY % fractionDigitsAttrs \"\">\n<!ENTITY % lengthAttrs \"\">\n<!ENTITY % minLengthAttrs \"\">\n<!ENTITY % maxLengthAttrs \"\">\n<!ENTITY % enumerationAttrs \"\">\n<!ENTITY % whiteSpaceAttrs \"\">\n<!ENTITY % patternAttrs \"\">\n\n<!-- Define some entities for informative use as attribute\n        types -->\n<!ENTITY % URIref \"CDATA\">\n<!ENTITY % XPathExpr \"CDATA\">\n<!ENTITY % QName \"NMTOKEN\">\n<!ENTITY % QNames \"NMTOKENS\">\n<!ENTITY % NCName \"NMTOKEN\">\n<!ENTITY % nonNegativeInteger \"NMTOKEN\">\n<!ENTITY % boolean \"(true|false)\">\n<!ENTITY % simpleDerivationSet \"CDATA\">\n<!--\n        #all or space-separated list drawn from derivationChoice\n  -->\n\n<!--\n        Note that the use of \'facet\' below is less restrictive\n        than is really intended:  There should in fact be no\n        more than one of each of minInclusive, minExclusive,\n        maxInclusive, maxExclusive, totalDigits, fractionDigits,\n        length, maxLength, minLength within datatype,\n        and the min- and max- variants of Inclusive and Exclusive\n        are mutually exclusive. On the other hand,  pattern and\n        enumeration may repeat.\n  -->\n<!ENTITY % minBound \"(%minInclusive; | %minExclusive;)\">\n<!ENTITY % maxBound \"(%maxInclusive; | %maxExclusive;)\">\n<!ENTITY % bounds \"%minBound; | %maxBound;\">\n<!ENTITY % numeric \"%totalDigits; | %fractionDigits;\">\n<!ENTITY % ordered \"%bounds; | %numeric;\">\n<!ENTITY % unordered\n   \"%pattern; | %enumeration; | %whiteSpace; | %length; |\n   %maxLength; | %minLength;\">\n<!ENTITY % facet \"%ordered; | %unordered;\">\n<!ENTITY % facetAttr \n        \"value CDATA #REQUIRED\n        id ID #IMPLIED\">\n<!ENTITY % fixedAttr \"fixed %boolean; #IMPLIED\">\n<!ENTITY % facetModel \"(%annotation;)?\">\n<!ELEMENT %simpleType;\n        ((%annotation;)?, (%restriction; | %list; | %union;))>\n<!ATTLIST %simpleType;\n    name      %NCName; #IMPLIED\n    final     %simpleDerivationSet; #IMPLIED\n    id        ID       #IMPLIED\n    %simpleTypeAttrs;>\n<!-- name is required at top level -->\n<!ELEMENT %restriction; ((%annotation;)?,\n                         (%restriction1; |\n                          ((%simpleType;)?,(%facet;)*)),\n                         (%attrDecls;))>\n<!ATTLIST %restriction;\n    base      %QName;                  #IMPLIED\n    id        ID       #IMPLIED\n    %restrictionAttrs;>\n<!--\n        base and simpleType child are mutually exclusive,\n        one is required.\n\n        restriction is shared between simpleType and\n        simpleContent and complexContent (in XMLSchema.xsd).\n        restriction1 is for the latter cases, when this\n        is restricting a complex type, as is attrDecls.\n  -->\n<!ELEMENT %list; ((%annotation;)?,(%simpleType;)?)>\n<!ATTLIST %list;\n    itemType      %QName;             #IMPLIED\n    id        ID       #IMPLIED\n    %listAttrs;>\n<!--\n        itemType and simpleType child are mutually exclusive,\n        one is required\n  -->\n<!ELEMENT %union; ((%annotation;)?,(%simpleType;)*)>\n<!ATTLIST %union;\n    id            ID       #IMPLIED\n    memberTypes   %QNames;            #IMPLIED\n    %unionAttrs;>\n<!--\n        At least one item in memberTypes or one simpleType\n        child is required\n  -->\n\n<!ELEMENT %maxExclusive; %facetModel;>\n<!ATTLIST %maxExclusive;\n        %facetAttr;\n        %fixedAttr;\n        %maxExclusiveAttrs;>\n<!ELEMENT %minExclusive; %facetModel;>\n<!ATTLIST %minExclusive;\n        %facetAttr;\n        %fixedAttr;\n        %minExclusiveAttrs;>\n\n<!ELEMENT %maxInclusive; %facetModel;>\n<!ATTLIST %maxInclusive;\n        %facetAttr;\n        %fixedAttr;\n        %maxInclusiveAttrs;>\n<!ELEMENT %minInclusive; %facetModel;>\n<!ATTLIST %minInclusive;\n        %facetAttr;\n        %fixedAttr;\n        %minInclusiveAttrs;>\n\n<!ELEMENT %totalDigits; %facetModel;>\n<!ATTLIST %totalDigits;\n        %facetAttr;\n        %fixedAttr;\n        %totalDigitsAttrs;>\n<!ELEMENT %fractionDigits; %facetModel;>\n<!ATTLIST %fractionDigits;\n        %facetAttr;\n        %fixedAttr;\n        %fractionDigitsAttrs;>\n\n<!ELEMENT %length; %facetModel;>\n<!ATTLIST %length;\n        %facetAttr;\n        %fixedAttr;\n        %lengthAttrs;>\n<!ELEMENT %minLength; %facetModel;>\n<!ATTLIST %minLength;\n        %facetAttr;\n        %fixedAttr;\n        %minLengthAttrs;>\n<!ELEMENT %maxLength; %facetModel;>\n<!ATTLIST %maxLength;\n        %facetAttr;\n        %fixedAttr;\n        %maxLengthAttrs;>\n\n<!-- This one can be repeated -->\n<!ELEMENT %enumeration; %facetModel;>\n<!ATTLIST %enumeration;\n        %facetAttr;\n        %enumerationAttrs;>\n\n<!ELEMENT %whiteSpace; %facetModel;>\n<!ATTLIST %whiteSpace;\n        %facetAttr;\n        %fixedAttr;\n        %whiteSpaceAttrs;>\n\n<!-- This one can be repeated -->\n<!ELEMENT %pattern; %facetModel;>\n<!ATTLIST %pattern;\n        %facetAttr;\n        %patternAttrs;>\n','datatypes',NULL);
INSERT INTO resource_entry (goid, version, uri, uri_hash, type, content_type, content, resource_key1, security_zone_goid) VALUES (toGoid(0,-7),0,'http://www.w3.org/2001/XMLSchema.dtd','8yxOhhglB4ig2jm9Tl3Jb7wJ53OS0+aRQBJgpdleDH/HFJ9+XjbMys52YTDpRTqn8q1Zt8xAUMQEl9kEdjAlMw==','DTD','text/plain','<!-- DTD for XML Schemas: Part 1: Structures\n     Public Identifier: \"-//W3C//DTD XMLSCHEMA 200102//EN\"\n     Official Location: http://www.w3.org/2001/XMLSchema.dtd -->\n<!-- $Id: XMLSchema.dtd,v 1.31 2001/10/24 15:50:16 ht Exp $ -->\n<!-- Note this DTD is NOT normative, or even definitive. -->           <!--d-->\n<!-- prose copy in the structures REC is the definitive version -->    <!--d-->\n<!-- (which shouldn\'t differ from this one except for this -->         <!--d-->\n<!-- comment and entity expansions, but just in case) -->              <!--d-->\n<!-- With the exception of cases with multiple namespace\n     prefixes for the XML Schema namespace, any XML document which is\n     not valid per this DTD given redefinitions in its internal subset of the\n     \'p\' and \'s\' parameter entities below appropriate to its namespace\n     declaration of the XML Schema namespace is almost certainly not\n     a valid schema. -->\n\n<!-- The simpleType element and its constituent parts\n     are defined in XML Schema: Part 2: Datatypes -->\n<!ENTITY % xs-datatypes PUBLIC \'datatypes\' \'datatypes.dtd\' >\n\n<!ENTITY % p \'xs:\'> <!-- can be overriden in the internal subset of a\n                         schema document to establish a different\n                         namespace prefix -->\n<!ENTITY % s \':xs\'> <!-- if %p is defined (e.g. as foo:) then you must\n                         also define %s as the suffix for the appropriate\n                         namespace declaration (e.g. :foo) -->\n<!ENTITY % nds \'xmlns%s;\'>\n\n<!-- Define all the element names, with optional prefix -->\n<!ENTITY % schema \"%p;schema\">\n<!ENTITY % complexType \"%p;complexType\">\n<!ENTITY % complexContent \"%p;complexContent\">\n<!ENTITY % simpleContent \"%p;simpleContent\">\n<!ENTITY % extension \"%p;extension\">\n<!ENTITY % element \"%p;element\">\n<!ENTITY % unique \"%p;unique\">\n<!ENTITY % key \"%p;key\">\n<!ENTITY % keyref \"%p;keyref\">\n<!ENTITY % selector \"%p;selector\">\n<!ENTITY % field \"%p;field\">\n<!ENTITY % group \"%p;group\">\n<!ENTITY % all \"%p;all\">\n<!ENTITY % choice \"%p;choice\">\n<!ENTITY % sequence \"%p;sequence\">\n<!ENTITY % any \"%p;any\">\n<!ENTITY % anyAttribute \"%p;anyAttribute\">\n<!ENTITY % attribute \"%p;attribute\">\n<!ENTITY % attributeGroup \"%p;attributeGroup\">\n<!ENTITY % include \"%p;include\">\n<!ENTITY % import \"%p;import\">\n<!ENTITY % redefine \"%p;redefine\">\n<!ENTITY % notation \"%p;notation\">\n\n<!-- annotation elements -->\n<!ENTITY % annotation \"%p;annotation\">\n<!ENTITY % appinfo \"%p;appinfo\">\n<!ENTITY % documentation \"%p;documentation\">\n\n<!-- Customisation entities for the ATTLIST of each element type.\n     Define one of these if your schema takes advantage of the\n     anyAttribute=\'##other\' in the schema for schemas -->\n\n<!ENTITY % schemaAttrs \'\'>\n<!ENTITY % complexTypeAttrs \'\'>\n<!ENTITY % complexContentAttrs \'\'>\n<!ENTITY % simpleContentAttrs \'\'>\n<!ENTITY % extensionAttrs \'\'>\n<!ENTITY % elementAttrs \'\'>\n<!ENTITY % groupAttrs \'\'>\n<!ENTITY % allAttrs \'\'>\n<!ENTITY % choiceAttrs \'\'>\n<!ENTITY % sequenceAttrs \'\'>\n<!ENTITY % anyAttrs \'\'>\n<!ENTITY % anyAttributeAttrs \'\'>\n<!ENTITY % attributeAttrs \'\'>\n<!ENTITY % attributeGroupAttrs \'\'>\n<!ENTITY % uniqueAttrs \'\'>\n<!ENTITY % keyAttrs \'\'>\n<!ENTITY % keyrefAttrs \'\'>\n<!ENTITY % selectorAttrs \'\'>\n<!ENTITY % fieldAttrs \'\'>\n<!ENTITY % includeAttrs \'\'>\n<!ENTITY % importAttrs \'\'>\n<!ENTITY % redefineAttrs \'\'>\n<!ENTITY % notationAttrs \'\'>\n<!ENTITY % annotationAttrs \'\'>\n<!ENTITY % appinfoAttrs \'\'>\n<!ENTITY % documentationAttrs \'\'>\n\n<!ENTITY % complexDerivationSet \"CDATA\">\n      <!-- #all or space-separated list drawn from derivationChoice -->\n<!ENTITY % blockSet \"CDATA\">\n      <!-- #all or space-separated list drawn from\n                      derivationChoice + \'substitution\' -->\n\n<!ENTITY % mgs \'%all; | %choice; | %sequence;\'>\n<!ENTITY % cs \'%choice; | %sequence;\'>\n<!ENTITY % formValues \'(qualified|unqualified)\'>\n\n\n<!ENTITY % attrDecls    \'((%attribute;| %attributeGroup;)*,(%anyAttribute;)?)\'>\n\n<!ENTITY % particleAndAttrs \'((%mgs; | %group;)?, %attrDecls;)\'>\n\n<!-- This is used in part2 -->\n<!ENTITY % restriction1 \'((%mgs; | %group;)?)\'>\n\n%xs-datatypes;\n\n<!-- the duplication below is to produce an unambiguous content model\n     which allows annotation everywhere -->\n<!ELEMENT %schema; ((%include; | %import; | %redefine; | %annotation;)*,\n                    ((%simpleType; | %complexType;\n                      | %element; | %attribute;\n                      | %attributeGroup; | %group;\n                      | %notation; ),\n                     (%annotation;)*)* )>\n<!ATTLIST %schema;\n   targetNamespace      %URIref;               #IMPLIED\n   version              CDATA                  #IMPLIED\n   %nds;                %URIref;               #FIXED \'http://www.w3.org/2001/XMLSchema\'\n   xmlns                CDATA                  #IMPLIED\n   finalDefault         %complexDerivationSet; \'\'\n   blockDefault         %blockSet;             \'\'\n   id                   ID                     #IMPLIED\n   elementFormDefault   %formValues;           \'unqualified\'\n   attributeFormDefault %formValues;           \'unqualified\'\n   xml:lang             CDATA                  #IMPLIED\n   %schemaAttrs;>\n<!-- Note the xmlns declaration is NOT in the Schema for Schemas,\n     because at the Infoset level where schemas operate,\n     xmlns(:prefix) is NOT an attribute! -->\n<!-- The declaration of xmlns is a convenience for schema authors -->\n \n<!-- The id attribute here and below is for use in external references\n     from non-schemas using simple fragment identifiers.\n     It is NOT used for schema-to-schema reference, internal or\n     external. -->\n\n<!-- a type is a named content type specification which allows attribute\n     declarations-->\n<!-- -->\n\n<!ELEMENT %complexType; ((%annotation;)?,\n                         (%simpleContent;|%complexContent;|\n                          %particleAndAttrs;))>\n\n<!ATTLIST %complexType;\n          name      %NCName;                        #IMPLIED\n          id        ID                              #IMPLIED\n          abstract  %boolean;                       #IMPLIED\n          final     %complexDerivationSet;          #IMPLIED\n          block     %complexDerivationSet;          #IMPLIED\n          mixed (true|false) \'false\'\n          %complexTypeAttrs;>\n\n<!-- particleAndAttrs is shorthand for a root type -->\n<!-- mixed is disallowed if simpleContent, overriden if complexContent\n     has one too. -->\n\n<!-- If anyAttribute appears in one or more referenced attributeGroups\n     and/or explicitly, the intersection of the permissions is used -->\n\n<!ELEMENT %complexContent; ((%annotation;)?, (%restriction;|%extension;))>\n<!ATTLIST %complexContent;\n          mixed (true|false) #IMPLIED\n          id    ID           #IMPLIED\n          %complexContentAttrs;>\n\n<!-- restriction should use the branch defined above, not the simple\n     one from part2; extension should use the full model  -->\n\n<!ELEMENT %simpleContent; ((%annotation;)?, (%restriction;|%extension;))>\n<!ATTLIST %simpleContent;\n          id    ID           #IMPLIED\n          %simpleContentAttrs;>\n\n<!-- restriction should use the simple branch from part2, not the \n     one defined above; extension should have no particle  -->\n\n<!ELEMENT %extension; ((%annotation;)?, (%particleAndAttrs;))>\n<!ATTLIST %extension;\n          base  %QName;      #REQUIRED\n          id    ID           #IMPLIED\n          %extensionAttrs;>\n\n<!-- an element is declared by either:\n a name and a type (either nested or referenced via the type attribute)\n or a ref to an existing element declaration -->\n\n<!ELEMENT %element; ((%annotation;)?, (%complexType;| %simpleType;)?,\n                     (%unique; | %key; | %keyref;)*)>\n<!-- simpleType or complexType only if no type|ref attribute -->\n<!-- ref not allowed at top level -->\n<!ATTLIST %element;\n            name               %NCName;               #IMPLIED\n            id                 ID                     #IMPLIED\n            ref                %QName;                #IMPLIED\n            type               %QName;                #IMPLIED\n            minOccurs          %nonNegativeInteger;   #IMPLIED\n            maxOccurs          CDATA                  #IMPLIED\n            nillable           %boolean;              #IMPLIED\n            substitutionGroup  %QName;                #IMPLIED\n            abstract           %boolean;              #IMPLIED\n            final              %complexDerivationSet; #IMPLIED\n            block              %blockSet;             #IMPLIED\n            default            CDATA                  #IMPLIED\n            fixed              CDATA                  #IMPLIED\n            form               %formValues;           #IMPLIED\n            %elementAttrs;>\n<!-- type and ref are mutually exclusive.\n     name and ref are mutually exclusive, one is required -->\n<!-- In the absence of type AND ref, type defaults to type of\n     substitutionGroup, if any, else the ur-type, i.e. unconstrained -->\n<!-- default and fixed are mutually exclusive -->\n\n<!ELEMENT %group; ((%annotation;)?,(%mgs;)?)>\n<!ATTLIST %group; \n          name        %NCName;               #IMPLIED\n          ref         %QName;                #IMPLIED\n          minOccurs   %nonNegativeInteger;   #IMPLIED\n          maxOccurs   CDATA                  #IMPLIED\n          id          ID                     #IMPLIED\n          %groupAttrs;>\n\n<!ELEMENT %all; ((%annotation;)?, (%element;)*)>\n<!ATTLIST %all;\n          minOccurs   (1)                    #IMPLIED\n          maxOccurs   (1)                    #IMPLIED\n          id          ID                     #IMPLIED\n          %allAttrs;>\n\n<!ELEMENT %choice; ((%annotation;)?, (%element;| %group;| %cs; | %any;)*)>\n<!ATTLIST %choice;\n          minOccurs   %nonNegativeInteger;   #IMPLIED\n          maxOccurs   CDATA                  #IMPLIED\n          id          ID                     #IMPLIED\n          %choiceAttrs;>\n\n<!ELEMENT %sequence; ((%annotation;)?, (%element;| %group;| %cs; | %any;)*)>\n<!ATTLIST %sequence;\n          minOccurs   %nonNegativeInteger;   #IMPLIED\n          maxOccurs   CDATA                  #IMPLIED\n          id          ID                     #IMPLIED\n          %sequenceAttrs;>\n\n<!-- an anonymous grouping in a model, or\n     a top-level named group definition, or a reference to same -->\n\n<!-- Note that if order is \'all\', group is not allowed inside.\n     If order is \'all\' THIS group must be alone (or referenced alone) at\n     the top level of a content model -->\n<!-- If order is \'all\', minOccurs==maxOccurs==1 on element/any inside -->\n<!-- Should allow minOccurs=0 inside order=\'all\' . . . -->\n\n<!ELEMENT %any; (%annotation;)?>\n<!ATTLIST %any;\n            namespace       CDATA                  \'##any\'\n            processContents (skip|lax|strict)      \'strict\'\n            minOccurs       %nonNegativeInteger;   \'1\'\n            maxOccurs       CDATA                  \'1\'\n            id              ID                     #IMPLIED\n            %anyAttrs;>\n\n<!-- namespace is interpreted as follows:\n                  ##any      - - any non-conflicting WFXML at all\n\n                  ##other    - - any non-conflicting WFXML from namespace other\n                                  than targetNamespace\n\n                  ##local    - - any unqualified non-conflicting WFXML/attribute\n                  one or     - - any non-conflicting WFXML from\n                  more URI        the listed namespaces\n                  references\n\n                  ##targetNamespace ##local may appear in the above list,\n                    with the obvious meaning -->\n\n<!ELEMENT %anyAttribute; (%annotation;)?>\n<!ATTLIST %anyAttribute;\n            namespace       CDATA              \'##any\'\n            processContents (skip|lax|strict)  \'strict\'\n            id              ID                 #IMPLIED\n            %anyAttributeAttrs;>\n<!-- namespace is interpreted as for \'any\' above -->\n\n<!-- simpleType only if no type|ref attribute -->\n<!-- ref not allowed at top level, name iff at top level -->\n<!ELEMENT %attribute; ((%annotation;)?, (%simpleType;)?)>\n<!ATTLIST %attribute;\n          name      %NCName;      #IMPLIED\n          id        ID            #IMPLIED\n          ref       %QName;       #IMPLIED\n          type      %QName;       #IMPLIED\n          use       (prohibited|optional|required) #IMPLIED\n          default   CDATA         #IMPLIED\n          fixed     CDATA         #IMPLIED\n          form      %formValues;  #IMPLIED\n          %attributeAttrs;>\n<!-- type and ref are mutually exclusive.\n     name and ref are mutually exclusive, one is required -->\n<!-- default for use is optional when nested, none otherwise -->\n<!-- default and fixed are mutually exclusive -->\n<!-- type attr and simpleType content are mutually exclusive -->\n\n<!-- an attributeGroup is a named collection of attribute decls, or a\n     reference thereto -->\n<!ELEMENT %attributeGroup; ((%annotation;)?,\n                       (%attribute; | %attributeGroup;)*,\n                       (%anyAttribute;)?) >\n<!ATTLIST %attributeGroup;\n                 name       %NCName;       #IMPLIED\n                 id         ID             #IMPLIED\n                 ref        %QName;        #IMPLIED\n                 %attributeGroupAttrs;>\n\n<!-- ref iff no content, no name.  ref iff not top level -->\n\n<!-- better reference mechanisms -->\n<!ELEMENT %unique; ((%annotation;)?, %selector;, (%field;)+)>\n<!ATTLIST %unique;\n          name     %NCName;       #REQUIRED\n	  id       ID             #IMPLIED\n	  %uniqueAttrs;>\n\n<!ELEMENT %key;    ((%annotation;)?, %selector;, (%field;)+)>\n<!ATTLIST %key;\n          name     %NCName;       #REQUIRED\n	  id       ID             #IMPLIED\n	  %keyAttrs;>\n\n<!ELEMENT %keyref; ((%annotation;)?, %selector;, (%field;)+)>\n<!ATTLIST %keyref;\n          name     %NCName;       #REQUIRED\n	  refer    %QName;        #REQUIRED\n	  id       ID             #IMPLIED\n	  %keyrefAttrs;>\n\n<!ELEMENT %selector; ((%annotation;)?)>\n<!ATTLIST %selector;\n          xpath %XPathExpr; #REQUIRED\n          id    ID          #IMPLIED\n          %selectorAttrs;>\n<!ELEMENT %field; ((%annotation;)?)>\n<!ATTLIST %field;\n          xpath %XPathExpr; #REQUIRED\n          id    ID          #IMPLIED\n          %fieldAttrs;>\n\n<!-- Schema combination mechanisms -->\n<!ELEMENT %include; (%annotation;)?>\n<!ATTLIST %include;\n          schemaLocation %URIref; #REQUIRED\n          id             ID       #IMPLIED\n          %includeAttrs;>\n\n<!ELEMENT %import; (%annotation;)?>\n<!ATTLIST %import;\n          namespace      %URIref; #IMPLIED\n          schemaLocation %URIref; #IMPLIED\n          id             ID       #IMPLIED\n          %importAttrs;>\n\n<!ELEMENT %redefine; (%annotation; | %simpleType; | %complexType; |\n                      %attributeGroup; | %group;)*>\n<!ATTLIST %redefine;\n          schemaLocation %URIref; #REQUIRED\n          id             ID       #IMPLIED\n          %redefineAttrs;>\n\n<!ELEMENT %notation; (%annotation;)?>\n<!ATTLIST %notation;\n	  name        %NCName;    #REQUIRED\n	  id          ID          #IMPLIED\n	  public      CDATA       #REQUIRED\n	  system      %URIref;    #IMPLIED\n	  %notationAttrs;>\n\n<!-- Annotation is either application information or documentation -->\n<!-- By having these here they are available for datatypes as well\n     as all the structures elements -->\n\n<!ELEMENT %annotation; (%appinfo; | %documentation;)*>\n<!ATTLIST %annotation; %annotationAttrs;>\n\n<!-- User must define annotation elements in internal subset for this\n     to work -->\n<!ELEMENT %appinfo; ANY>   <!-- too restrictive -->\n<!ATTLIST %appinfo;\n          source     %URIref;      #IMPLIED\n          id         ID         #IMPLIED\n          %appinfoAttrs;>\n<!ELEMENT %documentation; ANY>   <!-- too restrictive -->\n<!ATTLIST %documentation;\n          source     %URIref;   #IMPLIED\n          id         ID         #IMPLIED\n          xml:lang   CDATA      #IMPLIED\n          %documentationAttrs;>\n\n<!NOTATION XMLSchemaStructures PUBLIC\n           \'structures\' \'http://www.w3.org/2001/XMLSchema.xsd\' >\n<!NOTATION XML PUBLIC\n           \'REC-xml-1998-0210\' \'http://www.w3.org/TR/1998/REC-xml-19980210\' >\n','-//W3C//DTD XMLSCHEMA 200102//EN',NULL);

--
-- Table structure for table 'cluster_properties'
-- note that 'key' is unfortunately not a valid column name
--

DROP TABLE IF EXISTS cluster_properties;
CREATE TABLE cluster_properties (
  goid binary(16) NOT NULL,
  version integer NOT NULL,
  propkey varchar(128) NOT NULL,
  propvalue MEDIUMTEXT NOT NULL,
  properties MEDIUMTEXT,
  PRIMARY KEY (goid),
  UNIQUE(propkey)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Reserve Goid(0,-700001) id for cluster.hostname and insert default
--
INSERT INTO cluster_properties (goid, version, propkey, propvalue, properties) VALUES (toGoid(0,-700001),0,'cluster.hostname','',null);

DROP TABLE IF EXISTS sample_messages;
CREATE TABLE sample_messages (
  goid BINARY(16) NOT NULL,
  published_service_goid BINARY(16),
  name varchar(128) NOT NULL,
  xml mediumtext NOT NULL,
  operation_name varchar(128),
  security_zone_goid binary(16),
  INDEX i_ps_oid (published_service_goid),
  INDEX i_operation_name (operation_name),
  FOREIGN KEY (published_service_goid) REFERENCES published_service (goid) ON DELETE CASCADE,
  PRIMARY KEY (goid),
  CONSTRAINT sample_msg_security_zone FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS service_metrics;
CREATE TABLE service_metrics (
  goid BINARY(16) NOT NULL,
  nodeid VARCHAR(32) NOT NULL,
  published_service_goid binary(16) NOT NULL,
  resolution INTEGER NOT NULL,
  period_start BIGINT(20) NOT NULL,
  start_time BIGINT(20) NOT NULL,
  interval_size INTEGER NOT NULL,
  end_time BIGINT(20) NOT NULL,
  attempted INTEGER NOT NULL,
  authorized INTEGER NOT NULL,
  completed INTEGER NOT NULL,
  back_min INTEGER,
  back_max INTEGER,
  back_sum INTEGER NOT NULL,
  front_min INTEGER,
  front_max INTEGER,
  front_sum INTEGER NOT NULL,
  service_state VARCHAR(16),
  INDEX i_sm_nodeid (nodeid),
  INDEX i_sm_servicegoid (published_service_goid),
  INDEX i_sm_pstart (period_start),
  PRIMARY KEY (goid),
  UNIQUE (nodeid, published_service_goid, resolution, period_start)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS service_metrics_details;
CREATE TABLE service_metrics_details (
  service_metrics_goid BINARY(16) NOT NULL,
  mapping_values_oid BIGINT(20) NOT NULL,
  attempted INTEGER NOT NULL,
  authorized INTEGER NOT NULL,
  completed INTEGER NOT NULL,
  back_min INTEGER,
  back_max INTEGER,
  back_sum INTEGER NOT NULL,
  front_min INTEGER,
  front_max INTEGER,
  front_sum INTEGER NOT NULL,
  PRIMARY KEY (service_metrics_goid, mapping_values_oid),
  CONSTRAINT service_metrics_goid FOREIGN KEY (service_metrics_goid) REFERENCES service_metrics (goid) ON DELETE CASCADE,
  FOREIGN KEY (mapping_values_oid) REFERENCES message_context_mapping_values (objectid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table 'service_documents'
--

DROP TABLE IF EXISTS service_documents;
CREATE TABLE service_documents (
  goid binary(16) NOT NULL,
  version int(11) NOT NULL,
  service_goid binary(16) NOT NULL,
  uri MEDIUMTEXT,
  type VARCHAR(32) NOT NULL,
  content_type VARCHAR(32) NOT NULL,
  content MEDIUMTEXT,
  INDEX i_sd_service_type (service_goid, type),
  PRIMARY KEY (goid),
  FOREIGN KEY (service_goid) REFERENCES published_service (goid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;


--
-- Table structure for table 'keystore_file'
--

DROP TABLE IF EXISTS keystore_file;
CREATE TABLE keystore_file (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  format varchar(128) NOT NULL,
  databytes mediumblob,
  properties mediumtext,
  PRIMARY KEY (objectid),
  UNIQUE(name)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

-- placeholder for legacy Software Static, never loaded or saved
insert into keystore_file (objectid, version, name, format, databytes, properties) values (0, 0, 'Software Static', 'ss', null, null);

-- tar.gz of items in sca 6000 keydata directory
insert into keystore_file (objectid, version, name, format, databytes, properties) values (1, 0, 'HSM', 'hsm.sca.targz', null, null);

-- bytes of a PKCS#12 keystore
insert into keystore_file (objectid, version, name, format, databytes, properties) values (2, 0, 'Software DB', 'sdb.pkcs12', null, null);

-- placeholder for ID reserved for Luna, never loaded or saved
insert into keystore_file (objectid, version, name, format, databytes, properties) values (3, 0, 'SafeNet HSM', 'luna', null, null);

-- serialized NcipherKeyStoreData for an nCipher keystore
insert into keystore_file (objectid, version, name, format, databytes, properties) values (4, 0, 'nCipher HSM', 'hsm.NcipherKeyStoreData', null, null);

-- Reserve OID 5 for "Generic" keystores
-- insert into keystore_file (objectid, version, name, format, databytes, properties) values (5, 0, 'Generic', 'generic', null, null);

--
-- Table structure for keystore_key_metadata (stores metadata for keys in keystores accessible to the Gateway)
--
DROP TABLE IF EXISTS keystore_key_metadata;
CREATE TABLE keystore_key_metadata (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL default 0,
  keystore_file_oid bigint(20) NOT NULL,
  alias varchar(255) NOT NULL,
  security_zone_goid binary(16),
  PRIMARY KEY (objectid),
  UNIQUE KEY i_ks_alias (keystore_file_oid, alias),
  CONSTRAINT keystore_key_metadata_keystore_file FOREIGN KEY (keystore_file_oid) REFERENCES keystore_file (objectid) ON DELETE CASCADE,
  CONSTRAINT keystore_key_metadata_security_zone FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS shared_keys;
CREATE TABLE shared_keys (
  encodingid varchar(32) NOT NULL,
  b64edval varchar(2048) NOT NULL,
  primary key(encodingid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

-- Secure password storage
DROP TABLE IF EXISTS secure_password;
CREATE TABLE secure_password (
  goid binary(16) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  description varchar(256),
  usage_from_variable tinyint(1) NOT NULL DEFAULT 0,
  encoded_password mediumtext NOT NULL,
  last_update bigint(20) NOT NULL DEFAULT 0,
  type varchar(64) NOT NULL DEFAULT 'PASSWORD',
  security_zone_goid binary(16),
  PRIMARY KEY (goid),
  UNIQUE(name),
  CONSTRAINT secure_password_security_zone FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS http_configuration;
CREATE TABLE http_configuration (
  goid binary(16) NOT NULL,
  version integer NOT NULL,
  host varchar(128) NOT NULL,
  port int(5) NOT NULL DEFAULT 0,
  protocol varchar(8) DEFAULT NULL,
  path varchar(4096) DEFAULT NULL,
  username varchar(255) DEFAULT NULL,
  password_goid binary(16) DEFAULT NULL,
  ntlm_host varchar(128) DEFAULT NULL,
  ntlm_domain varchar(255) DEFAULT NULL,
  tls_version varchar(8) DEFAULT NULL,
  tls_key_use varchar(8) DEFAULT 'DEFAULT',
  tls_keystore_oid bigint(20) NOT NULL DEFAULT 0,
  tls_key_alias varchar(255) DEFAULT NULL,
  tls_cipher_suites varchar(4096) DEFAULT NULL,
  timeout_connect int(10) NOT NULL DEFAULT -1,
  timeout_read int(10) NOT NULL DEFAULT -1,
  follow_redirects tinyint(1) NOT NULL DEFAULT 0,
  proxy_use varchar(8) DEFAULT 'DEFAULT',
  proxy_host varchar(128) DEFAULT NULL,
  proxy_port int(5) NOT NULL DEFAULT 0,
  proxy_username varchar(255) DEFAULT NULL,
  proxy_password_goid binary(16) DEFAULT NULL,
  security_zone_goid binary(16),
  primary key(goid),
  FOREIGN KEY (password_goid) REFERENCES secure_password (goid),
  FOREIGN KEY (proxy_password_goid) REFERENCES secure_password (goid),
  CONSTRAINT http_config_security_zone FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- HTTP and HTTPS listeners and properties
--

DROP TABLE IF EXISTS connector;
CREATE TABLE connector (
  goid binary(16) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  enabled tinyint(1) NOT NULL DEFAULT 0,
  port int(8) NOT NULL,
  scheme varchar(128) NOT NULL DEFAULT 'http',
  endpoints varchar(256) NOT NULL,
  secure tinyint(1) NOT NULL DEFAULT 0,
  client_auth tinyint(1) NOT NULL DEFAULT 0,
  keystore_oid bigint(20) NULL,
  key_alias varchar(255) NULL,
  security_zone_goid binary(16),
  PRIMARY KEY (goid),
  CONSTRAINT connector_security_zone FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS connector_property;
CREATE TABLE connector_property (
  connector_goid binary(16) NOT NULL,
  name varchar(128) NOT NULL,
  value MEDIUMTEXT NOT NULL,
  FOREIGN KEY (connector_goid) REFERENCES connector (goid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for JDBC Connections
--
DROP TABLE IF EXISTS jdbc_connection;
CREATE TABLE jdbc_connection (
  goid binary(16) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  driver_class varchar(256) NOT NULL,
  jdbc_url varchar(4096) NOT NULL,
  user_name varchar(128) NOT NULL,
  password varchar(64) NOT NULL,
  enabled tinyint(1) NOT NULL DEFAULT 1,
  min_pool_size integer NOT NULL DEFAULT 3,
  max_pool_size integer NOT NULL DEFAULT 15,
  additional_properties mediumtext,
  security_zone_goid binary(16),
  PRIMARY KEY (goid),
  CONSTRAINT jdbc_connection_security_zone FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL,
  UNIQUE(name)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for SiteMinder Configuration
--
DROP TABLE IF EXISTS siteminder_configuration;
CREATE TABLE siteminder_configuration (
  goid binary(16) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  agent_name varchar(256) NOT NULL,
  address varchar(128) NOT NULL,
  secret varchar(4096) NOT NULL,
  ipcheck boolean DEFAULT false NOT NULL,
  update_sso_token boolean DEFAULT false NOT NULL,
  enabled tinyint(1) NOT NULL DEFAULT 1,
  hostname varchar(255) NOT NULL,
  fipsmode integer NOT NULL DEFAULT 0,
  host_configuration varchar(256) DEFAULT NULL,
  user_name varchar(256) DEFAULT NULL,
  password_goid binary(16) DEFAULT NULL,
  noncluster_failover boolean DEFAULT false NOT NULL,
  cluster_threshold integer DEFAULT 50,
  security_zone_goid binary(16),
  FOREIGN KEY (password_goid) REFERENCES secure_password (goid),
  PRIMARY KEY (goid),
  CONSTRAINT siteminder_configuration_security_zone FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL,
  INDEX i_name (name),
  UNIQUE(name)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for SiteMinder Configuration additional Properties
--
DROP TABLE IF EXISTS siteminder_configuration_property;
CREATE TABLE siteminder_configuration_property (
  goid binary(16) NOT NULL,
  name varchar(128) NOT NULL,
  value MEDIUMTEXT NOT NULL,
  FOREIGN KEY (goid) REFERENCES siteminder_configuration (goid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for UDDI Registries
-- Note: base_url is unique and has a size limit of 255 bytes, which is the max allowed for a unique key
-- in mysql when using utf-8 encoding. It is the max size of a hostname
--
DROP TABLE IF EXISTS uddi_registries;
CREATE TABLE uddi_registries (
  goid binary(16) NOT NULL,
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
  security_zone_goid binary(16),
  PRIMARY KEY (goid),
  UNIQUE(name),
  CONSTRAINT uddi_reg_security_zone FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table for UDDI registry subscription information
--
DROP TABLE IF EXISTS uddi_registry_subscription;
CREATE TABLE uddi_registry_subscription (
  goid binary(16) NOT NULL,
  version integer NOT NULL,
  uddi_registry_goid binary(16) NOT NULL,
  uddi_subscription_key varchar(255),
  uddi_subscription_expiry_time bigint NOT NULL,
  uddi_subscription_notified_time bigint NOT NULL,
  uddi_subscription_check_time bigint NOT NULL,
  PRIMARY KEY (goid),
  UNIQUE KEY  (uddi_registry_goid),
  FOREIGN KEY (uddi_registry_goid) REFERENCES uddi_registries (goid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for Gateway WSDLs published to UDDI. Known as 'Proxied Business Services'
-- Entity UDDIProxiedService
-- the general_keyword value is used to identify all services which originated from the same published service's wsdl
--
DROP TABLE IF EXISTS uddi_proxied_service_info;
CREATE TABLE uddi_proxied_service_info (
  goid binary(16) NOT NULL,
  published_service_goid binary(16) NOT NULL,
  uddi_registry_goid binary(16) NOT NULL,
  version integer NOT NULL,
  uddi_business_key varchar(255) NOT NULL,
  uddi_business_name varchar(255) NOT NULL,
  update_proxy_on_local_change tinyint(1) NOT NULL DEFAULT 0,
  remove_other_bindings tinyint(1) NOT NULL DEFAULT 0,
  created_from_existing tinyint(1) NOT NULL DEFAULT 0,
  metrics_enabled tinyint(1) NOT NULL DEFAULT 0,
  publish_wspolicy_enabled tinyint(1) NOT NULL DEFAULT 0,
  publish_wspolicy_full tinyint(1) NOT NULL DEFAULT 0,
  publish_wspolicy_inlined tinyint(1) NOT NULL DEFAULT 0,
  publish_type varchar(32) NOT NULL,
  wsdl_hash varchar(32) NOT NULL,
  properties mediumtext,
  security_zone_goid binary(16),
  PRIMARY KEY (goid),
  UNIQUE KEY  (published_service_goid),
  FOREIGN KEY (published_service_goid) REFERENCES published_service (goid) ON DELETE CASCADE,
  FOREIGN KEY (uddi_registry_goid) REFERENCES uddi_registries (goid) ON DELETE CASCADE,
  CONSTRAINT uddi_prox_svc_info_security_zone FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Child of uddi_proxied_service_info, represents the publishing of a specific wsdl:service from the Gateway's WSDL
--
DROP TABLE IF EXISTS uddi_proxied_service;
CREATE TABLE uddi_proxied_service (
  goid binary(16) NOT NULL,
  version integer NOT NULL,
  uddi_proxied_service_info_goid binary(16) NOT NULL,
  uddi_service_key varchar(255) NOT NULL,
  uddi_service_name varchar(255) NOT NULL,
  wsdl_service_name varchar(255) NOT NULL,
  wsdl_service_namespace varchar(255) NOT NULL,
  PRIMARY KEY (goid),
  UNIQUE KEY (uddi_proxied_service_info_goid, wsdl_service_name, wsdl_service_namespace),
  UNIQUE (uddi_service_key),
  FOREIGN KEY (uddi_proxied_service_info_goid) REFERENCES uddi_proxied_service_info (goid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Child of uddi_proxied_service_info, runtime information regarding its publish status
--
DROP TABLE IF EXISTS uddi_publish_status;
CREATE TABLE uddi_publish_status (
  goid binary(16) NOT NULL,
  version integer NOT NULL,
  uddi_proxied_service_info_goid binary(16) NOT NULL,
  publish_status varchar(32) NOT NULL,
  fail_count integer NOT NULL DEFAULT 0,
  PRIMARY KEY (goid),
  UNIQUE (uddi_proxied_service_info_goid),
  FOREIGN KEY (uddi_proxied_service_info_goid) REFERENCES uddi_proxied_service_info (goid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for password policy
--

DROP TABLE IF EXISTS password_policy;
CREATE TABLE password_policy (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  properties mediumtext,
  internal_identity_provider_goid binary(16),
  PRIMARY KEY (objectid),
  UNIQUE KEY  (internal_identity_provider_goid),
  FOREIGN KEY (internal_identity_provider_goid) REFERENCES identity_provider (goid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;
-- STIG default:
INSERT INTO password_policy (objectid, version, properties, internal_identity_provider_goid) VALUES (-2, 0, '<?xml version="1.0" encoding="UTF-8"?><java version="1.6.0_21" class="java.beans.XMLDecoder"> <object class="java.util.TreeMap">  <void method="put">   <string>allowableChangesPerDay</string>   <boolean>true</boolean>  </void>  <void method="put">   <string>charDiffMinimum</string>   <int>4</int>  </void>  <void method="put">   <string>forcePasswordChangeNewUser</string>   <boolean>true</boolean>  </void>  <void method="put">   <string>lowerMinimum</string>   <int>1</int>  </void>  <void method="put">   <string>maxPasswordLength</string>   <int>32</int>  </void>  <void method="put">   <string>minPasswordLength</string>   <int>8</int>  </void>  <void method="put">   <string>noRepeatingCharacters</string>   <boolean>true</boolean>  </void>  <void method="put">   <string>numberMinimum</string>   <int>1</int>  </void>  <void method="put">   <string>passwordExpiry</string>   <int>90</int>  </void>  <void method="put">   <string>repeatFrequency</string>   <int>10</int>  </void>  <void method="put">   <string>symbolMinimum</string>   <int>1</int>  </void>  <void method="put">   <string>upperMinimum</string>   <int>1</int>  </void> </object></java>', toGoid(0,-2));

--
-- Tracking table for metrics and policy attachment publishing
--
DROP TABLE IF EXISTS uddi_business_service_status;
CREATE TABLE uddi_business_service_status (
  goid binary(16) NOT NULL,
  version integer NOT NULL,
  published_service_goid binary(16) NOT NULL,
  uddi_registry_goid binary(16) NOT NULL,
  uddi_service_key varchar(255) NOT NULL,
  uddi_service_name varchar(255) NOT NULL,
  uddi_policy_publish_url varchar(255),
  uddi_policy_url varchar(255),
  uddi_policy_tmodel_key varchar(255),
  policy_status varchar(32) NOT NULL,
  uddi_metrics_tmodel_key varchar(255),
  metrics_reference_status varchar(32) NOT NULL,
  PRIMARY KEY (goid),
  FOREIGN KEY (published_service_goid) REFERENCES published_service (goid) ON DELETE CASCADE,
  FOREIGN KEY (uddi_registry_goid) REFERENCES uddi_registries (goid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table for a service published from (or otherwise associated with) UDDI
--
DROP TABLE IF EXISTS uddi_service_control;
CREATE TABLE uddi_service_control (
  goid binary(16) NOT NULL,
  version integer NOT NULL,
  published_service_goid binary(16) NOT NULL,
  uddi_registry_goid binary(16) NOT NULL,
  uddi_business_key varchar(255) NOT NULL,
  uddi_business_name varchar(255) NOT NULL,
  uddi_service_key varchar(255) NOT NULL,
  uddi_service_name varchar(255) NOT NULL,
  wsdl_service_name varchar(255) NOT NULL,
  wsdl_port_name varchar(255) NOT NULL,
  wsdl_port_binding varchar(255) NOT NULL,
  wsdl_port_binding_namespace varchar(255) NOT NULL,
  under_uddi_control tinyint(1) NOT NULL DEFAULT 0,
  monitoring_enabled tinyint(1) NOT NULL DEFAULT 0,
  update_wsdl_on_change tinyint(1) NOT NULL DEFAULT 0,
  disable_service_on_change tinyint(1) NOT NULL DEFAULT 0,
  metrics_enabled tinyint(1) NOT NULL DEFAULT 0,
  publish_wspolicy_enabled tinyint(1) NOT NULL DEFAULT 0,
  publish_wspolicy_full tinyint(1) NOT NULL DEFAULT 0,
  publish_wspolicy_inlined tinyint(1) NOT NULL DEFAULT 0,
  has_had_endpoints_removed tinyint(1) NOT NULL DEFAULT 0,
  has_been_overwritten tinyint(1) NOT NULL DEFAULT 0,
  security_zone_goid binary(16),
  PRIMARY KEY (goid),
  UNIQUE KEY  (published_service_goid),
  FOREIGN KEY (published_service_goid) REFERENCES published_service (goid) ON DELETE CASCADE,
  FOREIGN KEY (uddi_registry_goid) REFERENCES uddi_registries (goid) ON DELETE CASCADE,
  CONSTRAINT uddi_svc_ctrl_security_zone FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- UDDIServiceControl runtime information. Stores the last modified timestamp for a service we are monitoring in UDDI
-- Useful as it stops us processing duplicate notifications from UDDI
--
DROP TABLE IF EXISTS uddi_service_control_monitor_runtime;
CREATE TABLE uddi_service_control_monitor_runtime (
  goid binary(16) NOT NULL,
  version integer NOT NULL,
  uddi_service_control_goid binary(16) NOT NULL,
  last_uddi_modified_timestamp bigint(20) NOT NULL,
  access_point_url varchar(4096) NOT NULL,
  PRIMARY KEY (goid),
  UNIQUE KEY  (uddi_service_control_goid),
  FOREIGN KEY (uddi_service_control_goid) REFERENCES uddi_service_control (goid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table rbac_role
--

DROP TABLE IF EXISTS rbac_role;
CREATE TABLE rbac_role (
  goid binary(16) NOT NULL,
  version int(11) NOT NULL,
  name varchar(128) default NULL,
  tag varchar(36) default NULL,
  entity_type varchar(255),
  entity_oid bigint(20),
  entity_goid binary(16),
  description mediumtext,
  user_created tinyint(1) NOT NULL default 0,
  PRIMARY KEY (goid),
  UNIQUE KEY name (name),
  INDEX i_rbacrole_etype (entity_type),
  INDEX i_rbacrole_eoid (entity_oid),
  INDEX i_rbacrole_egoid (entity_goid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table rbac_assignment
--

DROP TABLE IF EXISTS rbac_assignment;
CREATE TABLE rbac_assignment (
  goid binary(16) NOT NULL,
  provider_goid binary(16) NOT NULL,
  role_goid binary(16) NOT NULL,
  identity_id varchar(255) NOT NULL,
  entity_type varchar(50) NOT NULL,
  PRIMARY KEY  (goid),
  UNIQUE KEY unique_assignment (provider_goid,role_goid,identity_id, entity_type),
  FOREIGN KEY (role_goid) REFERENCES rbac_role (goid) ON DELETE CASCADE,
  CONSTRAINT rbac_assignment_provider FOREIGN KEY (provider_goid) REFERENCES identity_provider (goid) ON DELETE CASCADE,
  INDEX i_rbacassign_poid (provider_goid),
  INDEX i_rbacassign_uid (identity_id)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table rbac_permission
--

DROP TABLE IF EXISTS rbac_permission;
CREATE TABLE rbac_permission (
  goid binary(16) NOT NULL,
  version int(11) NOT NULL,
  role_goid binary(16) default NULL,
  operation_type varchar(16) default NULL,
  other_operation varchar(255) default NULL,
  entity_type varchar(255) default NULL,
  PRIMARY KEY (goid),
  FOREIGN KEY (role_goid) REFERENCES rbac_role (goid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table rbac_predicate
--

DROP TABLE IF EXISTS rbac_predicate;
CREATE TABLE rbac_predicate (
  goid binary(16) NOT NULL,
  version int(11) NOT NULL,
  permission_goid binary(16) default NULL,
  PRIMARY KEY (goid),
  FOREIGN KEY (permission_goid) REFERENCES rbac_permission (goid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table rbac_predicate_attribute
--

DROP TABLE IF EXISTS rbac_predicate_attribute;
CREATE TABLE rbac_predicate_attribute (
  goid binary(16) NOT NULL,
  attribute varchar(255) default NULL,
  value varchar(255) default NULL,
  mode varchar(255) default NULL,
  PRIMARY KEY (goid),
  FOREIGN KEY (goid) REFERENCES rbac_predicate (goid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table rbac_predicate_security_zone
--
DROP TABLE IF EXISTS rbac_predicate_security_zone;
CREATE TABLE rbac_predicate_security_zone (
  goid binary(16) NOT NULL,
  security_zone_goid binary(16),
  PRIMARY KEY (goid),
  FOREIGN KEY (goid) REFERENCES rbac_predicate (goid) ON DELETE CASCADE,
  FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table rbac_predicate_oid
--

DROP TABLE IF EXISTS rbac_predicate_oid;
CREATE TABLE rbac_predicate_oid (
  goid binary(16) NOT NULL,
  entity_id varchar(255) default NULL,
  PRIMARY KEY (goid),
  FOREIGN KEY (goid) REFERENCES rbac_predicate (goid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table rbac_predicate_folder
--
DROP TABLE IF EXISTS rbac_predicate_folder;
CREATE TABLE rbac_predicate_folder (
  goid binary(16) NOT NULL,
  folder_goid binary(16) NOT NULL,
  transitive boolean NOT NULL,
  PRIMARY KEY (goid),
  FOREIGN KEY (goid) REFERENCES rbac_predicate (goid) ON DELETE CASCADE,
  FOREIGN KEY (folder_goid) REFERENCES folder (goid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table rbac_predicate_entityfolder
--
DROP TABLE IF EXISTS rbac_predicate_entityfolder;
CREATE TABLE rbac_predicate_entityfolder (
  goid binary(16) NOT NULL,
  entity_type VARCHAR(64) NOT NULL,
  entity_id VARCHAR(255) NOT NULL,
  PRIMARY KEY (goid),
  FOREIGN KEY (goid) REFERENCES rbac_predicate (goid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for assertion_access (allows assertion usage to be restricted via security zones)
--
DROP TABLE IF EXISTS assertion_access;
CREATE TABLE assertion_access (
  goid binary(16) NOT NULL,
  version int(11) NOT NULL,
  name varchar(255) NOT NULL,
  security_zone_goid binary(16),
  PRIMARY KEY (goid),
  UNIQUE KEY i_name (name),
  CONSTRAINT assertion_access_security_zone FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

-- Create Administrator role
-- XXX NOTE!! COPIED in Role#ADMIN_ROLE_OID                                                                      `

INSERT INTO rbac_role (goid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (toGoid(0, -100),0,'Administrator','ADMIN',null,null,null,'Users assigned to the {0} role have full access to the gateway.',0);
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -101), 0, toGoid(0, -100), 'CREATE', null, 'ANY');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -102), 0, toGoid(0, -100), 'READ',   null, 'ANY');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -103), 0, toGoid(0, -100), 'UPDATE', null, 'ANY');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -104), 0, toGoid(0, -100), 'DELETE', null, 'ANY');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -105), 0, toGoid(0, -100), 'OTHER', 'log-viewer', 'LOG_SINK');
-- Create Operator role
INSERT INTO rbac_role (goid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (toGoid(0, -150),0,'Operator',null,null,null,null,'Users assigned to the {0} role have read only access to the gateway.',0);
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -151), 0, toGoid(0, -150), 'READ', null, 'ANY');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -152), 0, toGoid(0, -150), 'OTHER', 'log-viewer', 'LOG_SINK');

-- Create other canned roles
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -201),0,toGoid(0, -200),'READ',NULL,'USER');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -202),0,toGoid(0, -201));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -202),'providerId','0000000000000000fffffffffffffffe','eq');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -203),0,toGoid(0, -200),'READ',NULL,'ID_PROVIDER_CONFIG');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -204),0,toGoid(0, -203));
INSERT INTO rbac_predicate_oid (goid, entity_id) VALUES (toGoid(0, -204),'0000000000000000fffffffffffffffe');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -205),0,toGoid(0, -200),'UPDATE',NULL,'USER');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -206),0,toGoid(0, -205));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -206),'providerId','0000000000000000fffffffffffffffe','eq');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -207),0,toGoid(0, -200),'READ',NULL,'GROUP');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -208),0,toGoid(0, -207));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -208),'providerId','0000000000000000fffffffffffffffe','eq');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -209),0,toGoid(0, -200),'DELETE',NULL,'USER');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -210),0,toGoid(0, -209));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -210),'providerId','0000000000000000fffffffffffffffe','eq');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -211),0,toGoid(0, -200),'CREATE',NULL,'USER');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -212),0,toGoid(0, -211));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -212),'providerId','0000000000000000fffffffffffffffe','eq');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -213),0,toGoid(0, -200),'CREATE',NULL,'GROUP');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -214),0,toGoid(0, -213));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -214),'providerId','0000000000000000fffffffffffffffe','eq');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -215),0,toGoid(0, -200),'DELETE',NULL,'GROUP');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -216),0,toGoid(0, -215));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -216),'providerId','0000000000000000fffffffffffffffe','eq');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -217),0,toGoid(0, -200),'UPDATE',NULL,'GROUP');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -218),0,toGoid(0, -217));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -218),'providerId','0000000000000000fffffffffffffffe','eq');

INSERT INTO rbac_role (goid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (toGoid(0, -250),0,'Publish External Identity Providers', null,null,null,null, 'Users assigned to the {0} role have the ability to create new external identity providers.',0);
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -251),0,toGoid(0, -250),'CREATE',NULL,'ID_PROVIDER_CONFIG');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -252),0,toGoid(0, -251));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -252),'typeVal','2','eq');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -253),0,toGoid(0, -250),'CREATE',NULL,'ID_PROVIDER_CONFIG');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -254),0,toGoid(0, -253));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -254),'typeVal','3','eq');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -258),0,toGoid(0, -250),'CREATE',NULL,'ID_PROVIDER_CONFIG');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -259),0,toGoid(0, -258));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -259),'typeVal','4','eq');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -255),0,toGoid(0, -250),'READ',NULL,'TRUSTED_CERT');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -256),0,toGoid(0, -250),'READ',NULL,'SSG_KEYSTORE');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -257),0,toGoid(0, -250),'READ',NULL,'SSG_KEY_ENTRY');

INSERT INTO rbac_role (goid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (toGoid(0, -300),0,'Search Users and Groups', null,null,null,null, 'Users assigned to the {0} role have permission to search and view users and groups in all identity providers.',0);
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -301),0,toGoid(0, -300),'READ',NULL,'USER');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -302),0,toGoid(0, -300),'READ',NULL,'ID_PROVIDER_CONFIG');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -303),0,toGoid(0, -300),'READ',NULL,'GROUP');

INSERT INTO rbac_role (goid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (toGoid(0, -350),0,'Publish Webservices', null,null,null,null, 'Users assigned to the {0} role have the ability to publish new web services.' ,0);
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -351),0,toGoid(0, -350),'READ',NULL,'GROUP');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -352),0,toGoid(0, -350),'READ',NULL,'ID_PROVIDER_CONFIG');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -353),0,toGoid(0, -350),'READ',NULL,'USER');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -354),0,toGoid(0, -350),'CREATE',NULL,'SERVICE');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -355),0,toGoid(0, -350),'READ',NULL,'SERVICE_TEMPLATE');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -356),0,toGoid(0, -350),'READ',NULL,'UDDI_REGISTRY');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -357),0,toGoid(0, -350),'READ',NULL,'JDBC_CONNECTION');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -358),0,toGoid(0, -350),'READ',NULL,'HTTP_CONFIGURATION');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -359),0,toGoid(0, -350),'READ',NULL,'ENCAPSULATED_ASSERTION');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -360),0,toGoid(0, -350),'READ',NULL,'ASSERTION_ACCESS');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -361),0,toGoid(0, -350),'CREATE',NULL,'ASSERTION_ACCESS');

INSERT INTO rbac_role (goid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (toGoid(0, -400),1,'Manage Webservices', null,null,null,null, 'Users assigned to the {0} role have the ability to publish new services and edit existing ones.',0);
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -401),0,toGoid(0, -400),'READ',NULL,'ID_PROVIDER_CONFIG');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -402),0,toGoid(0, -400),'READ',NULL,'GROUP');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -403),0,toGoid(0, -400),'READ',NULL,'USER');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -404),0,toGoid(0, -400),'READ',NULL,'SERVICE');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -405),0,toGoid(0, -400),'CREATE',NULL,'SERVICE');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -406),0,toGoid(0, -400),'UPDATE',NULL,'SERVICE');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -407),0,toGoid(0, -400),'DELETE',NULL,'SERVICE');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -408),0,toGoid(0, -400),'READ',NULL,'CLUSTER_INFO');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -409),0,toGoid(0, -400),'READ',NULL,'SERVICE_USAGE');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -410),0,toGoid(0, -400),'READ',NULL,'METRICS_BIN');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -411),0,toGoid(0, -400),'READ',NULL,'AUDIT_MESSAGE');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -412),0,toGoid(0, -400),'READ',NULL,'JMS_CONNECTION');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -413),0,toGoid(0, -400),'READ',NULL,'JMS_ENDPOINT');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -414),0,toGoid(0, -400),'READ',NULL,'SERVICE_TEMPLATE');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -415),0,toGoid(0, -400),'READ',NULL,'POLICY');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -416),0,toGoid(0, -400),'UPDATE',NULL,'POLICY');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -417),0,toGoid(0, -400),'CREATE',NULL,'FOLDER');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -418),0,toGoid(0, -400),'READ',  NULL,'FOLDER');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -419),0,toGoid(0, -400),'UPDATE',NULL,'FOLDER');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -420),0,toGoid(0, -400),'DELETE',NULL,'FOLDER');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -421),0,toGoid(0, -400),'CREATE',NULL,'POLICY_ALIAS');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -422),0,toGoid(0, -400),'READ',  NULL,'POLICY_ALIAS');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -423),0,toGoid(0, -400),'UPDATE',NULL,'POLICY_ALIAS');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -424),0,toGoid(0, -400),'DELETE',NULL,'POLICY_ALIAS');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -425),0,toGoid(0, -400),'CREATE',NULL,'SERVICE_ALIAS');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -426),0,toGoid(0, -400),'READ',  NULL,'SERVICE_ALIAS');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -427),0,toGoid(0, -400),'UPDATE',NULL,'SERVICE_ALIAS');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -428),0,toGoid(0, -400),'DELETE',NULL,'SERVICE_ALIAS');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -429),0,toGoid(0, -400),'READ',NULL,'UDDI_REGISTRY');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -430),0,toGoid(0, -400),'READ',NULL,'UDDI_PROXIED_SERVICE_INFO');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -431),0,toGoid(0, -400),'UPDATE',NULL,'UDDI_PROXIED_SERVICE_INFO');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -432),0,toGoid(0, -400),'DELETE',NULL,'UDDI_PROXIED_SERVICE_INFO');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -433),0,toGoid(0, -400),'CREATE',NULL,'UDDI_PROXIED_SERVICE_INFO');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -434),0,toGoid(0, -400),'READ',NULL,'UDDI_SERVICE_CONTROL');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -435),0,toGoid(0, -400),'UPDATE',NULL,'UDDI_SERVICE_CONTROL');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -436),0,toGoid(0, -400),'DELETE',NULL,'UDDI_SERVICE_CONTROL');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -437),0,toGoid(0, -400),'CREATE',NULL,'UDDI_SERVICE_CONTROL');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -438),0,toGoid(0, -400),'READ',NULL,'JDBC_CONNECTION');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -439),0,toGoid(0, -400),'READ',NULL,'HTTP_CONFIGURATION');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -440),0,toGoid(0, -400),'CREATE',NULL,'POLICY');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -441),0,toGoid(0, -400),'READ',NULL,'ENCAPSULATED_ASSERTION');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -442),0,toGoid(0, -400),'CREATE',NULL,'ASSERTION_ACCESS');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -443),0,toGoid(0, -400),'READ',NULL,'ASSERTION_ACCESS');

INSERT INTO rbac_role (goid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (toGoid(0, -450),0,'View Audit Records', null,null,null,null, 'Users assigned to the {0} role have the ability to view audits in the manager.',0);
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -451),0,toGoid(0, -450),'READ',NULL,'CLUSTER_INFO');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -452),0,toGoid(0, -450),'READ',NULL,'AUDIT_RECORD');

INSERT INTO rbac_role (goid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (toGoid(0, -500),0,'View Service Metrics', null,null,null,null, 'Users assigned to the {0} role have the ability to monitor service metrics in the manager.',0);
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -501),0,toGoid(0, -500),'READ',NULL,'METRICS_BIN');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -502),0,toGoid(0, -500),'READ',NULL,'SERVICE');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -503),0,toGoid(0, -500),'READ',NULL,'CLUSTER_INFO');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -504),0,toGoid(0, -500),'READ',NULL,'SERVICE_USAGE');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -505),0,toGoid(0, -500),'READ',NULL,'FOLDER');

INSERT INTO rbac_role (goid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (toGoid(0, -550),0,'Manage Cluster Status', null,null,null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete cluster status information.',0);
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -551),0,toGoid(0, -550),'READ',NULL,'CLUSTER_INFO');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -552),0,toGoid(0, -550),'UPDATE',NULL,'CLUSTER_INFO');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -553),0,toGoid(0, -550),'DELETE',NULL,'CLUSTER_INFO');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -554),0,toGoid(0, -550),'READ',NULL,'METRICS_BIN');

INSERT INTO rbac_role (goid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (toGoid(0, -600),0,'Manage Certificates (truststore)', null,null,null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete trusted certificates and policies for revocation checking.',0);
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -601),0,toGoid(0, -600),'UPDATE',NULL,'TRUSTED_CERT');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -602),0,toGoid(0, -600),'READ',NULL,'TRUSTED_CERT');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -603),0,toGoid(0, -600),'DELETE',NULL,'TRUSTED_CERT');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -604),0,toGoid(0, -600),'CREATE',NULL,'TRUSTED_CERT');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -605),0,toGoid(0, -600),'UPDATE',NULL,'REVOCATION_CHECK_POLICY');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -606),0,toGoid(0, -600),'READ',NULL,'REVOCATION_CHECK_POLICY');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -607),0,toGoid(0, -600),'DELETE',NULL,'REVOCATION_CHECK_POLICY');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -608),0,toGoid(0, -600),'CREATE',NULL,'REVOCATION_CHECK_POLICY');

INSERT INTO rbac_role (goid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (toGoid(0, -650),0,'Manage Message Destinations', null,null,null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete message destinations.',0);
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -651),1,toGoid(0, -650),'READ',NULL,'JMS_CONNECTION');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -652),1,toGoid(0, -650),'DELETE',NULL,'JMS_CONNECTION');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -653),1,toGoid(0, -650),'CREATE',NULL,'JMS_CONNECTION');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -654),1,toGoid(0, -650),'UPDATE',NULL,'JMS_CONNECTION');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -655),1,toGoid(0, -650),'CREATE',NULL,'JMS_ENDPOINT');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -656),1,toGoid(0, -650),'DELETE',NULL,'JMS_ENDPOINT');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -657),1,toGoid(0, -650),'UPDATE',NULL,'JMS_ENDPOINT');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -658),1,toGoid(0, -650),'READ',NULL,'JMS_ENDPOINT');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -659),1,toGoid(0, -650),'READ',NULL,'SERVICE');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -660),1,toGoid(0, -650),'READ',NULL,'SSG_KEYSTORE');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -661),1,toGoid(0, -650),'READ',NULL,'SSG_KEY_ENTRY');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -662),1,toGoid(0, -650),'READ',NULL,'SSG_ACTIVE_CONNECTOR');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -663),0,toGoid(0, -662));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -663),'type','MqNative','eq');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -664),1,toGoid(0, -650),'DELETE',NULL,'SSG_ACTIVE_CONNECTOR');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -665),0,toGoid(0, -664));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -665),'type','MqNative','eq');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -666),1,toGoid(0, -650),'CREATE',NULL,'SSG_ACTIVE_CONNECTOR');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -667),0,toGoid(0, -666));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -667),'type','MqNative','eq');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -668),1,toGoid(0, -650),'UPDATE',NULL,'SSG_ACTIVE_CONNECTOR');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -669),0,toGoid(0, -668));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -669),'type','MqNative','eq');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -670),0,toGoid(0, -650),'READ',NULL,'SECURE_PASSWORD');

INSERT INTO rbac_role (goid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (toGoid(0, -700),0,'Manage Cluster Properties', null,null,null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete cluster properties.',0);
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -701),0,toGoid(0, -700),'READ',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -702),0,toGoid(0, -700),'CREATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -703),0,toGoid(0, -700),'UPDATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -704),0,toGoid(0, -700),'DELETE',NULL,'CLUSTER_PROPERTY');

INSERT INTO rbac_role (goid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (toGoid(0, -750),0,'Manage Listen Ports', null,null,null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete Gateway listen ports (HTTP(S) and FTP(S)) and to list published services.',0);
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -751),0,toGoid(0, -750),'READ',NULL,'SSG_CONNECTOR');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -752),0,toGoid(0, -750),'CREATE',NULL,'SSG_CONNECTOR');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -753),0,toGoid(0, -750),'UPDATE',NULL,'SSG_CONNECTOR');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -754),0,toGoid(0, -750),'DELETE',NULL,'SSG_CONNECTOR');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -755),0,toGoid(0, -750),'READ',NULL,'SERVICE');

INSERT INTO rbac_role (goid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (toGoid(0, -800),0,'Manage Log Sinks', null,null,null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete log sinks.',0);
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -801),0,toGoid(0, -800),'READ',NULL,'LOG_SINK');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -802),0,toGoid(0, -800),'CREATE',NULL,'LOG_SINK');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -803),0,toGoid(0, -800),'UPDATE',NULL,'LOG_SINK');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -804),0,toGoid(0, -800),'DELETE',NULL,'LOG_SINK');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -805),0,toGoid(0, -800),'READ',NULL,'CLUSTER_INFO');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -806),0,toGoid(0, -800),'OTHER','log-viewer','LOG_SINK');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -807),0,toGoid(0, -800),'READ',NULL,'SSG_CONNECTOR');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -808),0,toGoid(0, -800),'READ',NULL,'SERVICE');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -809),0,toGoid(0, -800),'READ',NULL,'FOLDER');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -810),0,toGoid(0, -800),'READ',NULL,'JMS_ENDPOINT');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -811),0,toGoid(0, -800),'READ',NULL,'USER');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -812),0,toGoid(0, -800),'READ',NULL,'ID_PROVIDER_CONFIG');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -813),0,toGoid(0, -800),'READ',NULL,'POLICY');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -814),0,toGoid(0, -800),'READ',NULL,'EMAIL_LISTENER');

INSERT INTO rbac_role (goid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (toGoid(0, -850),0,'Gateway Maintenance', null,null,null,null, 'Users assigned to the {0} role have the ability to perform Gateway maintenance tasks.',0);
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -851),0,toGoid(0, -850),'READ',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -852),0,toGoid(0, -851));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -852),'name','audit.archiver.ftp.config','eq');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -853),0,toGoid(0, -850),'UPDATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -854),0,toGoid(0, -853));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -854),'name','audit.archiver.ftp.config','eq');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -855),0,toGoid(0, -850),'CREATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -856),0,toGoid(0, -855));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -856),'name','audit.archiver.ftp.config','eq');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -857),0,toGoid(0, -850),'DELETE',NULL,'AUDIT_RECORD');
-- No predicates implies all entities

INSERT INTO rbac_role (goid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (toGoid(0, -900),0,'Manage Email Listeners', null,null,null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete email listeners.',0);
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -901),0,toGoid(0, -900),'READ',NULL,'EMAIL_LISTENER');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -902),0,toGoid(0, -900),'CREATE',NULL,'EMAIL_LISTENER');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -903),0,toGoid(0, -900),'UPDATE',NULL,'EMAIL_LISTENER');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -904),0,toGoid(0, -900),'DELETE',NULL,'EMAIL_LISTENER');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -905),0,toGoid(0, -900),'READ',NULL,'SERVICE');

INSERT INTO rbac_role (goid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (toGoid(0, -950),0,'Manage JDBC Connections', null,null,null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete JDBC connections.',0);
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -951),0,toGoid(0, -950),'READ',NULL,'JDBC_CONNECTION');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -952),0,toGoid(0, -950),'CREATE',NULL,'JDBC_CONNECTION');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -953),0,toGoid(0, -950),'UPDATE',NULL,'JDBC_CONNECTION');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -954),0,toGoid(0, -950),'DELETE',NULL,'JDBC_CONNECTION');

INSERT INTO rbac_role (goid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (toGoid(0, -1000),0,'Manage UDDI Registries', null,null,null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete UDDI Registry connections.',0);
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1001),0,toGoid(0, -1000),'READ',NULL,'UDDI_REGISTRY');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1002),0,toGoid(0, -1000),'CREATE',NULL,'UDDI_REGISTRY');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1003),0,toGoid(0, -1000),'UPDATE',NULL,'UDDI_REGISTRY');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1004),0,toGoid(0, -1000),'DELETE',NULL,'UDDI_REGISTRY');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1005),0,toGoid(0, -1000),'READ',NULL,'SERVICE');

INSERT INTO rbac_role (goid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (toGoid(0, -1050),0,'Manage Secure Passwords', null,null,null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete any stored password.',0);
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1051),0,toGoid(0, -1050),'READ',NULL,'SECURE_PASSWORD');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1052),0,toGoid(0, -1050),'CREATE',NULL,'SECURE_PASSWORD');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1053),0,toGoid(0, -1050),'UPDATE',NULL,'SECURE_PASSWORD');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1054),0,toGoid(0, -1050),'DELETE',NULL,'SECURE_PASSWORD');

INSERT INTO `rbac_role` (goid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (toGoid(0, -1100),1,'Manage Private Keys',NULL,NULL,NULL,NULL,'Users in this role have the ability to read, create, update, and delete private keys, as well as the ability to change the designated special-purpose keys (eg, the SSL or CA key).',0);
INSERT INTO `rbac_permission` (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES
    (toGoid(0, -1101),0,toGoid(0, -1100),'UPDATE',NULL,'CLUSTER_PROPERTY'),
    (toGoid(0, -1102),0,toGoid(0, -1100),'DELETE',NULL,'SSG_KEY_ENTRY'),
    (toGoid(0, -1103),0,toGoid(0, -1100),'READ',NULL,'CLUSTER_PROPERTY'),
    (toGoid(0, -1104),0,toGoid(0, -1100),'READ',NULL,'SSG_KEY_ENTRY'),
    (toGoid(0, -1105),0,toGoid(0, -1100),'READ',NULL,'CLUSTER_PROPERTY'),
    (toGoid(0, -1106),0,toGoid(0, -1100),'UPDATE',NULL,'SSG_KEY_ENTRY'),
    (toGoid(0, -1107),0,toGoid(0, -1100),'CREATE',NULL,'SSG_KEY_ENTRY'),
    (toGoid(0, -1108),0,toGoid(0, -1100),'UPDATE',NULL,'CLUSTER_PROPERTY'),
    (toGoid(0, -1109),0,toGoid(0, -1100),'DELETE',NULL,'CLUSTER_PROPERTY'),
    (toGoid(0, -1110),0,toGoid(0, -1100),'CREATE',NULL,'CLUSTER_PROPERTY'),
    (toGoid(0, -1111),0,toGoid(0, -1100),'CREATE',NULL,'CLUSTER_PROPERTY'),
    (toGoid(0, -1112),0,toGoid(0, -1100),'UPDATE',NULL,'SSG_KEYSTORE'),
    (toGoid(0, -1113),0,toGoid(0, -1100),'DELETE',NULL,'CLUSTER_PROPERTY'),
    (toGoid(0, -1114),0,toGoid(0, -1100),'READ',NULL,'SSG_KEYSTORE'),
    (toGoid(0, -1115),0,toGoid(0, -1100),'UPDATE',NULL,'CLUSTER_PROPERTY'),
    (toGoid(0, -1116),0,toGoid(0, -1100),'DELETE',NULL,'CLUSTER_PROPERTY'),
    (toGoid(0, -1117),0,toGoid(0, -1100),'CREATE',NULL,'CLUSTER_PROPERTY'),
    (toGoid(0, -1118),0,toGoid(0, -1100),'READ',NULL,'CLUSTER_PROPERTY'),
    (toGoid(0, -1119),0,toGoid(0, -1100),'UPDATE',NULL,'CLUSTER_PROPERTY'),
    (toGoid(0, -1120),0,toGoid(0, -1100),'DELETE',NULL,'CLUSTER_PROPERTY'),
    (toGoid(0, -1121),0,toGoid(0, -1100),'CREATE',NULL,'CLUSTER_PROPERTY'),
    (toGoid(0, -1122),0,toGoid(0, -1100),'READ',NULL,'CLUSTER_PROPERTY');
INSERT INTO `rbac_predicate` (goid, version, permission_goid) VALUES
    (toGoid(0, -1101),0,toGoid(0, -1101)),
    (toGoid(0, -1103),0,toGoid(0, -1103)),
    (toGoid(0, -1105),0,toGoid(0, -1105)),
    (toGoid(0, -1108),0,toGoid(0, -1108)),
    (toGoid(0, -1109),0,toGoid(0, -1109)),
    (toGoid(0, -1110),0,toGoid(0, -1110)),
    (toGoid(0, -1111),0,toGoid(0, -1111)),
    (toGoid(0, -1113),0,toGoid(0, -1113)),
    (toGoid(0, -1115),0,toGoid(0, -1115)),
    (toGoid(0, -1116),0,toGoid(0, -1116)),
    (toGoid(0, -1117),0,toGoid(0, -1117)),
    (toGoid(0, -1118),0,toGoid(0, -1118)),
    (toGoid(0, -1119),0,toGoid(0, -1119)),
    (toGoid(0, -1120),0,toGoid(0, -1120)),
    (toGoid(0, -1121),0,toGoid(0, -1121)),
    (toGoid(0, -1122),0,toGoid(0, -1122));
INSERT INTO `rbac_predicate_attribute` (goid, attribute, value, mode) VALUES
    (toGoid(0, -1101),'name','keyStore.defaultSsl.alias','eq'),
    (toGoid(0, -1103),'name','keyStore.defaultCa.alias','eq'),
    (toGoid(0, -1105),'name','keyStore.defaultSsl.alias','eq'),
    (toGoid(0, -1108),'name','keyStore.defaultCa.alias','eq'),
    (toGoid(0, -1109),'name','keyStore.defaultSsl.alias','eq'),
    (toGoid(0, -1110),'name','keyStore.defaultCa.alias','eq'),
    (toGoid(0, -1111),'name','keyStore.defaultSsl.alias','eq'),
    (toGoid(0, -1113),'name','keyStore.defaultCa.alias','eq'),
    (toGoid(0, -1115),'name','keyStore.auditViewer.alias','eq'),
    (toGoid(0, -1116),'name','keyStore.auditViewer.alias','eq'),
    (toGoid(0, -1117),'name','keyStore.auditViewer.alias','eq'),
    (toGoid(0, -1118),'name','keyStore.auditViewer.alias','eq'),
    (toGoid(0, -1119),'name','keyStore.auditSigning.alias','eq'),
    (toGoid(0, -1120),'name','keyStore.auditSigning.alias','eq'),
    (toGoid(0, -1121),'name','keyStore.auditSigning.alias','eq'),
    (toGoid(0, -1122),'name','keyStore.auditSigning.alias','eq');

INSERT INTO rbac_role (goid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (toGoid(0, -1150),0,'Manage Password Policies', null,null,null,null, 'Users assigned to the {0} role have the ability to read and update any stored password policy and view the identity providers.',0);
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1151),0,toGoid(0, -1150),'READ',NULL,'PASSWORD_POLICY');
-- INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1052),0,toGoid(0, -1050),'CREATE',NULL,'PASSWORD_POLICY');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1153),0,toGoid(0, -1150),'UPDATE',NULL,'PASSWORD_POLICY');
-- INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1054),0,toGoid(0, -1050),'DELETE',NULL,'PASSWORD_POLICY');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1155),0,toGoid(0, -1150),'READ',NULL,'ID_PROVIDER_CONFIG');

--
-- New role to invoke the audit viewer policy. Requires READ on audits to be able to open the audit viewer.
--
INSERT INTO rbac_role (goid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (toGoid(0, -1200),0,'Invoke Audit Viewer Policy', null,null,null,null, 'Allow the INTERNAL audit-viewer policy to be invoked for an audited message (request / response or detail)',0);
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1201),0,toGoid(0, -1200),'OTHER','audit-viewer policy', 'AUDIT_RECORD');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1202),0,toGoid(0, -1200),'READ',NULL,'AUDIT_RECORD');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1203),0,toGoid(0, -1200),'READ',NULL,'CLUSTER_INFO');

INSERT INTO rbac_role (goid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (toGoid(0, -1250),0,'Manage Administrative Accounts Configuration', null,null,null,null, 'Users assigned to the {0} role have the ability to create/read/update cluster properties applicable to administrative accounts configurations.',0);
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1251),0,toGoid(0, -1250),'READ',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -1252),0,toGoid(0, -1251));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -1252),'name','logon.maxAllowableAttempts','eq');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1253),0,toGoid(0, -1250),'READ',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -1254),0,toGoid(0, -1253));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -1254),'name','logon.lockoutTime','eq');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1255),0,toGoid(0, -1250),'READ',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -1256),0,toGoid(0, -1255));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -1256),'name','logon.sessionExpiry','eq');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1257),0,toGoid(0, -1250),'READ',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -1258),0,toGoid(0, -1257));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -1258),'name','logon.inactivityPeriod','eq');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1259),0,toGoid(0, -1250),'UPDATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -1260),0,toGoid(0, -1259));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -1260),'name','logon.maxAllowableAttempts','eq');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1261),0,toGoid(0, -1250),'UPDATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -1262),0,toGoid(0, -1261));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -1262),'name','logon.lockoutTime','eq');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1263),0,toGoid(0, -1250),'UPDATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -1264),0,toGoid(0, -1263));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -1264),'name','logon.sessionExpiry','eq');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1265),0,toGoid(0, -1250),'UPDATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -1266),0,toGoid(0, -1265));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -1266),'name','logon.inactivityPeriod','eq');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1267),0,toGoid(0, -1250),'CREATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -1268),0,toGoid(0, -1267));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -1268),'name','logon.maxAllowableAttempts','eq');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1269),0,toGoid(0, -1250),'CREATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -1270),0,toGoid(0, -1269));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -1270),'name','logon.lockoutTime','eq');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1271),0,toGoid(0, -1250),'CREATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -1272),0,toGoid(0, -1271));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -1272),'name','logon.sessionExpiry','eq');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1273),0,toGoid(0, -1250),'CREATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -1274),0,toGoid(0, -1273));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -1274),'name','logon.inactivityPeriod','eq');

--
-- New role for viewing the default log (oid = -810)
--
-- NOTE: This is an entity specific role and will be deleted if the default log
-- sink is removed.
--
INSERT INTO rbac_role (goid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (toGoid(0, -1300),0,'View ssg Log Sink (#-1,300)',null,'LOG_SINK',-810,null, 'Users assigned to the {0} role have the ability to read the log sink and any associated log files.',0);
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1301),0,toGoid(0, -1300),'READ',NULL,'LOG_SINK');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -1301),0,toGoid(0, -1301));
INSERT INTO rbac_predicate_oid (goid, entity_id) VALUES (toGoid(0, -1301),'-810');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1302),0,toGoid(0, -1300),'READ',NULL,'CLUSTER_INFO');

INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1303),0,toGoid(0, -1300),'OTHER','log-viewer','LOG_SINK');

INSERT INTO rbac_role (goid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (toGoid(0, -1350),0,'Manage Encapsulated Assertions', null,'ENCAPSULATED_ASSERTION',null,null, 'Users assigned to the {0} role have the ability to create/read/update/delete encapsulated assertions.',0);
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1351),0,toGoid(0, -1350),'CREATE',null,'ENCAPSULATED_ASSERTION');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1352),0,toGoid(0, -1350),'READ',NULL,'ENCAPSULATED_ASSERTION');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1353),0,toGoid(0, -1350),'UPDATE',null, 'ENCAPSULATED_ASSERTION');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1354),0,toGoid(0, -1350),'DELETE',NULL,'ENCAPSULATED_ASSERTION');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1355),0,toGoid(0, -1350),'READ',NULL,'POLICY');
INSERT INTO rbac_predicate (goid, version, permission_goid) VALUES (toGoid(0, -1356),0,toGoid(0, -1355));
INSERT INTO rbac_predicate_attribute (goid, attribute, value, mode) VALUES (toGoid(0, -1356),'type','Included Policy Fragment','eq');

INSERT INTO rbac_role (goid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (toGoid(0, -1450),0,'Manage Custom Key Value Store', null,'CUSTOM_KEY_VALUE_STORE',null,null, 'Users assigned to the {0} role have the ability to read, create, update, and delete key values from custom key value store.',0);
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1451),0,toGoid(0, -1450),'CREATE',null,'CUSTOM_KEY_VALUE_STORE');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1452),0,toGoid(0, -1450),'READ',null,'CUSTOM_KEY_VALUE_STORE');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1453),0,toGoid(0, -1450),'UPDATE',null,'CUSTOM_KEY_VALUE_STORE');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1454),0,toGoid(0, -1450),'DELETE',null,'CUSTOM_KEY_VALUE_STORE');

-- Assign Administrator role to existing admin user
INSERT INTO rbac_assignment (goid, provider_goid, role_goid, identity_id, entity_type) VALUES (toGoid(0, -105), toGoid(0,-2), toGoid(0, -100), '00000000000000000000000000000003', 'User');

DROP TABLE IF EXISTS sink_config;
CREATE TABLE sink_config (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  name varchar(32) NOT NULL,
  description mediumtext,
  type varchar(32) NOT NULL DEFAULT 'FILE',
  enabled tinyint(1) NOT NULL DEFAULT 0,
  severity varchar(32) NOT NULL DEFAULT 'INFO',
  categories mediumtext,
  properties mediumtext,
  security_zone_goid binary(16),
  PRIMARY KEY  (objectid),
  CONSTRAINT sink_config_security_zone FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
--  NOTE: if changing this configuration, also update the defaults in JdkLogConfig 
--
INSERT INTO sink_config (objectid, version, name, description, type, enabled, severity, categories, properties, security_zone_goid) VALUES (-810,0,'ssg','Main log','FILE',1,'INFO','AUDIT,LOG','<java version="1.6.0" class="java.beans.XMLDecoder"><object class="java.util.HashMap"><void method="put"><string>file.maxSize</string><string>20000</string></void><void method="put"><string>file.format</string><string>STANDARD</string></void><void method="put"><string>file.logCount</string><string>10</string></void></object></java>',NULL);
INSERT INTO sink_config (objectid, version, name, description, type, enabled, severity, categories, properties, security_zone_goid) VALUES (-811,0,'sspc','Process Controller Log','FILE',0,'FINEST','SSPC','<java version="1.6.0" class="java.beans.XMLDecoder"><object class="java.util.HashMap"><void method="put"><string>file.maxSize</string><string>20000</string></void><void method="put"><string>file.format</string><string>STANDARD</string></void><void method="put"><string>file.logCount</string><string>10</string></void></object></java>',NULL);

DROP TABLE IF EXISTS wsdm_subscription;
CREATE TABLE wsdm_subscription (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  uuid varchar(36) NOT NULL,
  callback_url varchar(255) NOT NULL,
  reference_parameters mediumtext,
  published_service_goid BINARY(16) NOT NULL,
  esm_service_goid BINARY(16) NOT NULL DEFAULT X'0000000000000000FFFFFFFFFFFFFFFF',
  termination_time bigint(20) NOT NULL,
  topic int(11) NOT NULL,
  notification_policy_guid CHAR(36),
  last_notification bigint(20),
  owner_node_id varchar(36),
  PRIMARY KEY  (objectid),
  UNIQUE KEY uuid (uuid),
  FOREIGN KEY (notification_policy_guid) REFERENCES policy (guid)  
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS email_listener;
CREATE TABLE email_listener (
  goid BINARY(16) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  host varchar(128) NOT NULL,
  port int(8) NOT NULL,
  server_type varchar(4) NOT NULL,
  use_ssl tinyint(1) NOT NULL,
  delete_on_receive tinyint(1) NOT NULL,
  username varchar(255) NOT NULL,
  password varchar(32) NOT NULL,
  folder varchar(255) NOT NULL,
  poll_interval int(8) NOT NULL,
  active tinyint(1) NOT NULL default 1,
  properties mediumtext,
  security_zone_goid binary(16),
  PRIMARY KEY  (goid),
  CONSTRAINT email_listener_security_zone FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS email_listener_state;
CREATE TABLE email_listener_state (
  goid BINARY(16) NOT NULL,
  version integer NOT NULL,
  owner_node_id varchar(36),
  last_poll_time bigint(20),
  last_message_id bigint(20),
  email_listener_goid BINARY(16) NOT NULL,
  PRIMARY KEY  (goid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS active_connector;
CREATE TABLE active_connector (
  goid binary(16) NOT NULL,
  version integer NOT NULL,
  enabled tinyint(1) NOT NULL,
  name varchar(128) NOT NULL,
  type varchar(128) NOT NULL,
  hardwired_service_goid binary(16),
  security_zone_goid binary(16),
  old_objectid bigint(20),
  PRIMARY KEY (goid),
  CONSTRAINT active_conn_security_zone FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS active_connector_property;
CREATE TABLE active_connector_property (
  connector_goid binary(16) NOT NULL,
  name varchar(128) NOT NULL,
  value MEDIUMTEXT NOT NULL,
  FOREIGN KEY (connector_goid) REFERENCES active_connector (goid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table for stored secure conversation sessions
--
DROP TABLE IF EXISTS wssc_session;
CREATE TABLE wssc_session (
  objectid bigint(20) NOT NULL,
  session_key_hash varchar(128),
  inbound tinyint(1) NOT NULL DEFAULT 0,
  identifier varchar(4096) NOT NULL,
  service_url varchar(4096),
  encrypted_key varchar(4096),
  created bigint(20) NOT NULL,
  expires bigint(20) NOT NULL,
  provider_goid binary(16) NOT NULL,
  user_id varchar(255) NOT NULL,
  user_login varchar(255) NOT NULL,
  namespace varchar(4096),
  token mediumtext,
  UNIQUE KEY (session_key_hash),
  PRIMARY KEY (objectid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table for generic (runtime) entity types
--
DROP TABLE IF EXISTS generic_entity;
CREATE TABLE generic_entity (
  goid binary(16) NOT NULL,
  version int(11) NOT NULL,
  name varchar(255),
  description mediumtext,
  classname varchar(255) NOT NULL,
  enabled boolean DEFAULT TRUE,
  value_xml mediumtext,
  PRIMARY KEY (goid),
  UNIQUE KEY i_classname_name (classname, name)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;


DROP TABLE IF EXISTS encapsulated_assertion;
CREATE TABLE encapsulated_assertion (
  goid binary(16) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  guid varchar(255) NOT NULL,
  policy_goid binary(16) NOT NULL,
  security_zone_goid binary(16),
  FOREIGN KEY (policy_goid) REFERENCES policy (goid),
  PRIMARY KEY (goid),
  UNIQUE KEY i_guid (guid),
  CONSTRAINT encass_security_zone FOREIGN KEY (security_zone_goid) REFERENCES security_zone (goid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS encapsulated_assertion_property;
CREATE TABLE encapsulated_assertion_property (
  encapsulated_assertion_goid binary(16) NOT NULL,
  name varchar(128) NOT NULL,
  value MEDIUMTEXT NOT NULL,
  FOREIGN KEY (encapsulated_assertion_goid) REFERENCES encapsulated_assertion (goid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS encapsulated_assertion_argument;
CREATE TABLE encapsulated_assertion_argument (
  goid binary(16) NOT NULL,
  version integer NOT NULL,
  encapsulated_assertion_goid binary(16) NOT NULL,
  argument_name varchar(128) NOT NULL,
  argument_type varchar(128) NOT NULL,
  gui_prompt tinyint(1) NOT NULL,
  gui_label varchar(255),
  ordinal int(20) NOT NULL,
  FOREIGN KEY (encapsulated_assertion_goid) REFERENCES encapsulated_assertion (goid) ON DELETE CASCADE,
  PRIMARY KEY (goid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS encapsulated_assertion_result;
CREATE TABLE encapsulated_assertion_result (
  goid binary(16) NOT NULL,
  version integer NOT NULL,
  encapsulated_assertion_goid binary(16) NOT NULL,
  result_name varchar(128) NOT NULL,
  result_type varchar(128) NOT NULL,
  FOREIGN KEY (encapsulated_assertion_goid) REFERENCES encapsulated_assertion (goid) ON DELETE CASCADE,
  PRIMARY KEY (goid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

-- Firewall Rules table structure --
DROP TABLE IF EXISTS firewall_rule;
CREATE TABLE firewall_rule (
  goid binary(16) NOT NULL,
  version integer NOT NULL,
  ordinal integer NOT NULL,
  name varchar(128) NOT NULL,
  enabled tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (goid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS firewall_rule_property;
CREATE TABLE firewall_rule_property (
  firewall_rule_goid binary(16) NOT NULL,
  name varchar(128) NOT NULL,
  value MEDIUMTEXT NOT NULL,
  FOREIGN KEY (firewall_rule_goid) REFERENCES firewall_rule (goid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Custom key value store
--
DROP TABLE IF EXISTS custom_key_value_store;
CREATE TABLE custom_key_value_store (
  goid binary(16) NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  value mediumblob NOT NULL,
  PRIMARY KEY (goid),
  UNIQUE KEY (name)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS license_document;
CREATE TABLE license_document (
  goid binary(16) NOT NULL,
  version integer NOT NULL,
  contents mediumtext,
  PRIMARY KEY (goid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS ssg_version;
CREATE TABLE ssg_version (
   current_version char(10) NOT NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

-- create new RBAC role for Manage Firewall Rules --
INSERT INTO rbac_role (goid, version, name, entity_type, description, user_created) VALUES (toGoid(0, -1400), 0, 'Manage Firewall Rules', 'FIREWALL_RULE', 'Users assigned to the {0} role have the ability to read, create, update and delete Firewall rules.', 0);
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1275),0,toGoid(0, -1400),'CREATE',NULL,'FIREWALL_RULE');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1276),0,toGoid(0, -1400),'READ',NULL,'FIREWALL_RULE');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1277),0,toGoid(0, -1400),'UPDATE',NULL,'FIREWALL_RULE');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1278),0,toGoid(0, -1400),'DELETE',NULL,'FIREWALL_RULE');

-- create new RBAC role for SiteMinder Configuration --
INSERT INTO rbac_role (goid, version, name, tag, entity_type, entity_oid, entity_goid, description, user_created) VALUES (toGoid(0, -1500),0,'Manage SiteMinder Configuration', null, 'SITEMINDER_CONFIGURATION', null, null, 'Users assigned to the {0} role have the ability to read, create, update and delete SiteMinder configuration.',0);
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1501),0,toGoid(0, -1500),'READ',NULL,'SITEMINDER_CONFIGURATION');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1502),0,toGoid(0, -1500),'CREATE',NULL,'SITEMINDER_CONFIGURATION');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1503),0,toGoid(0, -1500),'UPDATE',NULL,'SITEMINDER_CONFIGURATION');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1504),0,toGoid(0, -1500),'DELETE',NULL,'SITEMINDER_CONFIGURATION');
INSERT INTO rbac_permission (goid, version, role_goid, operation_type, other_operation, entity_type) VALUES (toGoid(0, -1505),0,toGoid(0, -1500),'READ',NULL,'SECURE_PASSWORD');


INSERT INTO ssg_version (current_version) VALUES ('8.0.0');

SET FOREIGN_KEY_CHECKS = 1;

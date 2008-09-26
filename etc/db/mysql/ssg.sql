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
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Dumping data for table 'hibernate_unique_key'
--


INSERT INTO hibernate_unique_key VALUES (1);

--
-- Table structure for table 'identity_provider'
--

DROP TABLE IF EXISTS identity_provider;
CREATE TABLE identity_provider (
  objectid bigint(20) NOT NULL,
  version int(11) default NULL,
  name varchar(128) NOT NULL,
  description mediumtext,
  type bigint(20) NOT NULL,
  properties mediumtext,
  PRIMARY KEY  (objectid),
  UNIQUE KEY ipnm_idx (name)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Dumping data for table 'identity_provider'
--

INSERT INTO identity_provider (objectid,name,description,type,properties,version) VALUES (-2,'Internal Identity Provider','Internal Identity Provider',1,'<java version="1.6.0_01" class="java.beans.XMLDecoder"><object class="java.util.HashMap"><void method="put"><string>adminEnabled</string><boolean>true</boolean></void></object></java>',0);

--
-- Table structure for table 'internal_group'
--

DROP TABLE IF EXISTS internal_group;
CREATE TABLE internal_group (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  name varchar(128) NOT NULL,
  description mediumtext,
  PRIMARY KEY  (objectid),
  UNIQUE KEY g_idx (name)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table 'internal_user'
--

DROP TABLE IF EXISTS internal_user;
CREATE TABLE internal_user (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  name varchar(128) default NULL,
  login varchar(255) NOT NULL,
  password varchar(32) NOT NULL,
  first_name varchar(32) default NULL,
  last_name varchar(32) default NULL,
  email varchar(128) default NULL,
  description varchar(255) default NULL,
  expiration bigint(20) NOT NULL,
  PRIMARY KEY  (objectid),
  UNIQUE KEY l_idx (login)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Dumping data for table 'internal_user'
--


INSERT INTO internal_user VALUES (3,0,'admin','admin','a41306e4b1b5858d3e3d705dd2e738e2','','','',-1);

--
-- Table structure for table 'internal_user_group'
--

DROP TABLE IF EXISTS internal_user_group;
CREATE TABLE internal_user_group (
  objectid bigint(20) not null,
  version int(11) not null,
  internal_group bigint(20) NOT NULL,
  provider_oid bigint(20) NOT NULL,
  user_id varchar(255),
  subgroup_id varchar(255),
  PRIMARY KEY (objectid),
  INDEX (internal_group),
  INDEX (provider_oid),
  INDEX (user_id),
  INDEX (subgroup_id)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;


--
-- Table structure for table 'folder'
--

DROP TABLE IF EXISTS folder;
CREATE TABLE folder (
  objectid bigint(20) NOT NULL,
  name varchar(128) NOT NULL,
  parent_folder_oid bigint(20),
  PRIMARY KEY  (objectid),
  FOREIGN KEY (parent_folder_oid) REFERENCES folder (objectid) ON DELETE SET NULL,
  UNIQUE KEY `i_name_parent` (`name`,`parent_folder_oid`)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;


INSERT INTO folder VALUES (-5002, 'Root Node', NULL);

--
-- Table structure for table 'published_service'
--

DROP TABLE IF EXISTS published_service;
CREATE TABLE published_service (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  name varchar(255) NOT NULL,
  policy_xml mediumtext,
  policy_oid bigint(20) default NULL,
  wsdl_url varchar(255),
  wsdl_xml mediumtext,
  disabled TINYINT(1) NOT NULL DEFAULT 0,
  soap TINYINT(1) NOT NULL DEFAULT 1,
  internal TINYINT(1) NOT NULL DEFAULT 0,
  routing_uri varchar(128),
  http_methods mediumtext,
  lax_resolution TINYINT(1) NOT NULL DEFAULT 0, 
  folder_oid bigint(20),
  PRIMARY KEY (objectid),
  FOREIGN KEY (policy_oid) REFERENCES policy (objectid),
  FOREIGN KEY (folder_oid) REFERENCES folder (objectid) ON DELETE SET NULL
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table 'published_service_alias'
--
DROP TABLE IF EXISTS published_service_alias;
CREATE TABLE published_service_alias (
  `objectid` bigint(20) NOT NULL,
  `version` int(11) NOT NULL,
  `folder_oid` bigint(20) NOT NULL,
  `published_service_oid` bigint(20) NOT NULL,
  UNIQUE KEY (folder_oid, published_service_oid),
  FOREIGN KEY (published_service_oid) REFERENCES published_service (objectid) ON DELETE CASCADE,
  FOREIGN KEY (folder_oid) REFERENCES folder (objectid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table 'policy'
--

DROP TABLE IF EXISTS policy;
CREATE TABLE policy (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  name varchar(255) NOT NULL,
  xml mediumtext NOT NULL,
  policy_type VARCHAR(32) NOT NULL,
  soap TINYINT(1) NOT NULL DEFAULT 0,
  guid char(36) NOT NULL,
  internal_tag VARCHAR(64),
  folder_oid bigint(20),
  PRIMARY KEY (objectid),
  UNIQUE KEY i_name (name),
  UNIQUE KEY i_guid (guid),
  FOREIGN KEY (folder_oid) REFERENCES folder (objectid) ON DELETE SET NULL,
  INDEX (policy_type)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

---
--- Policy XML rollback support
---

DROP TABLE IF EXISTS policy_version;
CREATE TABLE policy_version (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  name varchar(255),
  policy_oid bigint(20) NOT NULL,
  ordinal int(20) NOT NULL,
  time bigint(20) NOT NULL,
  user_provider_oid bigint(20),
  user_login varchar(255),
  active boolean,
  xml mediumtext,
  PRIMARY KEY (objectid),
  INDEX (policy_oid),
  FOREIGN KEY (policy_oid) REFERENCES policy (objectid) ON DELETE CASCADE
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;


--
-- Table structure for table 'policy_alias'
--
DROP TABLE IF EXISTS policy_alias;
CREATE TABLE policy_alias (
  `objectid` bigint(20) NOT NULL,
  `version` int(11) NOT NULL,
  `folder_oid` bigint(20) NOT NULL,
  `policy_oid` bigint(20) NOT NULL,
  UNIQUE KEY (folder_oid, policy_oid),
  FOREIGN KEY (policy_oid) REFERENCES policy (objectid) ON DELETE CASCADE,
  FOREIGN KEY (folder_oid) REFERENCES folder (objectid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


--
-- Dumping data for table 'published_service'
--

--
-- Table structure for table 'client_cert'
--

DROP TABLE IF EXISTS client_cert;
CREATE TABLE client_cert (
  objectid bigint NOT NULL,
  provider bigint NOT NULL,
  user_id varchar(255),
  login varchar(255),
  cert mediumtext DEFAULT NULL,
  reset_counter int NOT NULL,
  thumbprint_sha1 varchar(64),
  ski varchar(64),
  PRIMARY KEY  (objectid),
  FOREIGN KEY (provider) REFERENCES identity_provider (objectid) ON DELETE CASCADE,
  unique key i_identity (provider, user_id),
  INDEX i_thumb (thumbprint_sha1),
  INDEX i_ski (ski)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Dumping data for table 'client_cert'
--

--
-- Table structure for table 'service_resolution'
--

DROP TABLE IF EXISTS service_resolution;
CREATE TABLE service_resolution (
  objectid bigint(20) NOT NULL,
  serviceid bigint NOT NULL,
  digested varchar(32) NOT NULL,
  soapaction mediumtext character set latin1 BINARY,
  urn mediumtext character set latin1 BINARY,
  uri mediumtext character set latin1 BINARY,
  unique(digested),
  PRIMARY KEY (objectid)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Dumping data for table 'service_resolution'
--

--
-- Table structure for table 'cluster_info'
--

DROP TABLE IF EXISTS cluster_info;

CREATE TABLE cluster_info (
  nodeid varchar(32) NOT NULL,
  mac varchar(18) NOT NULL,
  name varchar(128) NOT NULL,
  address varchar(16) NOT NULL,
  multicast_address varchar(16),
  uptime bigint NOT NULL,
  avgload double NOT NULL,
  statustimestamp bigint NOT NULL,
  PRIMARY KEY(nodeid)
)  TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Dumping data for table 'cluster_info'
--

--
-- Table structure for table 'service_usage'
--

DROP TABLE IF EXISTS service_usage;
CREATE TABLE service_usage (
  serviceid bigint NOT NULL,
  nodeid varchar(32) NOT NULL,
  requestnr bigint NOT NULL,
  authorizedreqnr bigint NOT NULL,
  completedreqnr bigint NOT NULL,
  primary key(serviceid, nodeid)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Dumping data for table 'service_usage'
--

--
-- Table structure for table 'jms_connection'
--

DROP TABLE IF EXISTS jms_connection;
CREATE TABLE jms_connection (
  objectid bigint NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  jndi_url varchar(255) NOT NULL,
  factory_classname varchar(255) NOT NULL,
  destination_factory_url varchar(255) default '',
  queue_factory_url varchar(255) default '',
  topic_factory_url varchar(255) default '',
  username varchar(32) default '',
  password varchar(32) default '',
  properties mediumtext,
  primary key(objectid)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table 'jms_endpoint'
--

DROP TABLE IF EXISTS jms_endpoint;
CREATE TABLE jms_endpoint(
  objectid bigint NOT NULL,
  version integer NOT NULL,
  connection_oid bigint NOT NULL,
  name varchar(128) NOT NULL,
  destination_name varchar(128) NOT NULL,
  failure_destination_name varchar(128),
  acknowledgement_type varchar(128),    
  reply_type integer default '0',
  reply_to_queue_name varchar(128),
  disabled tinyint(1) NOT NULL default 0,
  username varchar(32) default '',
  password varchar(32) default '',
  max_concurrent_requests integer default '1',
  is_message_source tinyint default '0',
  outbound_message_type varchar(128),
  use_message_id_for_correlation tinyint(1) NOT NULL DEFAULT 0,
  primary key(objectid)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS revocation_check_policy;
CREATE TABLE revocation_check_policy (
  objectid bigint(20) NOT NULL,
  version int(11) default NULL,
  name varchar(128) NOT NULL,
  revocation_policy_xml mediumtext,
  default_policy tinyint default '0',
  default_success tinyint default '0',
  continue_server_unavailable tinyint default '0',
  PRIMARY KEY  (objectid),
  UNIQUE KEY rcp_name_idx (name)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table 'trusted_cert'
--

DROP TABLE IF EXISTS trusted_cert;
CREATE TABLE trusted_cert (
  objectid bigint NOT NULL,
  version integer NOT NULL,
  name varchar(128) NOT NULL,
  subject_dn varchar(255) NOT NULL,
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
  revocation_policy_oid bigint(20),
  primary key(objectid),
  unique i_thumb (thumbprint_sha1),
  INDEX i_ski (ski),
  INDEX i_subject_dn (subject_dn),
  FOREIGN KEY (revocation_policy_oid) REFERENCES revocation_check_policy (objectid)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS fed_user;
CREATE TABLE fed_user (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  name varchar(128) NOT NULL,
  provider_oid bigint(20) NOT NULL,
  subject_dn varchar(255),
  email varchar(128) default NULL,
  login varchar(255),
  first_name varchar(32) default NULL,
  last_name varchar(32) default NULL,
  PRIMARY KEY (objectid),
  INDEX i_provider_oid (provider_oid),
  INDEX i_email (email),
  INDEX i_login (login),
  INDEX i_subject_dn (subject_dn),
  UNIQUE KEY i_name (provider_oid, name)
) Type=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS fed_group;
CREATE TABLE fed_group (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  provider_oid bigint(20) NOT NULL,
  name varchar(128) NOT NULL,
  description mediumtext,
  PRIMARY KEY  (objectid),
  INDEX i_provider_oid (provider_oid),
  UNIQUE KEY i_name (provider_oid, name)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS fed_user_group;
CREATE TABLE fed_user_group (
  provider_oid bigint(20) NOT NULL,
  fed_user_oid bigint(20) NOT NULL,
  fed_group_oid bigint(20) NOT NULL,
  PRIMARY KEY (provider_oid,fed_user_oid,fed_group_oid)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS fed_group_virtual;
CREATE TABLE fed_group_virtual (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  provider_oid bigint(20) NOT NULL,
  name varchar(128) NOT NULL,
  description mediumtext,
  x509_subject_dn_pattern varchar(255),
  saml_email_pattern varchar(128),
  properties mediumtext,
  PRIMARY KEY  (objectid),
  INDEX i_provider_oid (provider_oid),
  INDEX i_x509_subject_dn_pattern (x509_subject_dn_pattern),
  INDEX i_saml_email_pattern (saml_email_pattern),
  UNIQUE KEY i_name (provider_oid, name)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for message_context_mapping_keys
--
DROP TABLE IF EXISTS message_context_mapping_keys;
CREATE TABLE message_context_mapping_keys (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  guid char(36) NOT NULL,
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
  UNIQUE KEY (guid),
  INDEX (guid)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for message_context_mapping_values
--
DROP TABLE IF EXISTS message_context_mapping_values;
CREATE TABLE message_context_mapping_values (
  objectid bigint(20) NOT NULL,
  guid char(36) NOT NULL,
  mapping_keys_oid bigint(20) NOT NULL,
  mapping1_value varchar(255),
  mapping2_value varchar(255),
  mapping3_value varchar(255),
  mapping4_value varchar(255),
  mapping5_value varchar(255),
  create_time bigint(20),
  PRIMARY KEY  (objectid),
  FOREIGN KEY (mapping_keys_oid) REFERENCES message_context_mapping_keys (objectid),
  UNIQUE KEY (guid),
  INDEX (guid)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

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
  ip_address varchar(32),
  user_name varchar(255),
  user_id varchar(255),
  provider_oid bigint(20) NOT NULL DEFAULT -1,
  signature varchar(175),
  PRIMARY KEY  (objectid),
  KEY idx_time (time),
  KEY idx_ip_address (ip_address),
  KEY idx_prov_user (provider_oid, user_id)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

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
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table `audit_message`
--

DROP TABLE IF EXISTS audit_message;
CREATE TABLE audit_message (
  objectid bigint(20) NOT NULL,
  status varchar(32) NOT NULL,
  request_id varchar(40) NOT NULL,
  service_oid bigint(20),
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
  KEY idx_service_oid (service_oid),
  FOREIGN KEY (objectid) REFERENCES audit_main (objectid) ON DELETE CASCADE,
  FOREIGN KEY (mapping_values_oid) REFERENCES message_context_mapping_values (objectid)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS audit_system;
CREATE TABLE audit_system (
  objectid bigint(20) NOT NULL,
  component_id integer NOT NULL,
  action varchar(32) NOT NULL,
  PRIMARY KEY (objectid),
  KEY idx_component_id (component_id),
  KEY idx_action (action),
  FOREIGN KEY (objectid) REFERENCES audit_main (objectid) ON DELETE CASCADE
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

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
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS audit_detail_params;
CREATE TABLE audit_detail_params (
  audit_detail_oid bigint(20) NOT NULL,
  position integer NOT NULL,
  value MEDIUMTEXT NOT NULL,
  PRIMARY KEY (audit_detail_oid, position),
  FOREIGN KEY (audit_detail_oid) REFERENCES audit_detail (objectid) ON DELETE CASCADE
) Type=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS message_id;
CREATE TABLE message_id (
  messageid varchar(255) NOT NULL PRIMARY KEY,
  expires bigint(20) NOT NULL
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS counters;
CREATE TABLE counters (
  counterid bigint(20) NOT NULL,
  userid varchar(128) NOT NULL,
  providerid bigint(20) NOT NULL,
  countername varchar(128) NOT NULL,
  cnt_sec bigint(20) default 0,
  cnt_min bigint(20) default 0,
  cnt_hr bigint(20) default 0,
  cnt_day bigint(20) default 0,
  cnt_mnt bigint(20) default 0,
  last_update bigint(20) default 0,
  unique(userid, providerid, countername),
  PRIMARY KEY (counterid)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table 'community_schemas'
--

DROP TABLE IF EXISTS community_schemas;
CREATE TABLE community_schemas (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  name varchar(128) default '',
  tns varchar(128) default '',
  schema_xml mediumtext,
  system tinyint(1) NOT NULL default 0,
  PRIMARY KEY (objectid),
  UNIQUE KEY csnm_idx (name)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table 'cluster_properties'
-- note that 'key' is unfortunately not a valid column name
--

DROP TABLE IF EXISTS cluster_properties;
CREATE TABLE cluster_properties (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  propkey varchar(128) NOT NULL,
  propvalue MEDIUMTEXT NOT NULL,
  PRIMARY KEY (objectid),
  UNIQUE(propkey)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS sample_messages;
CREATE TABLE sample_messages (
  objectid bigint(20) NOT NULL,
  published_service_oid bigint(20),
  name varchar(128) NOT NULL,
  xml mediumtext NOT NULL,
  operation_name varchar(128),
  INDEX i_ps_oid (published_service_oid),
  INDEX i_operation_name (operation_name),
  FOREIGN KEY (published_service_oid) REFERENCES published_service (objectid) ON DELETE CASCADE,
  PRIMARY KEY (objectid)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS service_metrics;
CREATE TABLE service_metrics (
  nodeid VARCHAR(32) NOT NULL,
  published_service_oid BIGINT(20) NOT NULL,
  resolution INTEGER NOT NULL,
  period_start BIGINT(20) NOT NULL,
  start_time BIGINT(20) NOT NULL,
  interval_size INTEGER NOT NULL,
  end_time BIGINT(20) NOT NULL,
  attempted INTEGER NOT NULL,
  authorized INTEGER NOT NULL,
  completed INTEGER NOT NULL,
  back_min INTEGER NOT NULL,
  back_max INTEGER NOT NULL,
  back_sum INTEGER NOT NULL,
  front_min INTEGER NOT NULL,
  front_max INTEGER NOT NULL,
  front_sum INTEGER NOT NULL,
  service_state VARCHAR(16),
  mapping_values_oid BIGINT(20),
  INDEX i_sm_nodeid (nodeid),
  INDEX i_sm_serviceoid (published_service_oid),
  INDEX i_sm_resolution (resolution),
  INDEX i_sm_pstart (period_start),
  PRIMARY KEY (nodeid, published_service_oid, resolution, period_start),
  FOREIGN KEY (mapping_values_oid) REFERENCES message_context_mapping_values (objectid)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table 'service_documents'
--

DROP TABLE IF EXISTS service_documents;
CREATE TABLE service_documents (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  service_oid bigint(20) NOT NULL,
  uri MEDIUMTEXT,
  type VARCHAR(32) NOT NULL,
  content_type VARCHAR(32) NOT NULL,
  content MEDIUMTEXT,
  INDEX i_sd_service_type (service_oid, type),
  PRIMARY KEY (objectid),
  FOREIGN KEY (service_oid) REFERENCES published_service (objectid) ON DELETE CASCADE
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;


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
  PRIMARY KEY (objectid),
  UNIQUE(name)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

-- placeholder, never loaded or saved
insert into keystore_file values (0, 0, "Software Static", "ss", null);

-- tar.gz of items in sca 6000 keydata directory
insert into keystore_file values (1, 0, "HSM", "hsm.sca.targz", null);

-- bytes of a PKCS#12 keystore
insert into keystore_file values (2, 0, "Software DB", "sdb.pkcs12", null);

DROP TABLE IF EXISTS shared_keys;
CREATE TABLE shared_keys (
  encodingid varchar(32) NOT NULL,
  b64edval varchar(256) NOT NULL,
  primary key(encodingid)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

-- HTTP and HTTPS listeners and properties
DROP TABLE IF EXISTS connector_property;
DROP TABLE IF EXISTS connector;

CREATE TABLE connector (
  objectid bigint(20) NOT NULL,
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
  PRIMARY KEY (objectid)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

CREATE TABLE connector_property (
  objectid bigint(20) NOT NULL,
  version integer NOT NULL,
  connector_oid bigint(20) NOT NULL,
  name varchar(128) NOT NULL,
  value MEDIUMTEXT NOT NULL,
  PRIMARY KEY (objectid),
  FOREIGN KEY (connector_oid) REFERENCES connector (objectid) ON DELETE CASCADE
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table rbac_role
--

DROP TABLE IF EXISTS rbac_role;
CREATE TABLE rbac_role (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  name varchar(128) default NULL,
  entity_type varchar(255),
  entity_oid bigint(20),
  description mediumtext,
  PRIMARY KEY (objectid),
  UNIQUE KEY name (name),
  UNIQUE KEY entity_info (entity_type, entity_oid),
  INDEX i_rbacrole_etype (entity_type),
  INDEX i_rbacrole_eoid (entity_oid)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table rbac_assignment
--

DROP TABLE IF EXISTS rbac_assignment;
CREATE TABLE rbac_assignment (
  objectid bigint(20) NOT NULL,
  provider_oid bigint(20) NOT NULL,
  role_oid bigint(20) NOT NULL,
  identity_id varchar(255) NOT NULL,
  entity_type varchar(50) NOT NULL,
  PRIMARY KEY  (objectid),
  UNIQUE KEY unique_assignment (provider_oid,role_oid,identity_id, entity_type),
  FOREIGN KEY (role_oid) REFERENCES rbac_role (objectid) ON DELETE CASCADE,
  FOREIGN KEY (provider_oid) REFERENCES identity_provider (objectid) ON DELETE CASCADE,  
  INDEX i_rbacassign_poid (provider_oid),
  INDEX i_rbacassign_uid (identity_id)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table rbac_permission
--

DROP TABLE IF EXISTS rbac_permission;
CREATE TABLE rbac_permission (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  role_oid bigint(20) default NULL,
  operation_type varchar(16) default NULL,
  other_operation varchar(255) default NULL,
  entity_type varchar(255) default NULL,
  PRIMARY KEY (objectid),
  FOREIGN KEY (role_oid) REFERENCES rbac_role (objectid) ON DELETE CASCADE
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table rbac_predicate
--

DROP TABLE IF EXISTS rbac_predicate;
CREATE TABLE rbac_predicate (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  permission_oid bigint(20) default NULL,
  PRIMARY KEY (objectid),
  FOREIGN KEY (permission_oid) REFERENCES rbac_permission (objectid) ON DELETE CASCADE
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table rbac_predicate_attribute
--

DROP TABLE IF EXISTS rbac_predicate_attribute;
CREATE TABLE rbac_predicate_attribute (
  objectid bigint(20) NOT NULL,
  attribute varchar(255) default NULL,
  value varchar(255) default NULL,
  PRIMARY KEY (objectid),
  FOREIGN KEY (objectid) REFERENCES rbac_predicate (objectid) ON DELETE CASCADE
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table rbac_predicate_oid
--

DROP TABLE IF EXISTS rbac_predicate_oid;
CREATE TABLE rbac_predicate_oid (
  objectid bigint(20) NOT NULL,
  entity_id varchar(255) default NULL,
  PRIMARY KEY (objectid),
  FOREIGN KEY (objectid) REFERENCES rbac_predicate (objectid) ON DELETE CASCADE
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

-- Create Administrator role
-- XXX NOTE!! COPIED in Role#ADMIN_ROLE_OID

INSERT INTO rbac_role VALUES (-100,0,'Administrator',null,null,'Users assigned to the {0} role have full access to the gateway.');
INSERT INTO rbac_permission VALUES (-101, 0, -100, 'CREATE', null, 'ANY');
INSERT INTO rbac_permission VALUES (-102, 0, -100, 'READ',   null, 'ANY');
INSERT INTO rbac_permission VALUES (-103, 0, -100, 'UPDATE', null, 'ANY');
INSERT INTO rbac_permission VALUES (-104, 0, -100, 'DELETE', null, 'ANY');

-- Create Operator role
INSERT INTO rbac_role VALUES (-150,0,'Operator',null,null,'Users assigned to the {0} role have read only access to the gateway.');
INSERT INTO rbac_permission VALUES (-151, 0, -150, 'READ', null, 'ANY');

-- Create other canned roles
INSERT INTO rbac_role VALUES (-200,0,'Manage Internal Users and Groups', null,null, 'Users assigned to the {0} role have the ability to create, read, update and delete users and groups in the internal identity provider.');
INSERT INTO rbac_permission VALUES (-201,0,-200,'READ',NULL,'USER');
INSERT INTO rbac_predicate VALUES (-202,0,-201);
INSERT INTO rbac_predicate_attribute VALUES (-202,'providerId','-2');
INSERT INTO rbac_permission VALUES (-203,0,-200,'READ',NULL,'ID_PROVIDER_CONFIG');
INSERT INTO rbac_predicate VALUES (-204,0,-203);
INSERT INTO rbac_predicate_oid VALUES (-204,'-2');
INSERT INTO rbac_permission VALUES (-205,0,-200,'UPDATE',NULL,'USER');
INSERT INTO rbac_predicate VALUES (-206,0,-205);
INSERT INTO rbac_predicate_attribute VALUES (-206,'providerId','-2');
INSERT INTO rbac_permission VALUES (-207,0,-200,'READ',NULL,'GROUP');
INSERT INTO rbac_predicate VALUES (-208,0,-207);
INSERT INTO rbac_predicate_attribute VALUES (-208,'providerId','-2');
INSERT INTO rbac_permission VALUES (-209,0,-200,'DELETE',NULL,'USER');
INSERT INTO rbac_predicate VALUES (-210,0,-209);
INSERT INTO rbac_predicate_attribute VALUES (-210,'providerId','-2');
INSERT INTO rbac_permission VALUES (-211,0,-200,'CREATE',NULL,'USER');
INSERT INTO rbac_predicate VALUES (-212,0,-211);
INSERT INTO rbac_predicate_attribute VALUES (-212,'providerId','-2');
INSERT INTO rbac_permission VALUES (-213,0,-200,'CREATE',NULL,'GROUP');
INSERT INTO rbac_predicate VALUES (-214,0,-213);
INSERT INTO rbac_predicate_attribute VALUES (-214,'providerId','-2');
INSERT INTO rbac_permission VALUES (-215,0,-200,'DELETE',NULL,'GROUP');
INSERT INTO rbac_predicate VALUES (-216,0,-215);
INSERT INTO rbac_predicate_attribute VALUES (-216,'providerId','-2');
INSERT INTO rbac_permission VALUES (-217,0,-200,'UPDATE',NULL,'GROUP');
INSERT INTO rbac_predicate VALUES (-218,0,-217);
INSERT INTO rbac_predicate_attribute VALUES (-218,'providerId','-2');

INSERT INTO rbac_role VALUES (-250,0,'Publish External Identity Providers', null,null, 'Users assigned to the {0} role have the ability to create new external identity providers.');
INSERT INTO rbac_permission VALUES (-251,0,-250,'CREATE',NULL,'ID_PROVIDER_CONFIG');
INSERT INTO rbac_predicate VALUES (-252,0,-251);
INSERT INTO rbac_predicate_attribute VALUES (-252,'typeVal','2');
INSERT INTO rbac_permission VALUES (-253,0,-250,'CREATE',NULL,'ID_PROVIDER_CONFIG');
INSERT INTO rbac_predicate VALUES (-254,0,-253);
INSERT INTO rbac_predicate_attribute VALUES (-254,'typeVal','3');
INSERT INTO rbac_permission VALUES (-255,0,-250,'READ',NULL,'TRUSTED_CERT');

INSERT INTO rbac_role VALUES (-300,0,'Search Users and Groups', null,null, 'Users assigned to the {0} role have permission to search and view users and groups in all identity providers.');
INSERT INTO rbac_permission VALUES (-301,0,-300,'READ',NULL,'USER');
INSERT INTO rbac_permission VALUES (-302,0,-300,'READ',NULL,'ID_PROVIDER_CONFIG');
INSERT INTO rbac_permission VALUES (-303,0,-300,'READ',NULL,'GROUP');

INSERT INTO rbac_role VALUES (-350,0,'Publish Webservices', null,null, 'Users assigned to the {0} role have the ability to publish new web services.' );
INSERT INTO rbac_permission VALUES (-351,0,-350,'READ',NULL,'GROUP');
INSERT INTO rbac_permission VALUES (-352,0,-350,'READ',NULL,'ID_PROVIDER_CONFIG');
INSERT INTO rbac_permission VALUES (-353,0,-350,'READ',NULL,'USER');
INSERT INTO rbac_permission VALUES (-354,0,-350,'CREATE',NULL,'SERVICE');
INSERT INTO rbac_permission VALUES (-355,0,-350,'READ',NULL,'SERVICE_TEMPLATE');

INSERT INTO rbac_role VALUES (-400,1,'Manage Webservices', null,null, 'Users assigned to the {0} role have the ability to publish new services and edit existing ones.');
INSERT INTO rbac_permission VALUES (-401,0,-400,'READ',NULL,'ID_PROVIDER_CONFIG');
INSERT INTO rbac_permission VALUES (-402,0,-400,'READ',NULL,'GROUP');
INSERT INTO rbac_permission VALUES (-403,0,-400,'READ',NULL,'USER');
INSERT INTO rbac_permission VALUES (-404,0,-400,'READ',NULL,'SERVICE');
INSERT INTO rbac_permission VALUES (-405,0,-400,'CREATE',NULL,'SERVICE');
INSERT INTO rbac_permission VALUES (-406,0,-400,'UPDATE',NULL,'SERVICE');
INSERT INTO rbac_permission VALUES (-407,0,-400,'DELETE',NULL,'SERVICE');
INSERT INTO rbac_permission VALUES (-408,0,-400,'READ',NULL,'CLUSTER_INFO');
INSERT INTO rbac_permission VALUES (-409,0,-400,'READ',NULL,'SERVICE_USAGE');
INSERT INTO rbac_permission VALUES (-410,0,-400,'READ',NULL,'METRICS_BIN');
INSERT INTO rbac_permission VALUES (-411,0,-400,'READ',NULL,'AUDIT_MESSAGE');
INSERT INTO rbac_permission VALUES (-412,0,-400,'READ',NULL,'JMS_CONNECTION');
INSERT INTO rbac_permission VALUES (-413,0,-400,'READ',NULL,'JMS_ENDPOINT');
INSERT INTO rbac_permission VALUES (-414,0,-400,'READ',NULL,'SERVICE_TEMPLATE');
INSERT INTO rbac_permission VALUES (-415,0,-400,'READ',NULL,'POLICY');
INSERT INTO rbac_permission VALUES (-416,0,-400,'UPDATE',NULL,'POLICY');

-- Manage Webservices users can CRUD each and every possible folder
INSERT INTO rbac_permission VALUES (-417, 0, -400, 'CREATE', null, 'FOLDER');
INSERT INTO rbac_permission VALUES (-418, 0, -400, 'READ',   null, 'FOLDER');
INSERT INTO rbac_permission VALUES (-419, 0, -400, 'UPDATE', null, 'FOLDER');
INSERT INTO rbac_permission VALUES (-420, 0, -400, 'DELETE', null, 'FOLDER');


INSERT INTO rbac_role VALUES (-450,0,'View Audit Records and Logs', null,null, 'Users assigned to the {0} role have the ability to view audit and log details in manager.');
INSERT INTO rbac_permission VALUES (-451,0,-450,'READ',NULL,'CLUSTER_INFO');
INSERT INTO rbac_permission VALUES (-452,0,-450,'READ',NULL,'AUDIT_RECORD');

INSERT INTO rbac_role VALUES (-500,0,'View Service Metrics', null,null, 'Users assigned to the {0} role have the ability to monitor service metrics in the manager.');
INSERT INTO rbac_permission VALUES (-501,0,-500,'READ',NULL,'METRICS_BIN');
INSERT INTO rbac_permission VALUES (-502,0,-500,'READ',NULL,'SERVICE');
INSERT INTO rbac_permission VALUES (-503,0,-500,'READ',NULL,'CLUSTER_INFO');
INSERT INTO rbac_permission VALUES (-504,0,-500,'READ',NULL,'SERVICE_USAGE');

INSERT INTO rbac_role VALUES (-550,0,'Manage Cluster Status', null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete cluster status information.');
INSERT INTO rbac_permission VALUES (-551,0,-550,'READ',NULL,'CLUSTER_INFO');
INSERT INTO rbac_permission VALUES (-552,0,-550,'UPDATE',NULL,'CLUSTER_INFO');
INSERT INTO rbac_permission VALUES (-553,0,-550,'DELETE',NULL,'CLUSTER_INFO');

INSERT INTO rbac_role VALUES (-600,0,'Manage Certificates (truststore)', null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete trusted certificates and policies for revocation checking.');
INSERT INTO rbac_permission VALUES (-601,0,-600,'UPDATE',NULL,'TRUSTED_CERT');
INSERT INTO rbac_permission VALUES (-602,0,-600,'READ',NULL,'TRUSTED_CERT');
INSERT INTO rbac_permission VALUES (-603,0,-600,'DELETE',NULL,'TRUSTED_CERT');
INSERT INTO rbac_permission VALUES (-604,0,-600,'CREATE',NULL,'TRUSTED_CERT');
INSERT INTO rbac_permission VALUES (-605,0,-600,'UPDATE',NULL,'REVOCATION_CHECK_POLICY');
INSERT INTO rbac_permission VALUES (-606,0,-600,'READ',NULL,'REVOCATION_CHECK_POLICY');
INSERT INTO rbac_permission VALUES (-607,0,-600,'DELETE',NULL,'REVOCATION_CHECK_POLICY');
INSERT INTO rbac_permission VALUES (-608,0,-600,'CREATE',NULL,'REVOCATION_CHECK_POLICY');

INSERT INTO rbac_role VALUES (-650,0,'Manage JMS Connections', null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete JMS connections.');
INSERT INTO rbac_permission VALUES (-651,1,-650,'READ',NULL,'JMS_CONNECTION');
INSERT INTO rbac_permission VALUES (-652,1,-650,'DELETE',NULL,'JMS_CONNECTION');
INSERT INTO rbac_permission VALUES (-653,1,-650,'CREATE',NULL,'JMS_CONNECTION');
INSERT INTO rbac_permission VALUES (-654,1,-650,'UPDATE',NULL,'JMS_CONNECTION');
INSERT INTO rbac_permission VALUES (-655,1,-650,'CREATE',NULL,'JMS_ENDPOINT');
INSERT INTO rbac_permission VALUES (-656,1,-650,'DELETE',NULL,'JMS_ENDPOINT');
INSERT INTO rbac_permission VALUES (-657,1,-650,'UPDATE',NULL,'JMS_ENDPOINT');
INSERT INTO rbac_permission VALUES (-658,1,-650,'READ',NULL,'JMS_ENDPOINT');

INSERT INTO rbac_role VALUES (-700,0,'Manage Cluster Properties', null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete cluster properties.');
INSERT INTO rbac_permission VALUES (-701,0,-700,'READ',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_permission VALUES (-702,0,-700,'CREATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_permission VALUES (-703,0,-700,'UPDATE',NULL,'CLUSTER_PROPERTY');
INSERT INTO rbac_permission VALUES (-704,0,-700,'DELETE',NULL,'CLUSTER_PROPERTY');

INSERT INTO rbac_role VALUES (-750,0,'Manage Listen Ports', null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete Gateway listen ports (HTTP(S) and FTP(S)).');
INSERT INTO rbac_permission VALUES (-751,0,-750,'READ',NULL,'SSG_CONNECTOR');
INSERT INTO rbac_permission VALUES (-752,0,-750,'CREATE',NULL,'SSG_CONNECTOR');
INSERT INTO rbac_permission VALUES (-753,0,-750,'UPDATE',NULL,'SSG_CONNECTOR');
INSERT INTO rbac_permission VALUES (-754,0,-750,'DELETE',NULL,'SSG_CONNECTOR');

INSERT INTO rbac_role VALUES (-800,0,'Manage Log Sinks', null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete log sinks.');
INSERT INTO rbac_permission VALUES (-801,0,-800,'READ',NULL,'LOG_SINK');
INSERT INTO rbac_permission VALUES (-802,0,-800,'CREATE',NULL,'LOG_SINK');
INSERT INTO rbac_permission VALUES (-803,0,-800,'UPDATE',NULL,'LOG_SINK');
INSERT INTO rbac_permission VALUES (-804,0,-800,'DELETE',NULL,'LOG_SINK');

-- Assign Administrator role to existing admin user
INSERT INTO rbac_assignment VALUES (-105, -2, -100, '3', 'User');

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
  PRIMARY KEY  (objectid)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

INSERT INTO sink_config VALUES (-810,0,'ssg','Main log','FILE',1,'INFO','AUDIT,LOG','<java version="1.6.0" class="java.beans.XMLDecoder"><object class="java.util.HashMap"><void method="put"><string>file.maxSize</string><string>20000</string></void><void method="put"><string>file.format</string><string>STANDARD</string></void><void method="put"><string>file.logCount</string><string>10</string></void></object></java>');

DROP TABLE IF EXISTS config_data;
CREATE TABLE config_data (
  objectid bigint(20) NOT NULL,
  configdata mediumblob DEFAULT NULL,
  PRIMARY KEY (objectid)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

DROP TABLE IF EXISTS wsdm_subscription;
CREATE TABLE wsdm_subscription (
  objectid bigint(20) NOT NULL,
  version int(11) NOT NULL,
  uuid varchar(36) NOT NULL,
  callback_url varchar(255) NOT NULL,
  published_service_oid bigint(20) NOT NULL,
  termination_time bigint(20) NOT NULL,
  topic int(11) NOT NULL,
  notification_policy_guid CHAR(36),
  last_notification bigint(20),
  owner_node_id varchar(36),
  PRIMARY KEY  (objectid),
  UNIQUE KEY uuid (uuid)
) TYPE=InnoDB DEFAULT CHARACTER SET utf8;

SET FOREIGN_KEY_CHECKS = 1;

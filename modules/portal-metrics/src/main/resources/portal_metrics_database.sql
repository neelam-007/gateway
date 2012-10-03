--
-- MySQL version of SSG database creation script.
--

SET FOREIGN_KEY_CHECKS = 0;

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
  default_routing_url varchar(4096),
  http_methods mediumtext,
  lax_resolution TINYINT(1) NOT NULL DEFAULT 0,
  wss_processing TINYINT(1) NOT NULL DEFAULT 1,
  tracing TINYINT(1) NOT NULL DEFAULT 0,
  folder_oid bigint(20),
  soap_version VARCHAR(20) DEFAULT 'UNKNOWN',
  uuid VARCHAR (48)  NULL,
  PRIMARY KEY (objectid)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;
-- Removed FK constraint
--  FOREIGN KEY (policy_oid) REFERENCES policy (objectid),
--  FOREIGN KEY (folder_oid) REFERENCES folder (objectid) ON DELETE SET NULL

--
-- Table structure for table 'service_metrics'
--
DROP TABLE IF EXISTS service_metrics;
CREATE TABLE service_metrics (
  objectid bigint(20) NOT NULL AUTO_INCREMENT,
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
  back_min INTEGER,
  back_max INTEGER,
  back_sum INTEGER NOT NULL,
  front_min INTEGER,
  front_max INTEGER,
  front_sum INTEGER NOT NULL,
  service_state VARCHAR(16),
  uuid VARCHAR (48) NULL,
  INDEX i_sm_nodeid (nodeid),
  INDEX i_sm_serviceoid (published_service_oid),
  INDEX i_sm_pstart (period_start),
  PRIMARY KEY (objectid),
  UNIQUE (nodeid, published_service_oid, resolution, period_start)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

--
-- Table structure for table 'service_metrics_details'
--
DROP TABLE IF EXISTS service_metrics_details;
CREATE TABLE service_metrics_details (
  service_metrics_oid BIGINT(20) NOT NULL,
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
  PRIMARY KEY (service_metrics_oid, mapping_values_oid),
  FOREIGN KEY (service_metrics_oid) REFERENCES service_metrics (objectid) ON DELETE CASCADE,
  FOREIGN KEY (mapping_values_oid) REFERENCES message_context_mapping_values (objectid)
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
  auth_user_provider_id bigint(20),
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


SET FOREIGN_KEY_CHECKS = 1;

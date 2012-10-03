-- HSQL specific

CREATE TABLE published_service (
  objectid bigint NOT NULL,
  version int NOT NULL,
  name varchar(255) NOT NULL,
  policy_xml varchar(255),
  policy_oid bigint default NULL,
  wsdl_url varchar(255),
  wsdl_xml varchar(255),
  disabled TINYINT DEFAULT 0 NOT NULL,
  soap TINYINT DEFAULT 1 NOT NULL,
  internal TINYINT DEFAULT 0 NOT NULL,
  routing_uri varchar(128),
  default_routing_url varchar(4096),
  http_methods varchar(255),
  lax_resolution TINYINT DEFAULT 0 NOT NULL,
  wss_processing TINYINT DEFAULT 1 NOT NULL,
  tracing TINYINT DEFAULT 0 NOT NULL,
  folder_oid bigint,
  soap_version VARCHAR(20) DEFAULT 'UNKNOWN',
  uuid VARCHAR (48)  NULL,
  PRIMARY KEY (objectid)
);

CREATE TABLE service_metrics (
  objectid bigint NOT NULL IDENTITY,
  nodeid VARCHAR(32) NOT NULL,
  published_service_oid BIGINT NOT NULL,
  resolution INTEGER NOT NULL,
  period_start BIGINT NOT NULL,
  start_time BIGINT NOT NULL,
  interval_size INTEGER NOT NULL,
  end_time BIGINT NOT NULL,
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
  UNIQUE (nodeid, published_service_oid, resolution, period_start)
);

CREATE TABLE message_context_mapping_keys (
  objectid bigint NOT NULL,
  version int NOT NULL,
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
  create_time bigint,
  PRIMARY KEY (objectid)
);

CREATE TABLE message_context_mapping_values (
  objectid bigint NOT NULL,
  digested char(36) NOT NULL,
  mapping_keys_oid bigint NOT NULL,
  auth_user_provider_id bigint,
  auth_user_id varchar(255),
  auth_user_unique_id varchar(255),
  service_operation varchar(255),
  mapping1_value varchar(255),
  mapping2_value varchar(255),
  mapping3_value varchar(255),
  mapping4_value varchar(255),
  mapping5_value varchar(255),
  create_time bigint,
  PRIMARY KEY  (objectid),
  FOREIGN KEY (mapping_keys_oid) REFERENCES message_context_mapping_keys (objectid)
);

CREATE TABLE service_metrics_details (
  service_metrics_oid BIGINT NOT NULL,
  mapping_values_oid BIGINT NOT NULL,
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
);

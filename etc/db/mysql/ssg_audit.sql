--
-- Gateway database creation script for auditing tables on MySQL
--

SET FOREIGN_KEY_CHECKS = 0;

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
  provider_oid bigint(20) NOT NULL DEFAULT -1,
  signature varchar(1024),
  PRIMARY KEY  (objectid),
  KEY idx_time (time),
  KEY idx_ip_address (ip_address),
  KEY idx_prov_user (provider_oid, user_id)
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

SET FOREIGN_KEY_CHECKS = 1;

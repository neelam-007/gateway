--
-- Gateway database creation script for service metrics tables on MySQL
--

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS service_metrics;
CREATE TABLE service_metrics (
  objectid bigint(20) NOT NULL,
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
  INDEX i_sm_nodeid (nodeid),
  INDEX i_sm_serviceoid (published_service_oid),
  INDEX i_sm_pstart (period_start),
  PRIMARY KEY (objectid),
  UNIQUE (nodeid, published_service_oid, resolution, period_start)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8;

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

SET FOREIGN_KEY_CHECKS = 1;

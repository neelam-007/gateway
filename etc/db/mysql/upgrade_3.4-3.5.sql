--
-- Script to update mysql ssg database from 3.4(.1) to 3.5
--
-- Layer 7 Technologies, inc
--

alter table service_resolution drop index `soapaction`;
alter table service_resolution modify column soapaction mediumtext character set latin1 BINARY default '';
alter table service_resolution modify column urn mediumtext character set latin1 BINARY default '';
alter table service_resolution modify column uri mediumtext character set latin1 BINARY default '';
alter table service_resolution add digested varchar(32) default '';
update service_resolution set digested=HEX(MD5(CONCAT(soapaction,urn,uri)));
alter table service_resolution modify column digested varchar(32) NOT NULL;
CREATE UNIQUE INDEX digested ON service_resolution (digested);

DROP TABLE IF EXISTS service_metrics;
CREATE TABLE service_metrics (
  nodeid VARCHAR(18) NOT NULL,
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
  INDEX i_sm_nodeid (nodeid),
  INDEX i_sm_serviceoid (published_service_oid),
  INDEX i_sm_resolution (resolution),
  INDEX i_sm_pstart (period_start),
  PRIMARY KEY (nodeid, published_service_oid, resolution, period_start)
) TYPE=InnoDB;

--
-- Cluster Properties
--

alter table cluster_properties drop primary key;

alter table cluster_properties add objectid bigint(20) not null primary key auto_increment;
alter table cluster_properties add version integer not null;

-- Removes auto_increment but still primary key
alter table cluster_properties change objectid objectid bigint(20) not null;

create unique index i_cp_propkey on cluster_properties (propkey);

--
-- Community Schemas
--

alter table community_schemas add version integer not null;
alter table community_schemas change `schema` schema_xml mediumtext default '';

--
-- GLOBAL COUNTERS IN CLUSTER FIX
--
alter table counters modify column userid varchar(128) NOT NULL;

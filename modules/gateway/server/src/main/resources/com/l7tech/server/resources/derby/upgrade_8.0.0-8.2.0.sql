--
-- Script to update mysql ssg database from 8.0.0 to 8.2
--
-- Layer 7 Technologies, inc
--


--
-- Update the version
--
UPDATE ssg_version SET current_version = '8.0.0';

alter table jms_endpoint add column is_passthrough_message_rules smallint default 0;

create table jms_endpoint_message_rule (
  goid CHAR(16) FOR BIT DATA not null,
  version integer,
  jms_endpoint_goid CHAR(16) FOR BIT DATA not null REFERENCES jms_endpoint (goid) ON DELETE CASCADE,
  rule_name varchar(256),
  is_passthrough smallint,
  custom_pattern varchar(4096),
  PRIMARY KEY (goid)
);

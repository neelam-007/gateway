--
-- Script to update mysql ssg database from 3.3 to 3.4
--
-- Layer 7 Technologies, inc
--

ALTER TABLE client_cert ADD COLUMN ski VARCHAR(64);
ALTER TABLE client_cert ADD INDEX i_ski (ski);

ALTER TABLE trusted_cert ADD COLUMN ski VARCHAR(64);
ALTER TABLE trusted_cert ADD INDEX i_ski (ski);


alter table cluster_properties change propvalue propvalue mediumtext;
alter table  audit_admin          charset='utf8';
alter table  audit_detail         charset='utf8';
alter table  audit_detail_params  charset='utf8';
alter table  audit_main           charset='utf8';
alter table  audit_message        charset='utf8';
alter table  audit_system         charset='utf8';
alter table  client_cert          charset='utf8';
alter table  cluster_info         charset='utf8';
alter table  cluster_properties   charset='utf8';
alter table  community_schemas    charset='utf8';
alter table  counters             charset='utf8';
alter table  fed_group            charset='utf8';
alter table  fed_group_virtual    charset='utf8';
alter table  fed_user             charset='utf8';
alter table  fed_user_group       charset='utf8';
alter table  hibernate_unique_key charset='utf8';
alter table  identity_provider    charset='utf8';
alter table  internal_group       charset='utf8';
alter table  internal_user        charset='utf8';
alter table  internal_user_group  charset='utf8';
alter table  jms_connection       charset='utf8';
alter table  jms_endpoint         charset='utf8';
alter table  message_id           charset='utf8';
alter table  published_service    charset='utf8';
alter table  sample_messages      charset='utf8';
alter table  service_resolution   charset='utf8';
alter table  service_usage        charset='utf8';
alter table  trusted_cert         charset='utf8';

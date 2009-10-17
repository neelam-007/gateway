--
-- Script to update mysql ssg database from 3.6 to 3.6.5
--
-- Layer 7 Technologies, inc
--

-- Track which community schema entries are system managed
alter table community_schemas add column system tinyint(1) NOT NULL default 0;
update community_schemas set system=1 where name = 'soapenv';

-- Record authentication type in audit log
alter table audit_message add column authenticationType int(11);

-- Add hostname verification flag for trusted certs.
alter table trusted_cert add column verify_hostname tinyint(1) default 0;

-- Remove defaults from BLOB/mediumtext columns to enable compatilibity with MySQL 5.0 in strict mode
alter table identity_provider modify description mediumtext;
alter table service_resolution modify soapaction mediumtext character set latin1 BINARY;
alter table service_resolution modify urn mediumtext character set latin1 BINARY;
alter table service_resolution modify uri mediumtext character set latin1 BINARY;
alter table community_schemas modify schema_xml mediumtext;
alter table rbac_role modify description mediumtext;

-- Change role description from varchar(255) to mediumtext for better compatilibity with MySQL 5.0 in strict mode
alter table rbac_role modify column description mediumtext;

-- Add partition data to cluster info entry
alter table cluster_info add column nodeid varchar(32);
alter table cluster_info add column partition_name varchar(128);
alter table cluster_info add column cluster_port integer NOT NULL default 2124;
update cluster_info set partition_name='default_', nodeid=MD5(CONCAT(mac, '-default_'));
alter table cluster_info modify nodeid varchar(32) NOT NULL;
alter table cluster_info modify partition_name varchar(128) NOT NULL;
alter table cluster_info drop primary key;
alter table cluster_info add primary key (nodeid);

alter table service_usage modify column nodeid varchar(32) NOT NULL;
alter table audit_main modify column nodeid varchar(32) NOT NULL;
alter table service_metrics modify column nodeid varchar(32) NOT NULL;

update service_usage set nodeid = MD5(CONCAT(nodeid, '-default_'));
update audit_main set nodeid = MD5(CONCAT(nodeid, '-default_'));
update service_metrics set nodeid = MD5(CONCAT(nodeid, '-default_'));

-- Remove SOAP schema, which has gone virtual
delete from community_schemas where tns='http://schemas.xmlsoap.org/soap/envelope/';

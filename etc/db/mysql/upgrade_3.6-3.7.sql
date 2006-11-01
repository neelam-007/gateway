---
--- Script to update mysql ssg database from 3.6 to 3.7
---
--- Layer 7 Technologies, inc
---

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


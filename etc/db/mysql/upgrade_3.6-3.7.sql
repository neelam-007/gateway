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


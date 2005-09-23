---
--- Script to update mysql ssg database from 3.3 to 3.4
---
--- Layer 7 Technologies, inc
---

ALTER TABLE client_cert ADD COLUMN ski VARCHAR(64);
ALTER TABLE client_cert ADD INDEX i_ski (ski);

ALTER TABLE trusted_cert ADD COLUMN ski VARCHAR(64);
ALTER TABLE trusted_cert ADD INDEX i_ski (ski);


alter table  cluster_properties add column new_value mediumtext;
update cluster_properties set new_value=propvalue;
alter table  cluster_properties drop column propvalue;
alter table cluster_properties change new_value propvalue mediumtext;


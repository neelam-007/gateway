---
--- Script to update mysql ssg database from 3.2 to 3.3
---
--- Layer 7 Technologies, inc

ALTER TABLE trusted_cert ADD COLUMN trusted_as_saml_attesting_entity tinyint(1) default '0';
